import requests
from bs4 import BeautifulSoup
import os
import time
import re
import unicodedata

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
    return f"{categorie}_{text}.jpg"

def get_bird_image_links():
    url = "https://www.seor.fr/fiches-oiseaux.php"
    headers = {'User-Agent': 'Mozilla/5.0'}

    print("🦜 Extraction des liens d'images...")
    response = requests.get(url, headers=headers)
    soup = BeautifulSoup(response.text, 'html.parser')

    items = soup.find_all('div', class_='fiche_oiseau_detail')
    image_tasks = []

    for item in items:
        try:
            nom = item.find('h2').text.strip()
            # Nettoyage robuste avec ajout automatique du préfixe "oiseau_"
            file_name = normalize_filename(nom, categorie="oiseau")

            img_path = item.find('img')['src']
            full_url = "https://www.seor.fr/" + img_path

            image_tasks.append({'url': full_url, 'filename': file_name})
        except:
            continue

    return image_tasks

def download_images(image_list):
    folder = "images_oiseaux"
    if not os.path.exists(folder):
        os.makedirs(folder)
        print(f"📁 Dossier '{folder}' créé.")

    print(f"📥 Début du téléchargement de {len(image_list)} images...")

    for img in image_list:
        try:
            path = os.path.join(folder, img['filename'])

            img_data = requests.get(img['url']).content

            with open(path, 'wb') as handler:
                handler.write(img_data)

            print(f"✅ Téléchargé : {img['filename']}")

            time.sleep(0.2)
        except Exception as e:
            print(f"❌ Erreur sur {img['url']} : {e}")

if __name__ == "__main__":
    liens = get_bird_image_links()
    download_images(liens)