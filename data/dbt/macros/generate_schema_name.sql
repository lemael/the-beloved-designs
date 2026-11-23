{#
  Par défaut, dbt colle le schéma du profil et celui du modèle : un modèle "+schema: marts"
  irait dans "staging_marts". On veut exactement les schémas décidés (raw / staging / marts) :
  le schéma configuré sur le modèle est utilisé tel quel, sinon celui du profil (staging).
#}
{% macro generate_schema_name(custom_schema_name, node) -%}
    {%- if custom_schema_name is none -%}
        {{ target.schema | trim }}
    {%- else -%}
        {{ custom_schema_name | trim }}
    {%- endif -%}
{%- endmacro %}
