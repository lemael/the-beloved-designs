# Service `data` : bucket (fichier Excel) -> raw -> dbt build -> publication dans l'application.
# Décision du 2026-11-24 (decisions.md) : lancement À LA MAIN uniquement (Deploy/Redeploy dans Railway).

resource "railway_service" "data" {
  name       = "data"
  project_id = var.project_id

  source_repo        = "lemael/the-beloved-designs"
  source_repo_branch = "main"
  root_directory     = "/data" # data/Dockerfile : même image qu'en local

  start_command = "python run_pipeline.py"

  # Un job qui se termine : ne jamais le relancer automatiquement (sinon la publication serait rejouée).
  restart_policy_type = "NEVER"

  # Pas de déploiement à chaque push : ce fichier n'existe pas, donc aucun changement ne le déclenche.
  # Pour lancer la chaîne : bouton Deploy / Redeploy du service dans Railway.
  watch_patterns = ["data/.lancement-manuel-uniquement"]

  regions = [
    {
      region       = var.region
      num_replicas = 1
    }
  ]
}

resource "railway_variable_collection" "data" {
  environment_id = var.environment_id
  service_id     = railway_service.data.id

  # Aucune valeur secrète ici : uniquement des RÉFÉRENCES Railway, résolues par Railway au lancement.
  # `$${{...}}` produit le texte littéral `${{...}}` (sinon Terraform l'interpréterait).
  variables = [
    # Base de données : la même que le backend (projet agri-dashboard, voir decisions.md)
    { name = "DB_HOST", value = "$${{${var.backend_service_name}.DB_HOST}}" },
    { name = "DB_PORT", value = "$${{${var.backend_service_name}.DB_PORT}}" },
    { name = "DB_NAME", value = "$${{${var.backend_service_name}.DB_NAME}}" },
    { name = "DB_USERNAME", value = "$${{${var.backend_service_name}.DB_USERNAME}}" },
    { name = "DB_PASSWORD", value = "$${{${var.backend_service_name}.DB_PASSWORD}}" },

    # Bucket du fichier Excel
    { name = "S3_ENDPOINT", value = "$${{${var.bucket_name}.ENDPOINT}}" },
    { name = "S3_BUCKET", value = "$${{${var.bucket_name}.BUCKET}}" },
    { name = "S3_REGION", value = "$${{${var.bucket_name}.REGION}}" },
    { name = "S3_ACCESS_KEY_ID", value = "$${{${var.bucket_name}.ACCESS_KEY_ID}}" },
    { name = "S3_SECRET_ACCESS_KEY", value = "$${{${var.bucket_name}.SECRET_ACCESS_KEY}}" },
    { name = "S3_ADDRESSING_STYLE", value = "virtual" }, # style d'URL du bucket Railway : virtual-host

    # Garde-fous du chargeur et du script de publication : ce service a le droit d'écrire en production.
    { name = "LOADER_ALLOW_REMOTE", value = "1" },
    { name = "PUBLISH_ALLOW_REMOTE", value = "1" },
  ]
}
