-- Clients au format de beloved_designs.users, UNIQUEMENT ceux qui ont au moins une vente valide.
-- DONNÉES PERSONNELLES. Le mot de passe n'est pas géré ici : le script de publication crée des
-- comptes sans mot de passe utilisable et ne modifie jamais un compte existant.
with customers as (
    select * from {{ ref('stg_clients') }} where is_valid
),

buyers as (
    select customer_number, count(*) as valid_sales, max(sale_date) as last_sale_date
    from {{ ref('stg_ventes') }}
    where is_valid and customer_number is not null
    group by customer_number
)

select
    c.customer_number,
    c.phone,
    split_part(c.customer_name, ' ', 1)                                       as first_name,
    coalesce(nullif(trim(substr(c.customer_name, length(split_part(c.customer_name, ' ', 1)) + 1)), ''), '') as last_name,
    b.valid_sales,
    b.last_sale_date
from customers c
join buyers b on b.customer_number = c.customer_number
