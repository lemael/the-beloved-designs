terraform {
  required_providers {
    railway = {
      source  = "terraform-community-providers/railway"
      version = "~> 0.6.3" # provider communautaire : ne pas changer de version sans relire le plan
    }
  }
}

# Authentification : variable d'environnement RAILWAY_TOKEN (jamais écrite dans un fichier du dépôt).
provider "railway" {}
