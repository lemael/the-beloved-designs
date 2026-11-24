"""Chaîne complète du service Railway `data` : bucket -> raw -> dbt -> application.

Lancé À LA MAIN uniquement (bouton Deploy/Redeploy du service dans Railway), voir decisions.md (2026-11-24).
Étapes :
  1. télécharge le fichier Excel le plus récent du bucket (ou celui de S3_OBJECT_KEY) ;
  2. le charge dans le schéma raw (loader/load_excel.py) ;
  3. dbt build : en cas d'ERREUR, on s'arrête AVANT de publier (les anomalies de saisie ne sont que des avertissements) ;
  4. publie produits et promotions dans l'application (publisher/publish.py --sans-clients,
     sauf PUBLISH_CUSTOMERS=1).
Si le bucket ne contient aucun fichier Excel, rien n'est fait (sortie normale).

Variables (fournies par Railway au service, par référence au bucket et au backend) :
  S3_ENDPOINT, S3_BUCKET, S3_REGION, S3_ACCESS_KEY_ID, S3_SECRET_ACCESS_KEY, [S3_ADDRESSING_STYLE=virtual]
  [S3_OBJECT_KEY]  fichier précis à traiter ; sinon le .xlsx le plus récent du bucket
  DB_* (comme le backend), LOADER_ALLOW_REMOTE=1, PUBLISH_ALLOW_REMOTE=1
Usage local : python run_pipeline.py --file samples/exemple_boutique.xlsx [--dry-run]
"""
from __future__ import annotations

import argparse
import os
import subprocess
import sys
import tempfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
DBT_DIR = HERE / "dbt"


def step(title: str) -> None:
    print(f"\n=== {title} ===", flush=True)


def run(cmd: list[str], cwd: Path = HERE) -> None:
    print("$ " + " ".join(cmd), flush=True)
    result = subprocess.run(cmd, cwd=cwd)
    if result.returncode != 0:
        sys.exit(f"Échec de l'étape (code {result.returncode}) : arrêt, rien de plus n'est publié.")


def download_latest(target_dir: Path) -> Path | None:
    import boto3
    from botocore.config import Config

    missing = [v for v in ("S3_ENDPOINT", "S3_BUCKET", "S3_ACCESS_KEY_ID", "S3_SECRET_ACCESS_KEY") if not os.environ.get(v)]
    if missing:
        sys.exit(f"Variables du bucket manquantes : {', '.join(missing)}")

    s3 = boto3.client(
        "s3",
        endpoint_url=os.environ["S3_ENDPOINT"],
        region_name=os.environ.get("S3_REGION") or "auto",
        aws_access_key_id=os.environ["S3_ACCESS_KEY_ID"],
        aws_secret_access_key=os.environ["S3_SECRET_ACCESS_KEY"],
        config=Config(s3={"addressing_style": os.environ.get("S3_ADDRESSING_STYLE", "virtual")}),
    )
    bucket = os.environ["S3_BUCKET"]

    key = os.environ.get("S3_OBJECT_KEY")
    if not key:
        objects = []
        for page in s3.get_paginator("list_objects_v2").paginate(Bucket=bucket):
            objects += [o for o in page.get("Contents", []) if o["Key"].lower().endswith(".xlsx")]
        if not objects:
            return None
        latest = max(objects, key=lambda o: o["LastModified"])
        key = latest["Key"]
        print(f"Fichier le plus récent du bucket : {key} (déposé le {latest['LastModified']:%d/%m/%Y %H:%M} UTC)")

    destination = target_dir / Path(key).name  # le nom du fichier est gardé : il apparaît dans raw._source_file
    s3.download_file(bucket, key, str(destination))
    return destination


def main() -> None:
    parser = argparse.ArgumentParser(description="Bucket -> raw -> dbt build -> publication.")
    parser.add_argument("--file", type=Path, help="fichier local (sinon : téléchargé depuis le bucket)")
    parser.add_argument("--dry-run", action="store_true", help="publication à blanc (ROLLBACK)")
    args = parser.parse_args()

    with tempfile.TemporaryDirectory() as tmp:
        step("1/4 Fichier Excel")
        excel = args.file if args.file else download_latest(Path(tmp))
        if excel is None:
            print("Aucun fichier .xlsx dans le bucket : rien à faire.")
            return

        step("2/4 Chargement dans raw")
        run([sys.executable, "loader/load_excel.py", str(excel)])

    step("3/4 dbt build")
    run(["dbt", "build", "--project-dir", str(DBT_DIR), "--profiles-dir", str(DBT_DIR)], cwd=DBT_DIR)

    step("4/4 Publication dans l'application")
    publish = [sys.executable, "publisher/publish.py"]
    if os.environ.get("PUBLISH_CUSTOMERS") != "1":
        publish.append("--sans-clients")  # tant que l'activation des comptes importés n'est pas décidée
    if args.dry_run:
        publish.append("--dry-run")
    run(publish)
    print("\nChaîne terminée. Lignes à corriger par la boutique : vue staging.stg_anomalies.")


if __name__ == "__main__":
    main()
