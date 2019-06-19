CREATE TABLE IF NOT EXISTS definitions_relation_meta (meta JSONB);

/* relationships\meta.json */
WITH json_array AS (
    SELECT jsonb_array_elements('[
  {
    "type":"skos:subClassOf",
    "directed": true,
    "to":"is sub class of",
    "from":"has sub class"
  },
  {
    "type":"rdfs:seeAlso",
    "directed": false,
    "to":"see also",
    "from":"see also"
  },
  {
    "type":"skos:member",
    "directed": true,
    "to":"is member of",
    "from":"has member"
  }
]'::jsonb) AS relationship
)
INSERT INTO definitions_relation_meta (meta)
SELECT * FROM json_array;
