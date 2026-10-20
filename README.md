# DailyPull Image Generator

Outil de génération d'illustrations pour les cartes DailyPull. Le seul fournisseur est
FLUX.1 Schnell local, appelé via `~/scripts/local-media.py` sur le PC Windows. Le secours
payant 1min.ai a été retiré le 21/10/2026 (ops#2908) : si le fournisseur local ne répond
pas, l'API renvoie `503 UNAVAILABLE` et rien n'est généré ailleurs. La génération n'ajoute
ni faits ni cartes à l'application et ne publie rien.

### Revue obligatoire

Les réponses portent `REVIEW_REQUIRED`. Vérifier l'illustration, les faits de la
carte sur leurs sources, les droits et la doctrine DailyPull avant toute utilisation.
Le candidat local contient `candidate.json`, `source.png` et `image.png` dans le
répertoire parent de `localPath`. Suivre `~/.claude/commands/_local-media.md` pour
le reçu de revue et l'export. Le statut du fournisseur n'est jamais une autorisation
de publier.

Le script travaille par SSH sur `windows-pc`, avec budgets de temps bornés. Code 20,
délai dépassé ou taille hors gabarit (`768x768`, `768x1024`, `1024x1024`, `1024x1280`,
`1536x1024`) : réponse HTTP 503. Code 30 : brief refusé, réponse HTTP 400.
`IMAGEGEN_LOCAL_ENABLED=false` désactive le fournisseur (toute génération répond 503).
`IMAGEGEN_LOCAL_SCRIPT` permet de préciser le chemin du helper.

## Modèle

| Modèle | ID |
|--------|----|
| FLUX.1 Schnell (local, PC Windows) | `flux1-schnell-local` |

## Prérequis

- Java 25+
- Maven 3.9+
- Accès SSH `windows-pc` et `~/scripts/local-media.py`

## Installation

```bash
git clone https://github.com/YoannSACHOT/dailypull-imagegen.git
cd dailypull-imagegen
cp src/main/resources/application-dev.yml.example src/main/resources/application-dev.yml
```

## Lancement

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Le serveur démarre sur le port **8090**.

## API

### Générer une image

```bash
curl -X POST http://localhost:8090/api/images/generate \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "A motivational card with golden sunrise, courage theme, elegant typography",
    "size": "1024x1024"
  }'
```

### Générer un batch

```bash
curl -X POST http://localhost:8090/api/images/generate/batch \
  -H "Content-Type: application/json" \
  -d '{
    "requests": [
      {"prompt": "Courage card, mountain peak at dawn"},
      {"prompt": "Serenity card, calm ocean under moonlight"}
    ]
  }'
```

### Lister les modèles disponibles

```bash
curl http://localhost:8090/api/images/models
```

## Paramètres de génération

| Paramètre | Défaut | Options |
|-----------|--------|---------|
| `prompt` | *(requis)* | |
| `model` | `FLUX_SCHNELL_LOCAL` | `FLUX_SCHNELL_LOCAL` |
| `size` | `1024x1024` | `768x768`, `768x1024`, `1024x1024`, `1024x1280`, `1536x1024` |

Le candidat est écrit par `local-media.py` ; son chemin est renvoyé dans `localPath`.

## Stack technique

- Java 25
- Spring Boot 4.0.3
- Jackson 3
- JUnit 5 + Mockito

## Licence

Projet interne [JIXTER](https://github.com/YoannSACHOT).
