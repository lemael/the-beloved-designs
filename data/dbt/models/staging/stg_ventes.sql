-- Ventes, typées. Une vente valide a une date valide, un produit connu, une quantité et un prix positifs.
with source as (
    select * from {{ source('raw', 'ventes') }}
),

products as (
    select product_reference from {{ ref('stg_produits') }} where is_valid
),

customers as (
    select customer_number from {{ ref('stg_clients') }} where is_valid
),

typed as (
    select
        {{ clean_text('n_vente') }}                    as sale_number,
        {{ parse_date('"date"') }}                     as sale_date,
        upper({{ clean_text('reference') }})           as product_reference,
        {{ parse_integer('quantite') }}                as quantity,
        {{ parse_amount('prix_unitaire_fcfa') }}       as unit_price,
        {{ clean_text('mode_de_paiement') }}           as payment_method,
        initcap({{ clean_text('ville') }})             as city,  -- "  douala " -> "Douala"
        upper({{ clean_text('n_client') }})            as customer_number,  -- vide = vente au comptoir
        "date"                                         as sale_date_raw,
        quantite                                       as quantity_raw,
        prix_unitaire_fcfa                             as unit_price_raw,
        _sheet                                         as source_sheet,
        _row_number                                    as source_row,
        _source_file                                   as source_file,
        _loaded_at                                     as loaded_at,
        row_number() over (partition by {{ clean_text('n_vente') }} order by _row_number) as occurrence
    from source
),

checked as (
    select
        typed.*,
        {{ reasons([
            "case when sale_number is null then 'numéro de vente manquant' when occurrence > 1 then 'vente en double (même numéro plus haut)' end",
            "case when sale_date is null then 'date invalide : ' || coalesce(sale_date_raw, '(vide)') end",
            "case when typed.product_reference is null then 'référence manquante' when products.product_reference is null then 'référence produit inconnue : ' || typed.product_reference end",
            "case when quantity is null then 'quantité illisible : ' || coalesce(quantity_raw, '(vide)') when quantity <= 0 then 'quantité nulle ou négative : ' || quantity end",
            "case when unit_price is null then 'prix illisible : ' || coalesce(unit_price_raw, '(vide)') when unit_price <= 0 then 'prix nul ou négatif' end",
            "case when payment_method is null then 'mode de paiement manquant' when payment_method not in ('Orange Money', 'MTN MoMo', 'Espèces') then 'mode de paiement inconnu : ' || payment_method end",
            "case when typed.customer_number is not null and customers.customer_number is null then 'client inconnu (absent ou invalide dans la feuille Clients) : ' || typed.customer_number end",
        ]) }} as reason_list
    from typed
    left join products on products.product_reference = typed.product_reference
    left join customers on customers.customer_number = typed.customer_number
)

select
    sale_number,
    sale_date,
    product_reference,
    quantity,
    unit_price,
    quantity * unit_price                              as line_amount,
    payment_method,
    city,
    customer_number,
    cardinality(reason_list) = 0                      as is_valid,
    nullif(array_to_string(reason_list, ' ; '), '')   as invalid_reasons,
    source_sheet,
    source_row,
    source_file,
    loaded_at
from checked
