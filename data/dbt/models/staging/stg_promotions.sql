-- Promotions. Le prix promotionnel est RECALCULÉ (prix normal x (1 - réduction)) : la formule Excel
-- n'a de valeur que si le fichier a été enregistré par Excel/LibreOffice, on ne s'y fie donc pas.
with source as (
    select * from {{ source('raw', 'promotions') }}
),

products as (
    select product_reference from {{ ref('stg_produits') }} where is_valid
),

typed as (
    select
        upper({{ clean_text('reference') }})          as product_reference,
        {{ parse_amount('prix_normal_fcfa') }}         as regular_price,
        {{ parse_integer('reduction') }}               as discount_percent,
        {{ parse_amount('prix_promotionnel_fcfa') }}   as promo_price_from_file,
        {{ parse_date('date_debut') }}                 as starts_on,
        {{ parse_date('date_fin') }}                   as ends_on,
        case lower({{ clean_text('statut') }})
            when 'active' then 'ACTIVE'
            when 'à venir' then 'A_VENIR'
            when 'terminée' then 'TERMINEE'
        end                                            as status,
        prix_normal_fcfa                               as regular_price_raw,
        reduction                                      as discount_raw,
        date_debut                                     as starts_on_raw,
        date_fin                                       as ends_on_raw,
        statut                                         as status_raw,
        _sheet                                         as source_sheet,
        _row_number                                    as source_row,
        _source_file                                   as source_file,
        _loaded_at                                     as loaded_at
    from source
),

checked as (
    select
        typed.*,
        {{ reasons([
            "case when upper(coalesce(typed.product_reference, '')) = 'À COMPLÉTER' then 'ligne modèle « À compléter » (à remplir ou supprimer)' when typed.product_reference is null then 'référence manquante' when products.product_reference is null then 'référence produit inconnue : ' || typed.product_reference end",
            "case when regular_price is null then 'prix normal illisible : ' || coalesce(regular_price_raw, '(vide)') when regular_price <= 0 then 'prix normal nul ou négatif' end",
            "case when discount_percent is null then 'réduction illisible : ' || coalesce(discount_raw, '(vide)') when discount_percent not between 1 and 100 then 'réduction hors de 1 à 100 % : ' || discount_percent end",
            "case when starts_on is null then 'date de début invalide : ' || coalesce(starts_on_raw, '(vide)') end",
            "case when ends_on is null then 'date de fin invalide : ' || coalesce(ends_on_raw, '(vide)') when starts_on > ends_on then 'date de fin avant la date de début' end",
            "case when status is null then 'statut inconnu (Active, À venir, Terminée) : ' || coalesce(status_raw, '(vide)') end",
        ]) }} as reason_list
    from typed
    left join products on products.product_reference = typed.product_reference
)

select
    product_reference,
    regular_price,
    discount_percent,
    round(regular_price * (1 - discount_percent / 100.0), 2) as promo_price,
    promo_price_from_file,
    starts_on,
    ends_on,
    status,
    cardinality(reason_list) = 0                      as is_valid,
    nullif(array_to_string(reason_list, ' ; '), '')   as invalid_reasons,
    source_sheet,
    source_row,
    source_file,
    loaded_at
from checked
