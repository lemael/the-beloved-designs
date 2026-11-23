-- Produits de la boutique, typés. Toutes les lignes sont gardées ; is_valid / invalid_reasons signalent les erreurs.
with source as (
    select * from {{ source('raw', 'produits') }}
),

typed as (
    select
        upper({{ clean_text('reference') }})                    as product_reference,
        {{ clean_text('nom_du_produit') }}                      as product_name,
        {{ clean_text('categorie') }}                           as category_name,
        case lower({{ clean_text('categorie') }})
            when 'femmes' then 'femmes'
            when 'hommes' then 'hommes'
            when 'chaussures' then 'chaussures'
            when 'sacs à main' then 'sacs-a-main'
            when 'cosmétiques' then 'cosmetiques'
        end                                                      as category_slug,  -- mêmes slugs que la boutique
        {{ parse_amount('prix_de_vente_fcfa') }}                 as sale_price,
        {{ parse_amount('prix_d_achat_fcfa') }}                  as purchase_price,
        {{ clean_text('taille') }}                               as size,
        {{ clean_text('couleur') }}                              as color,
        case lower({{ clean_text('actif') }})
            when 'oui' then true
            when 'non' then false
        end                                                      as is_active,
        prix_de_vente_fcfa                                       as sale_price_raw,
        prix_d_achat_fcfa                                        as purchase_price_raw,
        actif                                                    as active_raw,
        _sheet                                                   as source_sheet,
        _row_number                                              as source_row,
        _source_file                                             as source_file,
        _loaded_at                                               as loaded_at,
        -- 1 pour la première apparition d'une référence, 2+ pour les doublons plus bas dans la feuille
        row_number() over (partition by upper({{ clean_text('reference') }}) order by _row_number) as occurrence
    from source
),

checked as (
    select
        *,
        {{ reasons([
            "case when product_reference is null then 'référence manquante' end",
            "case when product_reference is not null and occurrence > 1 then 'référence en double (déjà présente plus haut)' end",
            "case when product_name is null then 'nom du produit manquant' end",
            "case when category_slug is null then 'catégorie inconnue : ' || coalesce(category_name, '(vide)') end",
            "case when sale_price is null then 'prix de vente illisible : ' || coalesce(sale_price_raw, '(vide)') when sale_price <= 0 then 'prix de vente nul ou négatif' end",
            "case when purchase_price_raw is not null and purchase_price is null then 'prix d''achat illisible : ' || purchase_price_raw when purchase_price < 0 then 'prix d''achat négatif' end",
            "case when is_active is null then 'colonne Actif à remplir avec Oui ou Non : ' || coalesce(active_raw, '(vide)') end",
        ]) }} as reason_list
    from typed
)

select
    product_reference,
    product_name,
    category_name,
    category_slug,
    sale_price,
    purchase_price,
    size,
    color,
    is_active,
    cardinality(reason_list) = 0                      as is_valid,
    nullif(array_to_string(reason_list, ' ; '), '')   as invalid_reasons,
    source_sheet,
    source_row,
    source_file,
    loaded_at
from checked
