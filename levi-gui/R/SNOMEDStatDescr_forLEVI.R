# R Script to count all descriptions in DE, FR and IT and plot as bar chart

# Import Libraries
library(DBI)
library(RMariaDB)
library(dplyr)
library(ggplot2)
library(scales)

args <- commandArgs(trailingOnly = TRUE)
if (length(args) < 6) {
  stop("Usage: Rscript SNOMEDStatDescr_forLEVI.R <host> <port> <user> <password> <dbname> <outputDir>")
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

# SQL-Query for all existing translations, without taking into account that concepts have been deactivated
sql_allDescriptions <- "
SELECT
  d.languageCode AS language,
  COUNT(*) AS count_descriptions
FROM full_description d
WHERE d.active = 1
  AND d.languageCode IN ('de', 'fr', 'it')
  AND EXISTS (
    SELECT 1 FROM full_concept c
    WHERE c.id = d.conceptId
      AND c.active = 1
  )
GROUP BY d.languageCode
ORDER BY count_descriptions DESC
"

# SQL-Quey for translations of only active SNOMED concepts
sql_onlyActiveConcepts <- "
SELECT
    fd.languageCode AS language,
    COUNT(*) AS count_descriptions
FROM full_concept fc
INNER JOIN (
    SELECT id, MAX(effectiveTime) AS max_time
    FROM full_concept
    GROUP BY id
) latest_c
    ON fc.id = latest_c.id
   AND fc.effectiveTime = latest_c.max_time
INNER JOIN full_description fd
    ON fc.id = fd.conceptId
WHERE fc.active = 1
  AND fd.active = 1
  AND fd.languageCode IN ('de','fr','it')
GROUP BY fd.languageCode
ORDER BY count_descriptions DESC;
"

# Execute SQL-Query
df_allDescriptions <- dbGetQuery(con, sql_allDescriptions)
# Set all languages to upper-case e.g. "DE"
df_allDescriptions$language <- toupper(df_allDescriptions$language)
# Set count descriptions as numeric
df_allDescriptions$count_descriptions  <- as.numeric(df_allDescriptions$count_descriptions)

df_onlyActiveConcepts <- dbGetQuery(con, sql_onlyActiveConcepts)
df_onlyActiveConcepts$language <- toupper(df_onlyActiveConcepts$language)
df_onlyActiveConcepts$count_descriptions  <- as.numeric(df_onlyActiveConcepts$count_descriptions)

# Set languages as factor
df_allDescriptions <- df_allDescriptions %>%
  mutate(language = factor(language, levels = c("DE", "FR", "IT")))

df_onlyActiveConcepts <- df_onlyActiveConcepts %>%
  mutate(language = factor(language, levels = c("DE", "FR", "IT")))

# Color palette for languages
palette_language <- c(
  "DE" = "#257394",
  "FR" = "#257394",
  "IT" = "#257394"
)

# Plot
# TODO: Change titel and caption
plot_allDescriptions <- ggplot(df_allDescriptions, aes(x = language, y = count_descriptions, fill = language)) +
  geom_col(width = 0.65) +
  geom_text(
    aes(label = scales::label_number(big.mark = "'")(count_descriptions)),
    vjust = -0.5,
    fontface = "bold"
  ) +
  scale_fill_manual(values = palette_language) +
  scale_y_continuous(expand = expansion(mult = c(0, 0.1))) +
  labs(
    title = "Anzahl aktive Übersetzungen pro Sprache",
    caption = "Es werden auch aktive Übersetzungen von inaktiven SNOMED-Konzepten gezählt. | Stand Juni 2026"
  ) +
  theme_minimal() +
  theme(
    plot.title = element_text(hjust = 0.5, face = "bold"),
    legend.position = "none",
    axis.title = element_blank(),
    axis.text.y = element_blank(),
    axis.ticks.y = element_blank(),
    panel.grid = element_blank(),
    panel.background = element_rect(fill = "grey95", color = NA),
    plot.background = element_rect(fill = "white", color = NA),
    plot.margin = margin(10, 15, 10, 15)
  )


# Plot
# TODO: Change titel and caption
plot_onlyActiveConcepts <- ggplot(df_onlyActiveConcepts, aes(x = language, y = count_descriptions, fill = language)) +
  geom_col(width = 0.65) +
  geom_text(
    aes(label = scales::label_number(big.mark = "'")(count_descriptions)),
    vjust = -0.5,
    fontface = "bold"
  ) +
  scale_fill_manual(values = palette_language) +
  scale_y_continuous(expand = expansion(mult = c(0, 0.1))) +
  labs(
    title = "Anzahl aktive Übersetzungen pro Sprache",
    caption = "Es werden nur aktive Übersetzungen und aktive SNOMED-CT-Konzepte berücksichtigt. | Stand Juni 2026"
  ) +
  theme_minimal() +
  theme(
    plot.title = element_text(hjust = 0.5, face = "bold"),
    legend.position = "none",
    axis.title = element_blank(),
    axis.text.y = element_blank(),
    axis.ticks.y = element_blank(),
    panel.grid = element_blank(),
    panel.background = element_rect(fill = "grey95", color = NA),
    plot.background = element_rect(fill = "white", color = NA),
    plot.margin = margin(10, 15, 10, 15)
  )

# Save plots as png
ggsave(file.path(output_dir, "descriptions_pro_sprache_all.png"), plot_allDescriptions, width = 8, height = 5, dpi = 150)
ggsave(file.path(output_dir, "descriptions_pro_sprache_onlyActive.png"), plot_onlyActiveConcepts, width = 8, height = 5, dpi = 150)

# Disconnect DB
#dbDisconnect(con)
