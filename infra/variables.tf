# Identifiants Railway du projet existant « the beloved designs » (ce ne sont pas des secrets).
# Le projet, l'environnement, le backend, le frontend et la base ne sont PAS gérés par Terraform :
# on ne fait que s'y rattacher (voir decisions.md, entrée Terraform du 2026-11-23).

variable "project_id" {
  description = "Projet Railway « the beloved designs »."
  type        = string
  default     = "10558435-fa99-4b15-b229-a275c3537224"
}

variable "environment_id" {
  description = "Environnement production du projet."
  type        = string
  default     = "e3bca0c3-9cb4-402b-bd5c-286ebe44192b"
}

variable "region" {
  description = "Région des services (la même que backend et frontend)."
  type        = string
  default     = "europe-west4-drams3a"
}

variable "bucket_name" {
  description = "Nom du bucket Railway du fichier Excel (créé hors Terraform : le provider n'a pas de ressource bucket)."
  type        = string
  default     = "excel-boutique"
}

variable "backend_service_name" {
  description = "Service dont on référence les variables DB_* (même base que l'application)."
  type        = string
  default     = "backend"
}
