-- Promotions au format de beloved_designs.promotions (+ produit lié via product_promotions).
-- Dates : début à 00:00 et fin à 23:59:59 heure du Cameroun, converties en instants (timestamptz).
with promotions as (
    select * from {{ ref('stg_promotions') }} where is_valid
),

products as (
    select reference, slug_prefix, name from {{ ref('app_products') }}
)

select
    pr.product_reference                                                as product_reference,
    p.slug_prefix                                                       as product_slug_prefix,
    left(p.name || ' : -' || pr.discount_percent || ' %', 150)          as label,
    pr.discount_percent::numeric(5, 2)                                  as discount_percent,
    pr.promo_price,
    (pr.starts_on::timestamp at time zone 'Africa/Douala')              as starts_at,
    ((pr.ends_on + 1)::timestamp at time zone 'Africa/Douala') - interval '1 second' as ends_at,
    pr.status in ('ACTIVE', 'A_VENIR')                                  as is_active
from promotions pr
join products p on p.reference = pr.product_reference
