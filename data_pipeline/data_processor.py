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

# --- Définition des Zones avec Mots-clés ET Coordonnées (Bounding Boxes) ---
# Format : id: {"nom": "...", "mots_cles": [...], "lat_min": ..., "lat_max": ..., "lon_min": ..., "lon_max": ...}
ZONES_DATA = {
    1: {
        "nom": "Littoral et zones côtières",
        "mots_cles": ["mer", "océan", "plage", "littoral", "côte", "marin", "marine", "falaise"],
        "lat_min": -21.38, "lat_max": -20.87, "lon_min": 55.21, "lon_max": 55.83 # Englobe globalement les côtes
    },
    2: {
        "nom": "Forêts et milieux denses",
        "mots_cles": ["forêt", "bois", "arbres", "végétation dense", "canopée", "endémique"],
        "lat_min": -21.15, "lat_max": -20.95, "lon_min": 55.40, "lon_max": 55.60 # Centre de l'île (Bébour/Bélouve)
    },
    3: {
        "nom": "Hauts et Montagnes",
        "mots_cles": ["altitude", "montagne", "hauts", "2 000 m", "piton", "volcan", "rempart", "cirque"],
        "lat_min": -21.28, "lat_max": -21.05, "lon_min": 55.45, "lon_max": 55.75 # Zone Volcan / Piton des Neiges
    },
    4: {
        "nom": "Milieux Urbains et Ouverts",
        "mots_cles": ["ville", "jardin", "urbain", "parc", "habitation", "agricole", "savane", "ouvert"],
        "lat_min": -21.00, "lat_max": -20.87, "lon_min": 55.35, "lon_max": 55.55 # Principalement le Nord (St-Denis/Ste-Marie)
    }
}

def attribuer_zones(habitat_text):
    """Analyse le texte et retourne la liste des IDs de zones correspondantes."""
    zones_trouvees = []
    if not habitat_text:
        return [1, 2, 3, 4] # Partout par défaut

    texte_min = habitat_text.lower()
    for zone_id, data in ZONES_DATA.items():
        for mot in data["mots_cles"]:
            if mot in texte_min:
                zones_trouvees.append(zone_id)
                break
                
    if not zones_trouvees:
        zones_trouvees = [1, 2, 3, 4] 
    return zones_trouvees

def normalize_filename(text, categorie="oiseau"):
    """Transforme 'Astrild ondulé' en 'oiseau_astrild_ondule.jpg'"""
    text = unicodedata.normalize('NFD', text).encode('ascii', 'ignore').decode('utf-8')
    text = re.sub(r'[\s\-]+', '_', text.lower().strip())
    text = re.sub(r'[^\w_]', '', text)
    return f"{categorie}_{text}.jpg"

def get_uicn_status(soup):
    try:
        cells = soup.find_all('td', style=True)
        for cell in cells:
            if "background:#ffffff" not in cell.get('style', '').replace(" ", "").lower() and len(cell.text.strip()) == 2:
                return cell.text.strip()
        return "NE"
    except: return "NE"

def get_description_blocks(soup):
    ident, hab = "", ""
    containers = soup.find_all('div', class_='content_seor')
    for container in containers:
        titles = container.find_all('strong')
        for title in titles:
            text_title = title.get_text().strip().upper()
            content = [curr.strip() if isinstance(curr, str) else curr.get_text().strip() 
                       for curr in getattr(title, 'next_siblings', []) 
                       if not (getattr(curr, 'name', '') == 'strong')]
            # Astuce pour récupérer jusqu'au prochain <strong> (simplifié ici)
            curr = title.next_sibling
            bloc_text = ""
            while curr and getattr(curr, 'name', '') != 'strong':
                bloc_text += (curr.strip() if isinstance(curr, str) else curr.get_text().strip()) + " "
                curr = curr.next_sibling
            
            if "IDENTIFICATION" in text_title: ident = bloc_text.strip()
            elif "HABITAT" in text_title: hab = bloc_text.strip()
    return ident, hab

def scrape_seor_data():
    print("🦜 Connexion au site de la SEOR...")
    response = requests.get(LIST_URL, headers=HEADERS)
    soup = BeautifulSoup(response.text, 'html.parser')
    items = soup.find_all('div', class_='fiche_oiseau_detail')

    print(f"🔎 {len(items)} oiseaux trouvés. Génération de la base...")
    
    birds_list = []
    espece_zone_list = []
    
    # 1. On prépare la liste des zones pour l'export
    zones_list = []
    for z_id, z_data in ZONES_DATA.items():
        zones_list.append({
            'id': z_id,
            'nom_zone': z_data['nom'],
            'lat_min': z_data['lat_min'],
            'lat_max': z_data['lat_max'],
            'lon_min': z_data['lon_min'],
            'lon_max': z_data['lon_max']
        })

    espece_id_counter = 1 

    for item in items:
        try:
            main_name = item.find('h2').text.strip()
            nom_pei = item.find('h3').text.strip()
            detail_link = BASE_URL + item.find('a', class_='fiche_oiseau_bloc')['href']

            css_classes = item.find('div', class_='fiche_oiseau_img').get('class', [])
            status = next((s for c, s in [('espece_endemique', 'Endémique'), ('espece_indigene', 'Indigène'), 
                                          ('espece_introduit', 'Introduit'), ('espece_migrateur', 'Migrateur'), 
                                          ('espece_occasionnel', 'Occasionnel'), ('espece_disparue', 'Disparue')] 
                           if c in css_classes), "Non défini")

            print(f"📖 Lecture : {main_name}...")
            soup_det = BeautifulSoup(requests.get(detail_link, headers=HEADERS).text, 'html.parser')
            
            hab = get_description_blocks(soup_det)[1] # On prend juste l'habitat pour l'exemple
            
            # 2. On attribue les zones
            zones_de_loiseau = attribuer_zones(hab)

            birds_list.append({
                'id': espece_id_counter,
                'nom_pei': nom_pei,
                'nom_scientifique': main_name,
                'origine': status,
                'uicn': get_uicn_status(soup_det),
                'identification': get_description_blocks(soup_det)[0],
                'habitat': hab,
                'image_path': normalize_filename(main_name)
            })

            # 3. On remplit la table de jonction
            for zid in zones_de_loiseau:
                espece_zone_list.append({'espece_id': espece_id_counter, 'zone_id': zid})

            espece_id_counter += 1
            time.sleep(0.5)

        except Exception as e:
            continue

    return birds_list, zones_list, espece_zone_list

def generate_sqlite(species_data, zones_data, espece_zone_data):
    print("\n💾 Sauvegarde dans SQLite...")
    conn = sqlite3.connect("biodiv_reunion.db")

    # Export des 3 DataFrames
    pd.DataFrame(species_data).to_sql('species', conn, if_exists='replace', index=False)
    pd.DataFrame(zones_data).to_sql('zones', conn, if_exists='replace', index=False)
    pd.DataFrame(espece_zone_data).to_sql('espece_zone', conn, if_exists='replace', index=False)

    conn.close()
    print("✅ Base de données 'biodiv_reunion.db' générée avec succès (3 tables) !")

if __name__ == "__main__":
    b_data, z_data, ez_data = scrape_seor_data()
    generate_sqlite(b_data, z_data, ez_data)