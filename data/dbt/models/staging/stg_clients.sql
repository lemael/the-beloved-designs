-- Clients (DONNÉES PERSONNELLES). Téléphone validé avec la même règle que l'application (format international).
with source as (
    select * from {{ source('raw', 'clients') }}
),

typed as (
    select
        upper({{ clean_text('n_client') }})                      as customer_number,
        {{ clean_text('nom_du_client') }}                        as customer_name,
        regexp_replace({{ clean_text('numero_de_telephone') }}, '\s', '', 'g') as phone,
        {{ clean_text('quartier') }}                             as district,
        initcap({{ clean_text('ville') }})                       as city,
        {{ clean_text('notes') }}                                as notes,
        numero_de_telephone                                      as phone_raw,
        _sheet                                                   as source_sheet,
        _row_number                                              as source_row,
        _source_file                                             as source_file,
        _loaded_at                                               as loaded_at,
        row_number() over (partition by upper({{ clean_text('n_client') }}) order by _row_number) as number_occurrence,
        row_number() over (partition by regexp_replace({{ clean_text('numero_de_telephone') }}, '\s', '', 'g') order by _row_number) as phone_occurrence
    from source
),

checked as (
    select
        *,
        {{ reasons([
            "case when customer_number = 'À COMPLÉTER' then 'ligne modèle « À compléter » (à remplir ou supprimer)' when customer_number is null then 'numéro client manquant' when number_occurrence > 1 then 'numéro client en double' end",
            "case when customer_number <> 'À COMPLÉTER' and customer_name is null then 'nom du client manquant' end",
            "case when customer_number <> 'À COMPLÉTER' and (phone is null or phone !~ '^\\+[1-9][0-9]{6,14}$') then 'téléphone invalide (format international, ex. +237612345678) : ' || coalesce(phone_raw, '(vide)') when phone is not null and phone_occurrence > 1 then 'téléphone déjà utilisé par un autre client' end",
        ]) }} as reason_list
    from typed
)

select
    customer_number,
    customer_name,
    phone,
    district,
    city,
    notes,
    cardinality(reason_list) = 0                      as is_valid,
    nullif(array_to_string(reason_list, ' ; '), '')   as invalid_reasons,
    source_sheet,
    source_row,
    source_file,
    loaded_at
from checked
