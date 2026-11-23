{#
  Conversions "sûres" du texte brut (schéma raw) vers des types.
  Une valeur illisible donne NULL au lieu de faire échouer la requête :
  le modèle de staging la signale ensuite dans invalid_reasons.
  Volontairement strictes : "45 000 FCFA" n'est PAS interprété comme 45000, il est signalé.
#}

{# Entier : "12", "-3" ; tout le reste -> NULL #}
{% macro parse_integer(column) -%}
(case when trim({{ column }}) ~ '^-?[0-9]{1,9}$' then trim({{ column }})::integer end)
{%- endmacro %}

{# Montant en FCFA : "45000" ou "45000.5" ; texte, espaces, devise -> NULL #}
{% macro parse_amount(column) -%}
(case when trim({{ column }}) ~ '^-?[0-9]{1,10}(\.[0-9]+)?$' then trim({{ column }})::numeric(12, 2) end)
{%- endmacro %}

{# Date valide à partir de trois expressions entières ; 30/02 ou mois 13 -> NULL (to_date planterait) #}
{% macro make_date_safe(year, month, day) -%}
(case
    when {{ month }} between 1 and 12
     and {{ year }} between 1900 and 2100
     and {{ day }} between 1 and extract(day from (make_date({{ year }}, {{ month }}, 1) + interval '1 month' - interval '1 day'))
    then make_date({{ year }}, {{ month }}, {{ day }})
end)
{%- endmacro %}

{# Date : "JJ/MM/AAAA" (format de la boutique) ou "AAAA-MM-JJ..." (date Excel) ; sinon NULL #}
{% macro parse_date(column) -%}
(case
    when trim({{ column }}) ~ '^[0-9]{1,2}/[0-9]{1,2}/[0-9]{4}$' then {{ make_date_safe(
        "split_part(trim(" ~ column ~ "), '/', 3)::integer",
        "split_part(trim(" ~ column ~ "), '/', 2)::integer",
        "split_part(trim(" ~ column ~ "), '/', 1)::integer") }}
    when trim({{ column }}) ~ '^[0-9]{4}-[0-9]{2}-[0-9]{2}' then {{ make_date_safe(
        "substr(trim(" ~ column ~ "), 1, 4)::integer",
        "substr(trim(" ~ column ~ "), 6, 2)::integer",
        "substr(trim(" ~ column ~ "), 9, 2)::integer") }}
end)
{%- endmacro %}

{# Texte nettoyé : espaces retirés, chaîne vide -> NULL #}
{% macro clean_text(column) -%}
nullif(trim({{ column }}), '')
{%- endmacro %}

{# Liste des problèmes d'une ligne : tableau de messages, les NULL (pas de problème) sont retirés #}
{% macro reasons(checks) -%}
array_remove(array[
    {%- for check in checks %}
    {{ check }}{{ "," if not loop.last }}
    {%- endfor %}
]::text[], null)
{%- endmacro %}

{# Slug d'URL, comme ceux de l'application : minuscules, sans accents, mots séparés par des tirets.
   "SAC-001 Sac cabas cuir Élégance" -> "sac-001-sac-cabas-cuir-elegance" #}
{% macro slugify(expression) -%}
trim(both '-' from regexp_replace(
    translate(lower({{ expression }}),
        'àáâãäåçèéêëìíîïñòóôõöùúûüýÿœæ',
        'aaaaaaceeeeiiiinooooouuuuyyoa'),
    '[^a-z0-9]+', '-', 'g'))
{%- endmacro %}
