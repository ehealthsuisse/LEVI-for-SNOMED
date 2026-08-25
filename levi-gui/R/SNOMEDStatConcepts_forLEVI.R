# R Script to count all translated concepts in DE, FR and IT and plot as bar chart

# Import Libraries
library(DBI)
library(RMariaDB)
library(dplyr)
library(ggplot2)
library(scales)

args <- commandArgs(trailingOnly = TRUE)
if (length(args) < 6) {
  stop("Usage: Rscript SNOMEDStatConcepts.R <host> <port> <user> <password> <dbname> <outputDir>")
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

# SQL-Query for all translated concepts, including inactivated concepts
sql_allConcepts <- "
SELECT
d.languageCode AS language,
COUNT(DISTINCT d.conceptId) AS count_concepts
FROM full_description d
WHERE d.active = 1
AND d.languageCode IN ('de', 'fr', 'it')
AND EXISTS (
  SELECT 1 FROM full_concept c
  WHERE c.id = d.conceptId
  AND c.active = 1
)
GROUP BY d.languageCode
ORDER BY count_concepts DESC
"

# SQL-Query for only active concepts
sql_onlyActiveConcepts <- "
SELECT
    fd.languageCode AS language,
    COUNT(DISTINCT fc.id) AS count_concepts
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
ORDER BY count_concepts DESC;
"

# Execute SQL-Queries and clean data
df_allConcepts<- dbGetQuery(con, sql_allConcepts)
df_allConcepts$language <- toupper(df_allConcepts$language)
df_allConcepts$count_concepts  <- as.numeric(df_allConcepts$count_concepts)

df_onlyActiveConcepts<- dbGetQuery(con, sql_onlyActiveConcepts)

# Create scale lables
df_allConcepts <- df_allConcepts %>%
  mutate(
    language = toupper(language),
    count_concepts = as.numeric(count_concepts),
    label = scales::label_number(big.mark = "'")(count_concepts)
  )

df_onlyActiveConcepts <- df_onlyActiveConcepts %>%
  mutate(
    language = toupper(language),
    count_concepts = as.numeric(count_concepts),
    label = scales::label_number(big.mark = "'")(count_concepts)
  )

# Set languages as factor
df_allConcepts<- df_allConcepts %>%
mutate(language = factor(language, levels = c("DE", "FR", "IT")))

df_onlyActiveConcepts<- df_onlyActiveConcepts %>%
  mutate(language = factor(language, levels = c("DE", "FR", "IT")))

# Color palette for languages
palette_language <- c(
  "DE" = "#259462",
  "FR" = "#259462",
  "IT" = "#259462"
)

# Plot
# TODO: Change titel and caption
plot_allConcepts <- ggplot(df_allConcepts, aes(x = language, y = count_concepts, fill = language)) +
  geom_col(width = 0.65) +
  geom_text(aes(label = label), vjust = -0.5, fontface = "bold") +
  scale_fill_manual(values = palette_language) +
  scale_y_continuous(expand = expansion(mult = c(0, 0.1))) +
  labs(
    title = "Anzahl übersetzte Konzepte pro Sprache",
    caption = "Es werden auch inaktive Konzepte gezählt, sofern diese eine aktive Übersetzung haben | Stand Juni 2026"
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
plot_onlyActiveConcepts <- ggplot(df_onlyActiveConcepts, aes(x = language, y = count_concepts, fill = language)) +
  geom_col(width = 0.65) +
  geom_text(aes(label = label), vjust = -0.5, fontface = "bold") +
  scale_fill_manual(values = palette_language) +
  scale_y_continuous(expand = expansion(mult = c(0, 0.1))) +
  labs(
    title = "Anzahl übersetzte Konzepte pro Sprache",
    caption = "Es werden nur aktive übersetzte Konzepte gezählt | Stand Juni 2026"
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

total_concepts <- 381856

df_concepts_forPercentage <- df_onlyActiveConcepts %>%
  mutate(
    language = toupper(language),
    count_concepts = as.numeric(count_concepts),
    percent = count_concepts / total_concepts * 100,
    label = paste0(
      scales::label_number(big.mark = "'")(count_concepts),
      "\n(",
      round(percent, 1),
      "%)"
    )
  )

plot_concepts_percentage <- ggplot(df_concepts_forPercentage, aes(x = language, y = count_concepts, fill = language)) +
  # Hintergrundbalken bis 100%
  geom_col(aes(y = 381856), width = 0.65, fill = "grey85") +
  # eigentlicher Balken
  geom_col(width = 0.65) +
  geom_text(aes(label = label), vjust = -0.5, fontface = "bold") +
  scale_fill_manual(values = palette_language) +
  scale_y_continuous(expand = expansion(mult = c(0, 0.15))) +
  labs(
    title = "Anzahl übersetzte Konzepte pro Sprache",
    caption = "Basis: aktive SNOMED CT Konzepte (n = 381'856) | Stand Dezember 2025"
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

# Save plot as png
ggsave(file.path(output_dir, "concepts_all.png"), plot_allConcepts, width = 8, height = 5, dpi = 150)
ggsave(file.path(output_dir, "concepts_onlyActive.png"), plot_onlyActiveConcepts, width = 8, height = 5, dpi = 150)
ggsave(file.path(output_dir, "concepts_percentage.png"), plot_concepts_percentage, width = 8, height = 5, dpi = 150)

#dbDisconnect(con)
