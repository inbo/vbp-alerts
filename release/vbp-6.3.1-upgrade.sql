-- VBP one-time data migration for the upgrade from the 4.4.0-SNAPSHOT fork to 6.3.1.
-- Run once, BEFORE the first start of 6.3.1, on the alerts schema.
--
-- Our fork generated taxon/region/species-group queries with 'sort=+first_loaded_date'
-- (and 'sort=+last_assertion_date'). Upstream (#403) generates 'sort=first_loaded_date'.
-- QueryService looks up existing queries by exact base_url + query_path, so without this
-- update every new subscription would create a duplicate query instead of reusing the
-- existing one.

-- Preview the affected rows
SELECT id, name, query_path, query_path_forui
FROM query
WHERE query_path LIKE '%&sort=+%' OR query_path_forui LIKE '%&sort=+%';

-- Check that no row already exists in the new form (would become a duplicate)
SELECT o.id AS old_id, n.id AS new_id, o.name
FROM query o
JOIN query n
  ON n.base_url = o.base_url
 AND n.query_path = REPLACE(o.query_path, '&sort=+', '&sort=')
WHERE o.query_path LIKE '%&sort=+%';

UPDATE query
SET query_path       = REPLACE(query_path, '&sort=+', '&sort='),
    query_path_forui = REPLACE(query_path_forui, '&sort=+', '&sort=')
WHERE query_path LIKE '%&sort=+%' OR query_path_forui LIKE '%&sort=+%';
