{# Échoue pour chaque ligne dont la valeur est inférieure au minimum (ex. prix > 0, stock >= 0). #}
{% test value_at_least(model, column_name, min_value) %}
select *
from {{ model }}
where {{ column_name }} < {{ min_value }}
{% endtest %}
