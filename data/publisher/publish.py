"""Publication des tables finales dbt (schéma marts) dans les tables de l'application (schéma beloved_designs).

Décision « Publication des données Excel dans l'application » (memory/decisions.md, 2026-11-23) :
- uniquement des INSERT/UPDATE de lignes : jamais de CREATE/ALTER/DROP/TRUNCATE, jamais de DELETE ;
- une seule transaction : soit tout est publié, soit rien ;
- produit retrouvé par le début de son slug (référence Excel, ex. "sac-001-") : renommage sans doublon ;
  un produit créé à la main dans l'admin (slug sans préfixe de référence) n'est jamais touché ;
- clients : créés seulement s'ils n'existent pas (téléphone) ; un compte existant n'est JAMAIS modifié ;
  les comptes créés n'ont pas de mot de passe utilisable.

Usage (dans le conteneur `data`, après `dbt build`) :
    python /app/publisher/publish.py            # publie
    python /app/publisher/publish.py --dry-run  # fait tout puis annule (ROLLBACK) : montre ce qui changerait
    python /app/publisher/publish.py --sans-clients  # produits + promotions seulement, aucun compte client
"""
from __future__ import annotations

import argparse
import os
import sys

import psycopg
from psycopg.rows import dict_row

APP = "beloved_designs"
MARTS = "marts"
LOCAL_HOSTS = {"db", "localhost", "127.0.0.1"}

# Valeur qui ne correspond à aucun hash BCrypt : la connexion est refusée tant que le client
# n'a pas défini de mot de passe (moyen d'activation à décider, voir decisions.md).
NO_PASSWORD = "!compte-importe-sans-mot-de-passe"
CLIENT_ROLE = "CLIENT"


def connect() -> psycopg.Connection:
    missing = [v for v in ("DB_HOST", "DB_PORT", "DB_NAME", "DB_USERNAME", "DB_PASSWORD") if not os.environ.get(v)]
    if missing:
        sys.exit(f"Variables manquantes : {', '.join(missing)}")
    host = os.environ["DB_HOST"]
    if host not in LOCAL_HOSTS and os.environ.get("PUBLISH_ALLOW_REMOTE") != "1":
        sys.exit(
            f"Refus : DB_HOST={host} n'est pas une base locale. La publication en production se fait "
            "uniquement sur demande explicite, avec PUBLISH_ALLOW_REMOTE=1."
        )
    return psycopg.connect(
        host=host,
        port=int(os.environ["DB_PORT"]),
        dbname=os.environ["DB_NAME"],
        user=os.environ["DB_USERNAME"],
        password=os.environ["DB_PASSWORD"],
        connect_timeout=15,
        row_factory=dict_row,
    )


def fetch(cur, query: str, params=None) -> list[dict]:
    cur.execute(query, params)
    return cur.fetchall()


def publish_products(cur) -> dict[str, int]:
    """Retourne {slug_prefix: product_id} pour lier ensuite les promotions."""
    products = fetch(cur, f"SELECT * FROM {MARTS}.app_products ORDER BY reference")
    if not products:
        sys.exit(f"{MARTS}.app_products est vide : lancer `dbt build` avant de publier.")

    categories = {
        row["slug"]: row["id"]
        for row in fetch(cur, f"SELECT id, slug FROM {APP}.categories WHERE slug = ANY(%s)",
                         ([p["category_slug"] for p in products],))
    }
    missing = sorted({p["category_slug"] for p in products} - set(categories))
    if missing:
        sys.exit(f"Catégorie(s) absente(s) de l'application : {', '.join(missing)} — à créer d'abord.")

    ids, created, updated = {}, 0, 0
    for p in products:
        # Préfixe composé uniquement de [a-z0-9-] (macro slugify) : aucun caractère spécial pour LIKE.
        matches = fetch(cur, f"SELECT id, slug FROM {APP}.products WHERE slug LIKE %s", (p["slug_prefix"] + "%",))
        if len(matches) > 1:
            sys.exit(f"Ambigu : plusieurs produits commencent par « {p['slug_prefix']} » : "
                     + ", ".join(m["slug"] for m in matches))
        # Le nouveau slug ne doit pas appartenir à un autre produit (contrainte UNIQUE de l'application).
        other = fetch(cur, f"SELECT id FROM {APP}.products WHERE slug = %s", (p["slug"],))
        if other and (not matches or other[0]["id"] != matches[0]["id"]):
            sys.exit(f"Le slug « {p['slug']} » est déjà utilisé par un autre produit (id {other[0]['id']}).")

        values = (p["name"], p["slug"], p["price"], p["currency"], p["stock_quantity"], p["is_active"])
        if matches:
            product_id = matches[0]["id"]
            cur.execute(
                f"""UPDATE {APP}.products
                    SET name = %s, slug = %s, price = %s, currency = %s, stock_quantity = %s,
                        is_active = %s, updated_at = now()
                    WHERE id = %s""",
                values + (product_id,),
            )
            updated += 1
        else:
            cur.execute(
                f"""INSERT INTO {APP}.products
                        (name, slug, price, currency, stock_quantity, is_active, is_featured, created_at, updated_at)
                    VALUES (%s, %s, %s, %s, %s, %s, false, now(), now())
                    RETURNING id""",
                values,
            )
            product_id = cur.fetchone()["id"]
            created += 1
        # Lien catégorie ajouté s'il manque ; les catégories ajoutées à la main dans l'admin sont conservées.
        cur.execute(
            f"INSERT INTO {APP}.product_categories (product_id, category_id) VALUES (%s, %s) ON CONFLICT DO NOTHING",
            (product_id, categories[p["category_slug"]]),
        )
        ids[p["slug_prefix"]] = product_id

    print(f"  Produits      : {created} créés, {updated} mis à jour")
    return ids


def publish_promotions(cur, product_ids: dict[str, int]) -> None:
    promotions = fetch(cur, f"SELECT * FROM {MARTS}.app_promotions ORDER BY product_reference, starts_at")
    created, updated = 0, 0
    for promo in promotions:
        product_id = product_ids[promo["product_slug_prefix"]]
        # Une promotion est reconnue par son produit et sa date de début.
        existing = fetch(
            cur,
            f"""SELECT p.id FROM {APP}.promotions p
                JOIN {APP}.product_promotions pp ON pp.promotion_id = p.id
                WHERE pp.product_id = %s AND p.starts_at = %s""",
            (product_id, promo["starts_at"]),
        )
        values = (promo["label"], promo["discount_percent"], promo["starts_at"], promo["ends_at"], promo["is_active"])
        if existing:
            cur.execute(
                f"""UPDATE {APP}.promotions
                    SET label = %s, discount_percent = %s, starts_at = %s, ends_at = %s, is_active = %s
                    WHERE id = %s""",
                values + (existing[0]["id"],),
            )
            updated += 1
        else:
            cur.execute(
                f"""INSERT INTO {APP}.promotions (label, discount_percent, starts_at, ends_at, is_active)
                    VALUES (%s, %s, %s, %s, %s) RETURNING id""",
                values,
            )
            promotion_id = cur.fetchone()["id"]
            cur.execute(
                f"INSERT INTO {APP}.product_promotions (product_id, promotion_id) VALUES (%s, %s)",
                (product_id, promotion_id),
            )
            created += 1
    print(f"  Promotions    : {created} créées, {updated} mises à jour")


def publish_customers(cur) -> None:
    customers = fetch(cur, f"SELECT * FROM {MARTS}.app_customers ORDER BY customer_number")
    role = fetch(cur, f"SELECT id FROM {APP}.roles WHERE name = %s", (CLIENT_ROLE,))
    if not role:
        sys.exit(f"Rôle {CLIENT_ROLE} absent de l'application.")
    created, skipped = 0, 0
    for c in customers:
        if fetch(cur, f"SELECT id FROM {APP}.users WHERE phone = %s", (c["phone"],)):
            skipped += 1  # compte déjà existant (inscrit sur le site ou déjà importé) : jamais modifié
            continue
        cur.execute(
            f"""INSERT INTO {APP}.users (phone, password_hash, first_name, last_name, created_at, updated_at)
                VALUES (%s, %s, %s, %s, now(), now()) RETURNING id""",
            (c["phone"], NO_PASSWORD, c["first_name"], c["last_name"]),
        )
        user_id = cur.fetchone()["id"]
        cur.execute(f"INSERT INTO {APP}.user_roles (user_id, role_id) VALUES (%s, %s)", (user_id, role[0]["id"]))
        created += 1
    print(f"  Clients       : {created} créés, {skipped} déjà existants (non modifiés)")


def main() -> None:
    parser = argparse.ArgumentParser(description="Publie les marts dbt dans les tables de l'application.")
    parser.add_argument("--dry-run", action="store_true", help="tout exécuter puis annuler (rien n'est enregistré)")
    parser.add_argument(
        "--sans-clients",
        action="store_true",
        help="ne pas créer les comptes clients (tant que leur activation n'est pas décidée, un client importé "
             "ne peut ni se connecter ni s'inscrire avec son numéro)",
    )
    args = parser.parse_args()

    conn = connect()
    try:
        with conn.cursor() as cur:
            print(f"Publication vers {os.environ['DB_HOST']} / {APP}{' (essai à blanc)' if args.dry_run else ''}")
            product_ids = publish_products(cur)
            publish_promotions(cur, product_ids)
            if args.sans_clients:
                print("  Clients       : ignorés (--sans-clients), aucun compte créé")
            else:
                publish_customers(cur)
        if args.dry_run:
            conn.rollback()
            print("Mode --dry-run : tout a été annulé, rien n'est enregistré.")
        else:
            conn.commit()
            print("Publication terminée.")
    except BaseException:
        conn.rollback()  # une seule transaction : en cas d'erreur, rien n'est publié
        raise
    finally:
        conn.close()


if __name__ == "__main__":
    main()
