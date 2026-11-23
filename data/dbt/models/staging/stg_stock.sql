-- Inventaire par référence, typé. La référence doit exister parmi les produits valides.
with source as (
    select * from {{ source('raw', 'stock') }}
),

products as (
    select product_reference from {{ ref('stg_produits') }} where is_valid
),

typed as (
    select
        upper({{ clean_text('reference') }})          as product_reference,
        {{ parse_integer('quantite_en_stock') }}       as stock_quantity,
        {{ parse_date('date_inventaire') }}            as inventory_date,
        quantite_en_stock                              as stock_quantity_raw,
        date_inventaire                                as inventory_date_raw,
        _sheet                                         as source_sheet,
        _row_number                                    as source_row,
        _source_file                                   as source_file,
        _loaded_at                                     as loaded_at,
        row_number() over (partition by upper({{ clean_text('reference') }}) order by _row_number) as occurrence
    from source
),

checked as (
    select
        typed.*,
        {{ reasons([
            "case when typed.product_reference is null then 'référence manquante' when products.product_reference is null then 'référence produit inconnue : ' || typed.product_reference end",
            "case when typed.product_reference is not null and occurrence > 1 then 'référence en double dans l''inventaire' end",
            "case when stock_quantity is null then 'quantité illisible : ' || coalesce(stock_quantity_raw, '(vide)') when stock_quantity < 0 then 'stock négatif : ' || stock_quantity end",
            "case when inventory_date is null then 'date d''inventaire invalide : ' || coalesce(inventory_date_raw, '(vide)') end",
        ]) }} as reason_list
    from typed
    left join products on products.product_reference = typed.product_reference
)

select
    product_reference,
    stock_quantity,
    inventory_date,
    cardinality(reason_list) = 0                      as is_valid,
    nullif(array_to_string(reason_list, ' ; '), '')   as invalid_reasons,
    source_sheet,
    source_row,
    source_file,
    loaded_at
from checked
