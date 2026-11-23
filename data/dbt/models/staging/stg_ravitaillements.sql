-- Ravitaillements fournisseurs, typés. Statuts alignés sur ceux du back-office (PREVU / RECU / ANNULE).
with source as (
    select * from {{ source('raw', 'ravitaillements') }}
),

products as (
    select product_reference from {{ ref('stg_produits') }} where is_valid
),

typed as (
    select
        {{ parse_date('date_prevue') }}                         as expected_date,
        {{ clean_text('fournisseur') }}                         as supplier,
        upper({{ clean_text('reference') }})                    as product_reference,
        {{ parse_integer('quantite') }}                         as quantity,
        {{ parse_amount('prix_d_achat_unitaire_fcfa') }}         as unit_cost,
        case lower({{ clean_text('statut') }})
            when 'prévu' then 'PREVU'
            when 'reçu' then 'RECU'
            when 'annulé' then 'ANNULE'
        end                                                      as status,
        date_prevue                                              as expected_date_raw,
        quantite                                                 as quantity_raw,
        prix_d_achat_unitaire_fcfa                               as unit_cost_raw,
        statut                                                   as status_raw,
        _sheet                                                   as source_sheet,
        _row_number                                              as source_row,
        _source_file                                             as source_file,
        _loaded_at                                               as loaded_at
    from source
),

checked as (
    select
        typed.*,
        {{ reasons([
            "case when expected_date is null then 'date prévue invalide : ' || coalesce(expected_date_raw, '(vide)') end",
            "case when typed.product_reference is null then 'référence manquante' when products.product_reference is null then 'référence produit inconnue : ' || typed.product_reference end",
            "case when quantity is null then 'quantité illisible : ' || coalesce(quantity_raw, '(vide)') when quantity <= 0 then 'quantité nulle ou négative' end",
            "case when unit_cost_raw is not null and unit_cost is null then 'prix d''achat illisible : ' || unit_cost_raw when unit_cost < 0 then 'prix d''achat négatif' end",
            "case when status is null then 'statut inconnu (Prévu, Reçu ou Annulé) : ' || coalesce(status_raw, '(vide)') end",
        ]) }} as reason_list
    from typed
    left join products on products.product_reference = typed.product_reference
)

select
    expected_date,
    supplier,
    product_reference,
    quantity,
    unit_cost,
    quantity * unit_cost                               as total_cost,
    status,
    cardinality(reason_list) = 0                      as is_valid,
    nullif(array_to_string(reason_list, ' ; '), '')   as invalid_reasons,
    source_sheet,
    source_row,
    source_file,
    loaded_at
from checked
