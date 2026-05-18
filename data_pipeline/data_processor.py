import requests
from bs4 import BeautifulSoup
import sqlite3
import pandas as pd
import time
import os
import re
import unicodedata

# --- Configuration ---
BASE_URL = "https://www.seor.fr/"
LIST_URL = "https://www.seor.fr/fiches-oiseaux.php"
HEADERS = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'}

def normalize_filename(text, categorie="oiseau"):
    """
    Transforme 'Astrild ondulé' en 'oiseau_astrild_ondule.jpg'
    """
    # 1. Enlever les accents
    text = unicodedata.normalize('NFD', text)
    text = text.encode('ascii', 'ignore').decode('utf-8')
    # 2. Minuscules et remplacement des espaces/tirets par des underscores
    text = text.lower().strip()
    text = re.sub(r'[\s\-]+', '_', text)
    # 3. Supprimer les caractères non-alphanumériques restants
    text = re.sub(r'[^\w_]', '', text)
    # 4. Ajout du préfixe
    return f"{categorie}_{text}.jpg"

def get_uicn_status(soup):
    try:
        cells = soup.find_all('td', style=True)
        for cell in cells:
            style = cell['style'].replace(" ", "").lower()
            if "background:#ffffff" not in style and len(cell.text.strip()) == 2:
                return cell.text.strip()
        return "NE"
    except:
        return "NE"

def get_description_blocks(soup):
    ident, hab = "", ""
    containers = soup.find_all('div', class_='content_seor')
    for container in containers:
        titles = container.find_all('strong')
        for title in titles:
            text_title = title.get_text().strip().upper()
            content = []
            curr = title.next_sibling
            while curr and (not curr.name or curr.name != 'strong'):
                if isinstance(curr, str):
                    content.append(curr.strip())
                elif curr.name in ['b', 'i', 'em', 'span']:
                    content.append(curr.get_text().strip())
                curr = curr.next_sibling
            full_text = " ".join(filter(None, content)).strip()
            if "IDENTIFICATION" in text_title: ident = full_text
            elif "HABITAT" in text_title: hab = full_text
    return ident, hab

def scrape_seor_data():
    print("🦜 Connexion au site de la SEOR...")
    response = requests.get(LIST_URL, headers=HEADERS)
    soup = BeautifulSoup(response.text, 'html.parser')
    items = soup.find_all('div', class_='fiche_oiseau_detail')

    print(f"🔎 {len(items)} oiseaux trouvés. Génération de la base...")
    birds_list = []

    for item in items:
        try:
            main_name = item.find('h2').text.strip()
            nom_pei = item.find('h3').text.strip()
            detail_link = BASE_URL + item.find('a', class_='fiche_oiseau_bloc')['href']

            img_container = item.find('div', class_='fiche_oiseau_img')
            css_classes = img_container.get('class', [])
            status = "Non défini"
            if 'espece_endemique' in css_classes: status = "Endémique"
            elif 'espece_indigene' in css_classes: status = "Indigène"
            elif 'espece_introduit' in css_classes: status = "Introduit"
            elif 'espece_migrateur' in css_classes: status = "Migrateur"
            elif 'espece_occasionnel' in css_classes: status = "Occasionnel"
            elif 'espece_disparue' in css_classes: status = "Disparue"

            print(f"📖 Lecture : {main_name}...")
            res_det = requests.get(detail_link, headers=HEADERS)
            soup_det = BeautifulSoup(res_det.text, 'html.parser')
            uicn = get_uicn_status(soup_det)
            ident, hab = get_description_blocks(soup_det)

            # Appel de la fonction avec le préfixe explicite (bien qu'il soit par défaut)
            local_image_name = normalize_filename(main_name, categorie="oiseau")

            birds_list.append({
                'nom_pei': nom_pei,
                'common_name': main_name,
                'status': status,
                'uicn': uicn,
                'identification': ident,
                'habitat': hab,
                'image_path': local_image_name,
                'zone_id': 0
            })
            time.sleep(1)

        except Exception as e:
            print(f"⚠️ Erreur sur {main_name} : {e}")
            continue

    return birds_list

def generate_sqlite(data):
    if not data: return
    df = pd.DataFrame(data)
    db_name = "biodiv_reunion.db"

    conn = sqlite3.connect(db_name)
    df.to_sql('species', conn, if_exists='replace', index_label='id')

    conn.close()
    print(f"\n✅ Base de données '{db_name}' générée avec les références préfixées.")

if __name__ == "__main__":
    results = scrape_seor_data()
    generate_sqlite(results)