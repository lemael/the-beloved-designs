"""Chargeur Excel -> schéma `raw` (étape Extract + Load du pipeline ELT).

Décisions (memory/decisions.md, 2026-11-23) :
- les données sont chargées TELLES QUELLES : toutes les valeurs en texte, aucune correction
  (le nettoyage et le typage sont faits par dbt, dans le schéma staging) ;
- chaque chargement REMPLACE les tables raw par le contenu du fichier (le fichier contient tout l'historique) ;
- le chargeur n'écrit QUE dans le schéma raw (jamais dans beloved_designs, base partagée).

Usage (dans le conteneur `data`) :
    python /app/loader/load_excel.py /app/samples/fichier.xlsx
    python /app/loader/load_excel.py /app/samples/fichier.xlsx --dry-run   # lit et affiche, n'écrit rien
"""
from __future__ import annotations

import argparse
import os
import re
import sys
import unicodedata
from datetime import date, datetime, time
from pathlib import Path

import psycopg
from openpyxl import load_workbook
from psycopg import sql

# Seul schéma dans lequel le chargeur a le droit d'écrire.
RAW_SCHEMA = "raw"

# Feuilles chargées -> nom de la table dans raw.
# Feuilles volontairement NON chargées : "À lire" (documentation) et les feuilles calculées
# "Catalogue produits" et "Produits vendus" (ce sont des résultats que dbt doit produire, pas des sources).
# Liste encore "à décider" dans decisions.md : la modifier ici si besoin.
SHEETS = {
    "Produits": "produits",
    "Stock": "stock",
    "Ventes": "ventes",
    "Ravitaillements": "ravitaillements",
    "Clients actuels": "clients",
    "Produits en promotion": "promotions",
}

# Hôtes considérés comme locaux. Ailleurs (ex. Railway), il faut LOADER_ALLOW_REMOTE=1 :
# protège contre un chargement accidentel dans la base partagée pendant le développement.
LOCAL_HOSTS = {"db", "localhost", "127.0.0.1"}

METADATA_COLUMNS = ["_source_file", "_sheet", "_row_number", "_loaded_at"]


def to_column_name(header: object, index: int) -> str:
    """'Prix de vente (FCFA)' -> 'prix_de_vente_fcfa' ; en-tête vide -> 'colonne_<n>'."""
    if header is None or str(header).strip() == "":
        return f"colonne_{index}"
    text = unicodedata.normalize("NFKD", str(header)).encode("ascii", "ignore").decode()
    text = re.sub(r"[^a-zA-Z0-9]+", "_", text).strip("_").lower()
    if not text:
        return f"colonne_{index}"
    return text if not text[0].isdigit() else f"c_{text}"


def unique_names(names: list[str]) -> list[str]:
    """Deux en-têtes identiques -> nom, nom_2."""
    seen: dict[str, int] = {}
    result = []
    for name in names:
        if name in seen:
            seen[name] += 1
            result.append(f"{name}_{seen[name]}")
        else:
            seen[name] = 1
            result.append(name)
    return result


def to_text(value: object) -> str | None:
    """Valeur de cellule -> texte, sans la déformer (pas de correction ici)."""
    if value is None:
        return None
    if isinstance(value, bool):
        return "true" if value else "false"
    if isinstance(value, float) and value.is_integer():
        return str(int(value))  # 45000.0 -> '45000'
    if isinstance(value, (datetime, date, time)):
        return value.isoformat()
    return str(value)


def read_sheet(ws, ws_formulas, source_file: str) -> tuple[list[str], list[list[str | None]], list[str]]:
    """Retourne (colonnes, lignes, avertissements) pour une feuille."""
    warnings: list[str] = []
    rows = list(ws.iter_rows(values_only=True))
    if not rows:
        return [], [], [f"feuille « {ws.title} » vide"]

    header = rows[0]
    width = len(header)
    columns = unique_names([to_column_name(h, i + 1) for i, h in enumerate(header)])
    clash = set(columns) & set(METADATA_COLUMNS)
    if clash:
        raise ValueError(f"feuille « {ws.title} » : colonne(s) réservée(s) {sorted(clash)}")

    data = []
    for excel_row, values in enumerate(rows[1:], start=2):
        values = list(values[:width]) + [None] * (width - len(values))
        if all(v is None or (isinstance(v, str) and v.strip() == "") for v in values):
            continue  # ligne entièrement vide : aucune donnée à conserver
        # Formule sans valeur enregistrée : le fichier n'a pas été recalculé/enregistré par Excel ou LibreOffice
        for col, value in enumerate(values):
            formula = ws_formulas.cell(row=excel_row, column=col + 1).value
            if value is None and isinstance(formula, str) and formula.startswith("="):
                warnings.append(
                    f"« {ws.title} » ligne {excel_row}, colonne {columns[col]} : formule sans valeur "
                    "(ouvrir et enregistrer le fichier dans Excel/LibreOffice)"
                )
        data.append([to_text(v) for v in values] + [source_file, ws.title, str(excel_row)])
    return columns, data, warnings


def connect() -> psycopg.Connection:
    missing = [v for v in ("DB_HOST", "DB_PORT", "DB_NAME", "DB_USERNAME", "DB_PASSWORD") if not os.environ.get(v)]
    if missing:
        sys.exit(f"Variables manquantes : {', '.join(missing)}")
    host = os.environ["DB_HOST"]
    if host not in LOCAL_HOSTS and os.environ.get("LOADER_ALLOW_REMOTE") != "1":
        sys.exit(
            f"Refus : DB_HOST={host} n'est pas une base locale. "
            "Pour charger volontairement ailleurs (ex. production), définir LOADER_ALLOW_REMOTE=1."
        )
    return psycopg.connect(
        host=host,
        port=int(os.environ["DB_PORT"]),
        dbname=os.environ["DB_NAME"],
        user=os.environ["DB_USERNAME"],
        password=os.environ["DB_PASSWORD"],
        connect_timeout=15,
    )


def load(path: Path, dry_run: bool) -> None:
    if not path.is_file():
        sys.exit(f"Fichier introuvable : {path}")

    wb_values = load_workbook(path, data_only=True, read_only=False)  # valeurs (résultat des formules)
    wb_formulas = load_workbook(path, data_only=False, read_only=False)  # formules (pour les avertissements)

    missing = [name for name in SHEETS if name not in wb_values.sheetnames]
    if missing:
        sys.exit(f"Feuille(s) absente(s) du fichier : {', '.join(missing)} (feuilles présentes : {wb_values.sheetnames})")
    ignored = [name for name in wb_values.sheetnames if name not in SHEETS]

    tables = {}
    all_warnings = []
    for sheet, table in SHEETS.items():
        columns, rows, warnings = read_sheet(wb_values[sheet], wb_formulas[sheet], path.name)
        tables[table] = (columns, rows)
        all_warnings += warnings

    print(f"Fichier : {path.name}")
    for table, (columns, rows) in tables.items():
        print(f"  {RAW_SCHEMA}.{table:<16} {len(rows):>5} lignes  {len(columns)} colonnes")
    if ignored:
        print(f"  Feuilles non chargées : {', '.join(ignored)}")
    for w in all_warnings:
        print(f"  ATTENTION : {w}")

    if dry_run:
        print("Mode --dry-run : rien n'a été écrit.")
        return

    # Une seule transaction : soit toutes les tables sont remplacées, soit aucune (base jamais à moitié chargée).
    with connect() as conn, conn.cursor() as cur:
        cur.execute(sql.SQL("CREATE SCHEMA IF NOT EXISTS {}").format(sql.Identifier(RAW_SCHEMA)))
        for table, (columns, rows) in tables.items():
            target = sql.Identifier(RAW_SCHEMA, table)
            cur.execute(sql.SQL("DROP TABLE IF EXISTS {}").format(target))
            column_defs = [sql.SQL("{} text").format(sql.Identifier(c)) for c in columns]
            column_defs += [
                sql.SQL("_source_file text NOT NULL"),
                sql.SQL("_sheet text NOT NULL"),
                sql.SQL("_row_number integer NOT NULL"),
                sql.SQL("_loaded_at timestamptz NOT NULL DEFAULT now()"),
            ]
            cur.execute(sql.SQL("CREATE TABLE {} ({})").format(target, sql.SQL(", ").join(column_defs)))
            insert_columns = [sql.Identifier(c) for c in columns + ["_source_file", "_sheet", "_row_number"]]
            copy = sql.SQL("COPY {} ({}) FROM STDIN").format(target, sql.SQL(", ").join(insert_columns))
            with cur.copy(copy) as cp:
                for row in rows:
                    cp.write_row(row)
    print(f"Chargement terminé : {len(tables)} tables remplacées dans le schéma {RAW_SCHEMA}.")


def main() -> None:
    parser = argparse.ArgumentParser(description="Charge un fichier Excel de la boutique dans le schéma raw.")
    parser.add_argument("file", type=Path, help="chemin du fichier .xlsx")
    parser.add_argument("--dry-run", action="store_true", help="lire et afficher le résumé sans rien écrire")
    args = parser.parse_args()
    load(args.file, args.dry_run)


if __name__ == "__main__":
    main()
