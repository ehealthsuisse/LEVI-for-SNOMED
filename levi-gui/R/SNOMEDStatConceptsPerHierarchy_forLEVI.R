# R Script to count all translated concepts per hierarchy in DE, FR and IT and plot

# Import Libraries
library(DBI)
library(RMariaDB)
library(dplyr)
library(ggplot2)
library(scales)
library(tidyr)

args <- commandArgs(trailingOnly = TRUE)
if (length(args) < 6) {
  stop("Usage: Rscript SNOMEDStatConceptsPerHierarchy_forLEVI.R <host> <port> <user> <password> <dbname> <outputDir>")
}
db_host    <- args[1]
db_port    <- as.integer(args[2])
db_user    <- args[3]
db_password<- args[4]
db_name    <- args[5]
output_dir <- args[6]

dir.create(output_dir, showWarnings = FALSE, recursive = TRUE)

# Connect to Database
con <- dbConnect(
  RMariaDB::MariaDB(),
  host = db_host,
  port = db_port,
  user = db_user,
  password = db_password,
  dbname = db_name
)

# SQL-Query to count the translated concepts per hierarchy (only active concepts)
sql_concepts_hierarchy <- sql_concepts_hierarchy <- "
WITH latest_concept AS (
    SELECT 
        id,
        active,
        ROW_NUMBER() OVER (
            PARTITION BY id 
            ORDER BY effectiveTime DESC
        ) AS rn
    FROM full_concept
),
latest_fsn AS (
    SELECT 
        conceptId,
        term,
        ROW_NUMBER() OVER (
            PARTITION BY conceptId 
            ORDER BY effectiveTime DESC, id DESC
        ) AS rn
    FROM full_description
    WHERE typeId = 900000000000003001
      AND languageCode = 'en'
      AND term LIKE '%(%)'
),
active_descriptions AS (
    SELECT
        conceptId,
        languageCode
    FROM full_description
    WHERE active = 1
      AND languageCode IN ('de','fr','it')
)

SELECT
    d.languageCode AS Language,
    SUBSTRING_INDEX(SUBSTRING_INDEX(f.term, '(', -1), ')', 1) AS Semantic_Tag,
    COUNT(DISTINCT c.id) AS Count_Concepts
FROM latest_concept c
JOIN active_descriptions d
    ON c.id = d.conceptId
JOIN latest_fsn f
    ON f.conceptId = c.id
   AND f.rn = 1
WHERE c.rn = 1
  AND c.active = 1
GROUP BY
    d.languageCode,
    Semantic_Tag
ORDER BY
    Language,
    Count_Concepts DESC;
"

# Execute query
df_concepts_hierarchy <- dbGetQuery(con, sql_concepts_hierarchy)

# Clean the data
df_concepts_hierarchy$Language <- toupper(df_concepts_hierarchy$Language)
df_concepts_hierarchy$Count_Concepts  <- as.numeric(df_concepts_hierarchy$Count_Concepts)

langs <- c("DE", "FR", "IT")

df_complete <- df_concepts_hierarchy %>%
  complete(
    Language = Language,
    Semantic_Tag,
    fill = list(Count_Concepts = 0)
  )

# Create a map for all the semantic tags
map_semantictag <- tibble::tribble(
  ~Semantic_Tag, ~hierarchie,
  
  "body structure", "Body structure (body structure)",
  "cell", "Body structure (body structure)",
  "cell structure", "Body structure (body structure)",
  "morphologic abnormality", "Body structure (body structure)",
  
  "finding", "Clinical finding (finding)",
  "disorder", "Clinical finding (finding)",
  
  "environment / location", "Environment or geographical location (environment / location)",
  "environment", "Environment or geographical location (environment / location)",
  "geographic location", "Environment or geographical location (environment / location)",
  
  "event", "Event (event)",
  
  "observable entity", "Observable entity (observable entity)",
  
  "organism", "Organism (organism)",
  
  "clinical drug", "Pharmaceutical / biologic product (product)",
  "medicinal product", "Pharmaceutical / biologic product (product)",
  "medicinal product form", "Pharmaceutical / biologic product (product)",
  "physical object", "Physical object (physical object)",
  "product", "Pharmaceutical / biologic product (product)",
  
  "physical force", "Physical force (physical force)",
  
  "procedure", "Procedure (procedure)",
  "regime/therapy", "Procedure (procedure)",
  
  "qualifier value", "Qualifier value (qualifier value)",
  "administration method", "Qualifier value (qualifier value)",
  "basic dose form", "Qualifier value (qualifier value)",
  "disposition", "Qualifier value (qualifier value)",
  "dose form", "Qualifier value (qualifier value)",
  "intended site", "Qualifier value (qualifier value)",
  "number", "Qualifier value (qualifier value)",
  "product name", "Qualifier value (qualifier value)",
  "release characteristic", "Qualifier value (qualifier value)",
  "role", "Qualifier value (qualifier value)",
  "state of matter", "Qualifier value (qualifier value)",
  "transformation", "Qualifier value (qualifier value)",
  "supplier", "Qualifier value (qualifier value)",
  "unit of presentation", "Qualifier value (qualifier value)",
  
  "record artifact", "Record artifact (record artifact)",
  
  "situation", "Situation with explicit context (situation)",
  
  "SNOMED RT+CTV3", "SNOMED CT Concept (SNOMED RT+CTV3)",
  
  "metadata", "SNOMED CT Model Component (metadata)",
  "attribute", "SNOMED CT Model Component (metadata)",
  "core metadata concept", "SNOMED CT Model Component (metadata)",
  "foundation metadata concept", "SNOMED CT Model Component (metadata)",
  "link assertion", "SNOMED CT Model Component (metadata)",
  "linkage concept", "SNOMED CT Model Component (metadata)",
  "namespace concept", "SNOMED CT Model Component (metadata)",
  "OWL metadata concept", "SNOMED CT Model Component (metadata)",
  
  "social concept", "Social context (social concept)",
  "ethnic group", "Social context (social concept)",
  "life style", "Social context (social concept)",
  "occupation", "Social context (social concept)",
  "person", "Social context (social concept)",
  "racial group", "Social context (social concept)",
  "religion/philosophy", "Social context (social concept)",
  
  "special concept", "Special concept (special concept)",
  "inactive concept", "Special concept (special concept)",
  "navigational concept", "Special concept (special concept)",
  
  "specimen", "Specimen (specimen)",
  
  "staging scale", "Staging and scales (staging scale)",
  "assessment scale", "Staging and scales (staging scale)",
  "tumor staging", "Staging and scales (staging scale)",
  
  "substance", "Substance (substance)"
)

# Map the data with the semantic tags
df_mapped <- df_complete %>%
  left_join(map_semantictag, by = "Semantic_Tag")

# Count the concepts per hierarchy
df_final <- df_mapped %>%
  group_by(Language, hierarchie) %>%
  summarise(Count_Concepts = sum(Count_Concepts), .groups = "drop")


# Referenztabelle: Anzahl aktiver SNOMED CT Konzepte pro Hierarchie (Basis = gesamtes Release)
df_total_hierarchie <- tibble::tribble(
  ~hierarchie, ~total_konzepte,
  "Body structure (body structure)", 44170,
  "Clinical finding (finding)", 128991,
  "Environment or geographical location (environment / location)", 1876,
  "Event (event)", 3313,
  "Observable entity (observable entity)", 11050,
  "Organism (organism)", 34761,
  "Pharmaceutical / biologic product (product)", 26029,
  "Physical force (physical force)", 172,
  "Physical object (physical object)", 14112,
  "Procedure (procedure)", 60362,
  "Qualifier value (qualifier value)", 12486,
  "Record artifact (record artifact)", 522,
  "SNOMED CT Concept (SNOMED RT+CTV3)", 1,
  "SNOMED CT Model Component (metadata)", 1916,
  "Situation with explicit context (situation)", 5124,
  "Social context (social concept)", 4180,
  "Special concept (special concept)", 2,
  "Specimen (specimen)", 1844,
  "Staging and scales (staging scale)", 1667,
  "Substance (substance)", 29278
)

# Join mit den übersetzten Konzepten und Prozentsatz berechnen
df_final_pct <- df_final %>%
  left_join(df_total_hierarchie, by = "hierarchie") %>%
  mutate(
    anteil_prozent = round(100 * Count_Concepts / total_konzepte, 1)
  ) %>%
  filter(!is.na(hierarchie), !is.na(anteil_prozent))

# Plot 2: Heatmap mit prozentualer Übersetzungsabdeckung pro Hierarchie und Sprache
plot_hierarchie_percentage <- ggplot(df_final_pct, aes(
  x = Language,
  y = hierarchie,
  fill = anteil_prozent
)) +
  geom_tile(color = "white") +
  geom_text(aes(label = paste0(anteil_prozent, "%")), size = 3, fontface = "bold") +
  scale_fill_gradientn(
    colors = c(
      "#f4fbf6",
      "#e8f5ea",
      "#d7eddc",
      "#c3e3cc",
      "#abd8b8",
      "#90cba2",
      "#73bc8b",
      "#5ca774",
      "#4a8f61"
    ),
    limits = c(0, 100),
    labels = function(x) paste0(x, "%")
  ) +
  labs(
    title = "Übersetzungsabdeckung pro Hierarchie (in %)",
    x = "Sprache",
    y = "Semantic Tag / Hierarchie",
    fill = "Anteil (%)"
  ) +
  theme_minimal() +
  theme(
    axis.title = element_blank()
    #axis.text.y = element_blank(),
  )


# Save plot as png
ggsave(file.path(output_dir, "translations_pro_hierarchy.png"), plot_hierarchie_percentage, width = 8, height = 5, dpi = 150)

#dbDisconnect(con)
