-- Toutes les lignes invalides du fichier, avec leur feuille et leur ligne Excel :
-- c'est la liste à renvoyer à la boutique pour correction. Vide = fichier propre.
{% set models = [
    ('stg_produits', 'product_reference'),
    ('stg_stock', 'product_reference'),
    ('stg_ventes', 'sale_number'),
    ('stg_ravitaillements', 'product_reference'),
    ('stg_clients', 'customer_number'),
    ('stg_promotions', 'product_reference'),
] %}

{% for model, identifier in models %}
select
    source_sheet                      as sheet,
    source_row                        as excel_row,
    {{ identifier }}::text            as identifier,
    invalid_reasons                   as problems,
    source_file,
    loaded_at
from {{ ref(model) }}
where not is_valid
{% if not loop.last %}union all{% endif %}
{% endfor %}
