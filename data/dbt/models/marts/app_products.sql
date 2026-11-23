-- Produits au format de la table beloved_designs.products (+ catégorie et clés de rapprochement).
-- Publiés dans l'application par data/publisher/publish.py (dbt n'écrit jamais dans beloved_designs).
-- Décision 2026-11-23 : une référence Excel = un produit ; taille et couleur ajoutées au nom ;
-- le slug commence par la référence, ce qui permet de retrouver le produit au prochain import.
with products as (
    select * from {{ ref('stg_produits') }} where is_valid
),

stock as (
    select product_reference, stock_quantity from {{ ref('stg_stock') }} where is_valid
),

named as (
    select
        p.product_reference,
        case
            when coalesce(p.size, p.color) is null then p.product_name
            else p.product_name || ' — ' || concat_ws(', ', p.size, p.color)
        end                                           as name,
        p.sale_price,
        p.is_active,
        p.category_slug,
        s.stock_quantity
    from products p
    left join stock s on s.product_reference = p.product_reference
)

select
    product_reference                                          as reference,
    {{ slugify("product_reference") }} || '-'                  as slug_prefix,  -- clé de rapprochement : "sac-001-"
    {{ slugify("product_reference || ' ' || name") }}          as slug,
    name,
    sale_price                                                 as price,
    'FCFA'::varchar(10)                                        as currency,
    coalesce(stock_quantity, 0)                                as stock_quantity,  -- absent de l'inventaire = 0
    is_active,
    category_slug
from named
