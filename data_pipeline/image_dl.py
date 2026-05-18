import requests
from bs4 import BeautifulSoup
import os
import time

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
            # Nettoyage du nom pour le nom de fichier (ex: "Tuit-tuit" -> "tuit_tuit")
            file_name = nom.lower().replace(" ", "_").replace("-", "_") + ".jpg"
            
            img_path = item.find('img')['src']
            full_url = "https://www.seor.fr/" + img_path
            
            image_tasks.append({'url': full_url, 'filename': file_name})
        except:
            continue
            
    return image_tasks

def download_images(image_list):
    # Création du dossier s'il n'existe pas
    folder = "images_oiseaux"
    if not os.path.exists(folder):
        os.makedirs(folder)
        print(f"📁 Dossier '{folder}' créé.")

    print(f"📥 Début du téléchargement de {len(image_list)} images...")
    
    for img in image_list:
        try:
            path = os.path.join(folder, img['filename'])
            
            # On télécharge l'image
            img_data = requests.get(img['url']).content
            
            with open(path, 'wb') as handler:
                handler.write(img_data)
            
            print(f"✅ Téléchargé : {img['filename']}")
            
            # Petite pause pour respecter le serveur
            time.sleep(0.2)
        except Exception as e:
            print(f"❌ Erreur sur {img['url']} : {e}")

# --- Exécution ---
if __name__ == "__main__":
    liens = get_bird_image_links()
    
    # Affichage de la liste brute si besoin
    liste_brute = [l['url'] for l in liens]
    print(f"\n🔗 Liste des liens : {liste_brute}\n")
    
    # Lancement du téléchargement
    download_images(liens)