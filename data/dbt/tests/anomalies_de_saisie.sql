-- AVERTISSEMENT (ne bloque pas dbt build) : signale les lignes du fichier Excel à corriger.
-- Le détail est dans la vue staging.stg_anomalies.
{{ config(severity='warn') }}

select * from {{ ref('stg_anomalies') }}
