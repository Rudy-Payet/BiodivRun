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

# --- Définition des Zones avec Mots-clés enrichis ET Coordonnées (Bounding Boxes) ---
ZONES_DATA = {
    1: {
        "nom": "Ouest (Savane, Lagons et Étangs)", 
        # De Saint-Paul à Saint-Leu. Milieux secs et zones humides côtières.
        "mots_cles": ["savane", "lagon", "plage", "étang", "sec", "sèche", "ouest", "littoral", "mer", "océan", "côte", "mangrove"],
        "lat_min": -21.30, "lat_max": -21.00, "lon_min": 55.20, "lon_max": 55.35 
    },
    2: {
        "nom": "Nord & Est (Milieux Urbains et Agricoles)", 
        # De La Possession à Saint-Benoît. Villes, parcs et champs de canne.
        "mots_cles": ["ville", "jardin", "urbain", "parc", "habitation", "agricole", "canne", "culture", "périurbain", "nord", "est", "bâtiment"],
        "lat_min": -21.00, "lat_max": -20.87, "lon_min": 55.35, "lon_max": 55.85 
    },
    3: {
        "nom": "Sud & Sud Sauvage (Basalte et Forêts de basse altitude)", 
        # De Saint-Pierre à Saint-Philippe.
        "mots_cles": ["sud", "sauvage", "falaise", "basalte", "vacoa", "coulée", "mer", "océan", "pélagique"],
        "lat_min": -21.40, "lat_max": -21.30, "lon_min": 55.35, "lon_max": 55.85 
    },
    4: {
        "nom": "Centre & Cirques (Forêts humides et Ravines)", 
        # Mafate, Salazie, Cilaos, Bébour/Bélouve.
        "mots_cles": ["forêt", "humide", "cirque", "ravine", "montagne", "altitude", "bébour", "bélouve", "canopée", "sous-bois", "endémique", "indigène", "rempart"],
        "lat_min": -21.20, "lat_max": -21.00, "lon_min": 55.35, "lon_max": 55.65 
    },
    5: {
        "nom": "Massif du Volcan (Landes et minéral)", 
        # Piton de la Fournaise, Plaine des Sables.
        "mots_cles": ["volcan", "fournaise", "lande", "minéral", "roche", "caillou", "haute altitude", "plaine des sables"],
        "lat_min": -21.30, "lat_max": -21.20, "lon_min": 55.65, "lon_max": 55.85 
    }
}

# --- Base de données statique pour la Flore ---
plants_list = [
    {
        'id': 1001,
        'nom_pei': "Bois de poivre",
        'nom_scientifique': "Zanthoxylum heterophyllum",
        'origine': "Endémique",
        'uicn': "CR", # En danger critique
        'identification': "Le pied juvénile est très différent de l'adulte. Doit son nom à sa sève rouge. Les feuilles adultes comportent jusqu'à une douzaine de folioles aux nervures rouges bien marquées.",
        'habitat': "Milieux secs. Observée notamment à la Grotte des Premiers Français, dans l'Ouest.",
        'image_path': "plante_zanthoxylum_heterophyllum.jpg"
    },
    {
        'id': 1002,
        'nom_pei': "Acajou du Sénégal",
        'nom_scientifique': "Khaya senegalensis",
        'origine': "Introduit",
        'uicn': "VU", # Vulnérable (dans son aire d'origine africaine)
        'identification': "Arbre d'origine africaine avec un tronc très dégarni à sa base avant la première ramification qui pousse assez haut sur le tronc.",
        'habitat': "Planté pour le maintien des sols. Présent en très grandes quantités dans la Forêt de l'Étang Salé.",
        'image_path': "plante_khaya_senegalensis.jpg"
    },
    {
        'id': 1003,
        'nom_pei': "Piquant rouge",
        'nom_scientifique': "Themeda quadrivalvis",
        'origine': "Introduit (Envahissante)",
        'uicn': "NE",
        'identification': "Herbe annuelle à croissance rapide (jusqu'à 1,5m). La plante rougit en séchant, et son inflorescence est une grande panicule de 50 cm formée de plusieurs épis.",
        'habitat': "Très présente dans l'Ouest de l'île.",
        'image_path': "plante_themeda_quadrivalvis.jpg"
    },
    {
        'id': 1004,
        'nom_pei': "Bécabar / Macatia vert",
        'nom_scientifique': "Boerhavia diffusa",
        'origine': "Introduit (Envahissante)",
        'uicn': "LC", # Préoccupation mineure mondiale
        'identification': "Plante rampante. Feuilles opposées, ovales à sommet arrondi (4-5 nervures). Tiges teintées de rouge. Minuscules fleurs roses à violettes au bout d'un long pétiole.",
        'habitat': "Zones très ensoleillées au ras du sol, spécifiquement dans l'Ouest et le Sud.",
        'image_path': "plante_boerhavia_diffusa.jpg"
    },
    {
        'id': 1005,
        'nom_pei': "Queue de chat",
        'nom_scientifique': "Acalypha hispida",
        'origine': "Introduit",
        'uicn': "NE",
        'identification': "Petit arbuste dont la sève est toxique. Très longues inflorescences rouges retombantes sans pétale (jusqu'à 50 cm) associées à des feuilles vertes en forme de cœur.",
        'habitat': "Espaces urbains, parcs et jardins.",
        'image_path': "plante_acalypha_hispida.jpg"
    },
    {
        'id': 1006,
        'nom_pei': "Conflore",
        'nom_scientifique': "Canna indica",
        'origine': "Introduit",
        'uicn': "LC",
        'identification': "Feuilles longues, larges et pointues. Fleur rouge et jaune en grappe au bout du rameau. Les graines sont noires, dures et encapuchonnées dans une coque piquante.",
        'habitat': "De plus en plus présente dans les champs agricoles et sur les bords de chemins.",
        'image_path': "plante_canna_indica.jpg"
    },
    {
        'id': 1007,
        'nom_pei': "Arbre dauphin",
        'nom_scientifique': "Hura crepitans",
        'origine': "Introduit",
        'uicn': "NE",
        'identification': "Se reconnaît à ses feuilles ovales d'un beau vert fixées au bout d'un long pétiole.",
        'habitat': "Milieux urbains où il est souvent taillé pour limiter son développement.",
        'image_path': "plante_hura_crepitans.jpg"
    },
    {
        'id': 1008,
        'nom_pei': "Bois de fer bâtard",
        'nom_scientifique': "Sideroxylon borbonicum",
        'origine': "Endémique",
        'uicn': "VU", # Vulnérable
        'identification': "Petit arbre avec un tronc épais (max 10m). Les fleurs orange et les fruits à pulpe poisseuse sont regroupés en grappes directement sur les branches sans feuilles.",
        'habitat': "Forêts de basse et moyenne altitude.",
        'image_path': "plante_sideroxylon_borbonicum.jpg"
    },
    {
        'id': 1009,
        'nom_pei': "Figuier blanc",
        'nom_scientifique': "Ficus lateriflora",
        'origine': "Endémique",
        'uicn': "CR", # En danger critique
        'identification': "Feuilles adultes caduques alternes, ovales, pointues et sinueuses sur les bords. Les fruits, initialement verts, rougissent à maturité.",
        'habitat': "Forêts des Hauts, pouvant vivre jusqu'à 1500 mètres d'altitude.",
        'image_path': "plante_ficus_lateriflora.jpg"
    },
    {
        'id': 1010,
        'nom_pei': "Persil marron",
        'nom_scientifique': "Pilea urticifolia",
        'origine': "Endémique",
        'uicn': "LC", # Préoccupation mineure
        'identification': "Plante rameuse de 40 à 60 cm. Tige verte tirant sur le rouge. Feuilles charnues et dentées de tailles différentes, surmontées d'une inflorescence en épi de minuscules fleurs jaunes.",
        'habitat': "Observable dans toutes les forêts humides, souvent avec le pourpier marron.",
        'image_path': "plante_pilea_urticifolia.jpg"
    },
    {
        'id': 1011,
        'nom_pei': "Fanjan mâle",
        'nom_scientifique': "Cyathea borbonica",
        'origine': "Indigène",
        'uicn': "CR", # En danger critique
        'identification': "Fougère au port arborescent très reconnaissable typique des Hauts. Pousse sous forme de stipe (tronc compact).",
        'habitat': "Très fréquente dans les zones humides à toutes les altitudes.",
        'image_path': "plante_cyathea_borbonica.jpg"
    },
    {
        'id': 1012,
        'nom_pei': "Bois de Noël",
        'nom_scientifique': "Ardisia crenata",
        'origine': "Introduit (Envahissante)",
        'uicn': "NE",
        'identification': "Petit arbuste de 1,5 mètre de haut. Petit feuillage recouvrant le sol. Comportement colonisateur très marqué qui empêche les autres espèces de profiter de la lumière.",
        'habitat': "Sous-bois humide de basse ou moyenne altitude.",
        'image_path': "plante_ardisia_crenata.jpg"
    },
    {
        'id': 1013,
        'nom_pei': "Jacaranda",
        'nom_scientifique': "Jacaranda mimosifolia",
        'origine': "Introduit",
        'uicn': "NE",
        'identification': "",
        'habitat': "",
        'image_path': "plante_jacaranda_mimosifolia.jpg"
    },
    {
        'id': 1014,
        'nom_pei': "Flamboyant",
        'nom_scientifique': "Delonix regia",
        'origine': "Introduit",
        'uicn': "NE",
        'identification': "",
        'habitat': "",
        'image_path': "plante_delonix_regia.jpg"
    },
    {
        'id': 1015,
        'nom_pei': "Frangipanier",
        'nom_scientifique': "Plumeria rubra",
        'origine': "Introduit",
        'uicn': "NE",
        'identification': "",
        'habitat': "",
        'image_path': "plante_plumeria_rubrajpg"
    }
]
espece_zone_plants = [
    {'espece_id': 1001, 'zone_id': 1}, 
    {'espece_id': 1002, 'zone_id': 1}, 
    {'espece_id': 1003, 'zone_id': 1}, 
    {'espece_id': 1004, 'zone_id': 1}, 
    {'espece_id': 1004, 'zone_id': 3}, 
    {'espece_id': 1005, 'zone_id': 2}, 
    {'espece_id': 1006, 'zone_id': 2}, 
    {'espece_id': 1007, 'zone_id': 2}, 
    {'espece_id': 1008, 'zone_id': 3}, 
    {'espece_id': 1009, 'zone_id': 4}, 
    {'espece_id': 1010, 'zone_id': 4}, 
    {'espece_id': 1011, 'zone_id': 4}, 
    {'espece_id': 1012, 'zone_id': 4},
    {'espece_id': 1013, 'zone_id': 2}, 
    {'espece_id': 1014, 'zone_id': 1},
    {'espece_id': 1014, 'zone_id': 2},
    {'espece_id': 1015, 'zone_id': 1},
    {'espece_id': 1015, 'zone_id': 2} 
]

# --- Base de données statique pour les Reptiles ---
# IDs : 2001–2018 (hors plage oiseaux ~1-999 et plantes 1001-1012)
reptiles_list = [
    {
        'id': 2001,
        'nom_pei': "Gecko vert de Manapany",
        'nom_scientifique': "Phelsuma inexpectata",
        'origine': "Endémique",
        'uicn': "CR",
        'identification': "Vert pomme, taches rouges sur le dos, deux bandes blanches longitudinales partant des yeux.",
        'habitat': "Fine bande littorale, fourrés de vacoas, rochers (saxicole). Endémique strict du littoral sud de Manapany-les-Bains.",
        'image_path': "reptile_phelsuma_inexpectata.jpg"
    },
    {
        'id': 2002,
        'nom_pei': "Gecko vert de Bourbon",
        'nom_scientifique': "Phelsuma borbonica",
        'origine': "Endémique",
        'uicn': "EN",
        'identification': "Vert à bleu, taches rouges, coloration de la tête variable selon les individus.",
        'habitat': "Forêts humides de moyenne altitude, entre 400 et 1000m. Arboricole, inféodé aux grands arbres natifs.",
        'image_path': "reptile_phelsuma_borbonica.jpg"
    },
    {
        'id': 2003,
        'nom_pei': "Scinque de Bouton",
        'nom_scientifique': "Cryptoblepharus boutonii",
        'origine': "Indigène",
        'uicn': "NE",
        'identification': "Corps aplati, brun à noir avec rayures claires longitudinales. Lézard diurne très agile.",
        'habitat': "Rochers littoraux, zone de balancement des marées. Présent sur les côtes ouest et nord de l'île.",
        'image_path': "reptile_cryptoblepharus_boutonii.jpg"
    },
    {
        'id': 2004,
        'nom_pei': "Grand gecko vert de Madagascar",
        'nom_scientifique': "Phelsuma grandis",
        'origine': "Exotique Envahissante",
        'uicn': "NE",
        'identification': "Jusqu'à 30 cm, vert éclatant, tache rouge en forme de V entre les yeux. Le plus grand gecko diurne présent sur l'île.",
        'habitat': "Milieux dégradés, jardins, vergers, bambouseraies. Très adaptable, colonise les milieux urbains et périurbains.",
        'image_path': "reptile_phelsuma_grandis.jpg"
    },
    {
        'id': 2005,
        'nom_pei': "Gecko vert à trois taches rouges",
        'nom_scientifique': "Phelsuma laticauda",
        'origine': "Exotique Envahissante",
        'uicn': "NE",
        'identification': "Teinte jaune or caractéristique sur la nuque, trois bandes rouges sur le bas du dos.",
        'habitat': "Milieux jardinés, urbains et péri-urbains, bananeraies. Commun dans les zones d'habitation.",
        'image_path': "reptile_phelsuma_laticauda.jpg"
    },
    {
        'id': 2006,
        'nom_pei': "Gecko vert de Maurice",
        'nom_scientifique': "Phelsuma cepediana",
        'origine': "Exotique",
        'uicn': "NE",
        'identification': "Vert-bleu, taches rouges sur le dos, queue d'un bleu intense chez le mâle.",
        'habitat': "Arboricole, fréquente les milieux dégradés et les jardins riches en végétation.",
        'image_path': "reptile_phelsuma_cepediana.jpg"
    },
    {
        'id': 2007,
        'nom_pei': "Gecko vert des Seychelles",
        'nom_scientifique': "Phelsuma astriata",
        'origine': "Exotique",
        'uicn': "NE",
        'identification': "Vert avec petites taches rouges, ligne longitudinale sombre sur les flancs.",
        'habitat': "Jardins et vergers de basse altitude. Espèce thermophile peu présente en altitude.",
        'image_path': "reptile_phelsuma_astriata.jpg"
    },
    {
        'id': 2008,
        'nom_pei': "Gecko vert à ligne noire",
        'nom_scientifique': "Phelsuma lineata",
        'origine': "Exotique",
        'uicn': "NE",
        'identification': "Vert clair, large ligne noire prononcée sur chaque flanc. Très reconnaissable de profil.",
        'habitat': "Arboricole, colonise les murs et les jardins arborés. Présent dans les zones urbaines et périurbaines.",
        'image_path': "reptile_phelsuma_lineata.jpg"
    },
    {
        'id': 2009,
        'nom_pei': "Hémiphyllodactyle indo-pacifique",
        'nom_scientifique': "Hemiphyllodactylus typus",
        'origine': "Exotique",
        'uicn': "NE",
        'identification': "Très fin et allongé, gris-brun, aspect translucide. Gecko nocturne discret.",
        'habitat': "Forêts humides de moyenne altitude. Observé notamment à Mare Longue (Sud-Est).",
        'image_path': "reptile_hemiphyllodactylus_typus.jpg"
    },
    {
        'id': 2010,
        'nom_pei': "Hémidactyle africain",
        'nom_scientifique': "Hemidactylus mercatorius",
        'origine': "Exotique",
        'uicn': "NE",
        'identification': "Nocturne, gris ou beige, chevrons sombres sur le dos. Différenciable de H. frenatus par l'absence de vocalisation.",
        'habitat': "Espèce commensale des villes et des forêts dégradées. Présent sur toute la côte.",
        'image_path': "reptile_hemidactylus_mercatorius.jpg"
    },
    {
        'id': 2011,
        'nom_pei': "Hémidactyle à petites taches",
        'nom_scientifique': "Hemidactylus parvimaculatus",
        'origine': "Exotique",
        'uicn': "NE",
        'identification': "Nocturne clair, dos couvert de petites taches sombres régulières et bien définies.",
        'habitat': "Milieux secs, ravines côtières et murs en pierre. Favorise les zones chaudes et arides.",
        'image_path': "reptile_hemidactylus_parvimaculatus.jpg"
    },
    {
        'id': 2012,
        'nom_pei': "Gecko des maisons",
        'nom_scientifique': "Hemidactylus frenatus",
        'origine': "Exotique",
        'uicn': "NE",
        'identification': "Gris-rosé clair, peau légèrement translucide. Reconnaissable à ses vocalisations répétitives 'tchik-tchik'.",
        'habitat': "Intérieur et extérieur des habitations, zones urbaines. L'espèce de gecko la plus répandue dans les villes.",
        'image_path': "reptile_hemidactylus_frenatus.jpg"
    },
    {
        'id': 2013,
        'nom_pei': "Gecko blanc",
        'nom_scientifique': "Gehyra mutilata",
        'origine': "Exotique",
        'uicn': "NE",
        'identification': "Blanchâtre à gris clair, peau très fragile se déchirant facilement. Pupille verticale.",
        'habitat': "Commensal strict, vit sur ou dans les bâtiments. Exclusivement lié aux constructions humaines.",
        'image_path': "reptile_gehyra_mutilata.jpg"
    },
    {
        'id': 2014,
        'nom_pei': "Caméléon panthère / Endormi",
        'nom_scientifique': "Furcifer pardalis",
        'origine': "Exotique",
        'uicn': "NE",
        'identification': "Grand caméléon, mâle très coloré (vert, rouge, bleu selon l'humeur), femelle à la teinte terne orangée.",
        'habitat': "Milieux ouverts, lisières de forêts et jardins. Introduit depuis Madagascar, bien établi dans le Nord.",
        'image_path': "reptile_furcifer_pardalis.jpg"
    },
    {
        'id': 2015,
        'nom_pei': "Agame des roches",
        'nom_scientifique': "Agama picticauda",
        'origine': "Exotique Envahissante",
        'uicn': "NE",
        'identification': "Mâle au corps bleu métallique et à la tête jaune vif. Femelle brune tachetée, plus discrète.",
        'habitat': "Littoral chaud et ensoleillé, murs, blocs de pierre. Présent sur toute la côte ouest et la côte nord.",
        'image_path': "reptile_agama_picticauda.jpg"
    },
    {
        'id': 2016,
        'nom_pei': "Agame arlequin",
        'nom_scientifique': "Calotes versicolor",
        'origine': "Exotique Envahissante",
        'uicn': "NE",
        'identification': "Crête épineuse sur la nuque et le dos. La tête et la gorge du mâle deviennent rouge vif en période de reproduction.",
        'habitat': "Milieux secs, savanes et jardins. Fréquente les zones ouvertes et ensoleillées de basse altitude.",
        'image_path': "reptile_calotes_versicolor.jpg"
    },
    {
        'id': 2017,
        'nom_pei': "Couleuvre loup",
        'nom_scientifique': "Lycodon aulicus",
        'origine': "Exotique",
        'uicn': "NE",
        'identification': "Serpent fin, brun avec rayures claires transversales. Totalement inoffensif pour l'Homme.",
        'habitat': "Basse altitude, formations littorales et lisières de bois. Présent sur la côte sud et les zones boisées de basse altitude.",
        'image_path': "reptile_lycodon_aulicus.jpg"
    },
    {
        'id': 2018,
        'nom_pei': "Serpent aveugle",
        'nom_scientifique': "Indotyphlops braminus",
        'origine': "Exotique",
        'uicn': "NE",
        'identification': "Noir ou gris sombre, ressemble à un ver de terre. Tête non distincte de la queue. Espèce parthénogénétique (femelles uniquement).",
        'habitat': "Fouisseur, se réfugie sous les roches et dans la terre humide. Présent dans les jardins et milieux urbains.",
        'image_path': "reptile_indotyphlops_braminus.jpg"
    },
]

# Rattachement des reptiles aux zones géographiques
# Zone 1 : Ouest (Savane, Lagons, sec, littoral ouest)
# Zone 2 : Nord & Est (Milieux Urbains et Agricoles)
# Zone 3 : Sud & Sud Sauvage (Basalte, Forêts basse altitude, littoral sud)
# Zone 4 : Centre & Cirques (Forêts humides, Ravines, altitude)
espece_zone_reptiles = [
    {'espece_id': 2001, 'zone_id': 3},  # Gecko Manapany — endémique du littoral sud
    {'espece_id': 2002, 'zone_id': 4},  # Gecko Bourbon — forêts humides de moyenne altitude
    {'espece_id': 2003, 'zone_id': 1},  # Scinque de Bouton — rochers littoraux (ouest/nord)
    {'espece_id': 2003, 'zone_id': 2},
    {'espece_id': 2004, 'zone_id': 2},  # Grand gecko Madagascar — milieux urbains/jardins
    {'espece_id': 2004, 'zone_id': 1},
    {'espece_id': 2005, 'zone_id': 2},  # Gecko 3 taches rouges — milieux urbains/jardins
    {'espece_id': 2005, 'zone_id': 1},
    {'espece_id': 2006, 'zone_id': 2},  # Gecko Maurice — jardins/milieux dégradés
    {'espece_id': 2007, 'zone_id': 2},  # Gecko Seychelles — jardins basse altitude
    {'espece_id': 2007, 'zone_id': 1},
    {'espece_id': 2008, 'zone_id': 2},  # Gecko ligne noire — jardins/murs urbains
    {'espece_id': 2009, 'zone_id': 4},  # Hémiphyllodactyle — forêts humides (Mare Longue)
    {'espece_id': 2009, 'zone_id': 3},
    {'espece_id': 2010, 'zone_id': 2},  # Hémidactyle africain — villes/forêts dégradées
    {'espece_id': 2010, 'zone_id': 1},
    {'espece_id': 2011, 'zone_id': 1},  # Hémidactyle petites taches — milieux secs/ravines
    {'espece_id': 2011, 'zone_id': 3},
    {'espece_id': 2012, 'zone_id': 2},  # Gecko des maisons — zones urbaines (ubiquiste)
    {'espece_id': 2012, 'zone_id': 1},
    {'espece_id': 2012, 'zone_id': 3},
    {'espece_id': 2013, 'zone_id': 2},  # Gecko blanc — bâtiments (ubiquiste)
    {'espece_id': 2013, 'zone_id': 1},
    {'espece_id': 2014, 'zone_id': 2},  # Caméléon panthère — nord/jardins
    {'espece_id': 2015, 'zone_id': 1},  # Agame des roches — littoral chaud ouest/nord
    {'espece_id': 2015, 'zone_id': 2},
    {'espece_id': 2016, 'zone_id': 1},  # Agame arlequin — savanes/milieux secs
    {'espece_id': 2016, 'zone_id': 3},
    {'espece_id': 2017, 'zone_id': 3},  # Couleuvre loup — littoral et bois du sud
    {'espece_id': 2017, 'zone_id': 1},
    {'espece_id': 2018, 'zone_id': 2},  # Serpent aveugle — jardins/terre humide (ubiquiste)
    {'espece_id': 2018, 'zone_id': 1},
    {'espece_id': 2018, 'zone_id': 3},
]

def attribuer_zones(habitat_text, main_name=""):
    """
    Analyse le texte et retourne la liste des IDs de zones.
    Utilise les Regex pour éviter les faux positifs (ex: 'mer' dans 'commercial').
    """
    zones_trouvees = []
    
    if not habitat_text:
        return [1, 2] 

    texte_min = habitat_text.lower()
    
    for zone_id, data in ZONES_DATA.items():
        for mot in data["mots_cles"]:
            pattern = r'\b' + re.escape(mot) + r's?\b'
            
            if re.search(pattern, texte_min):
                zones_trouvees.append(zone_id)
                break 
                
    if not zones_trouvees:
        if "pétrel" in main_name.lower() or "paille-en-queue" in main_name.lower():
            zones_trouvees = [4, 5] 
        else:
            zones_trouvees = [2] 
            
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
            
            hab = get_description_blocks(soup_det)[1] 
            
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
    # 1. Scraping des données aviaires
    b_data, z_data, ez_data = scrape_seor_data()
    
    # 2. Ajout de la flore (données statiques)
    print("🌿 Ajout des données botaniques à la base...")
    b_data.extend(plants_list)
    ez_data.extend(espece_zone_plants)

    # 3. Ajout des reptiles (données statiques)
    print("🦎 Ajout des données herpétologiques à la base...")
    b_data.extend(reptiles_list)
    ez_data.extend(espece_zone_reptiles)
    
    # 4. Création et alimentation de la base SQLite
    generate_sqlite(b_data, z_data, ez_data)