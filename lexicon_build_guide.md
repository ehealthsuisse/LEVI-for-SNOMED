# LEVI Lexikon-Build Leitfaden

Dieser Leitfaden richtet sich an KI-Assistenten und erklärt Schritt für Schritt, wie die deutsch- und italienischsprachigen Lexika aufgebaut, ausgeliefert und im System eingesetzt werden. Alle Informationen basieren auf den Klassen `DeLexiconBuilder`, `ItLexiconBuilder`, `DeTokenizer`, `ItTokenizer` und den Verbraucher-Komponenten wie `HunspellSpellingChecker` und `ItalianTranslationRuleChecker`.

## Gesamtüberblick
- Quelle: aktive SNOMED CT-Beschreibungen aus den Tabellen `full_description` und `full_refset_Language`.
- Verarbeitung: sprachspezifische Tokenizer normalisieren, zerlegen und klassifizieren jedes Token.
- Statistik: Häufigkeiten, Groß-/Kleinschreibung, Beispiele und Kategorien werden ermittelt.
- Ausgabe: strukturierte Dateien unter `dictionary_<lang>/` (Allowlist, Kategorien, Typos, Verdachtsliste, Statistik).
- Einsatz: `HunspellSpellingChecker` liest die generierten Dateien (über Ressourcen oder über `levi.spelling.lexiconDir`) und dient Sprachecheckern wie `ItalianTranslationRuleChecker` als Ebene-1-Lexikon.

## Voraussetzungen
- Zugriff auf eine aktuelle SNOMED CT-Datenbank (Schema mit `full_description` und `full_refset_Language`).
- Java-Laufzeit mit Zugang zum `levi-core`-Artefakt.
- DB-Zugangsdaten sowie Sprache (`de` oder `it`) und zugehörige Refset-ID (z. B. `900000000000509007` für Deutsch, `900000000000510002` für Italienisch – anpassbar je nach Deployment).
- Schreibrechte auf ein Zielverzeichnis, in dem `dictionary_<lang>/` angelegt werden darf.

## CLI-Aufruf
Sowohl Deutsch als auch Italienisch verwenden das gleiche Interface:
```
java -cp levi-core/target/classes \
  ch.ehealth.levi.core.check.<De|It>LexiconBuilder \
  jdbc:mysql://host/db schema_user schema_pass \
  <languageCode> <refsetId> /path/zu/lexicon-output
```
Beispiel Deutsch:
```
java -cp levi-core/target/classes \
  ch.ehealth.levi.core.check.DeLexiconBuilder \
  jdbc:postgresql://localhost/snomed snomed snomed \
  de 900000000000509007 /data/lexika
```
Der Prozess protokolliert Anzahl verarbeiteter Beschreibungen und den Pfad zur Allowlist.

## Verarbeitungsschritte (gemeinsam)
1. **Datenzugriff:** SQL mit Window-Funktion zieht je Beschreibung die jüngste aktive Version inkl. Acceptability.
2. **Tokenizer:**
   - `DeTokenizer` entfernt Genitiv-Apostrophe, splittert Hyphen-Ketten, erkennt Mehrfachleerzeichen.
   - `ItTokenizer` entfernt Elisionspräfixe (`l'`, `dell'`, `un'` …) und behandelt Hyphen ähnlich.
3. **Tokenanalyse:**
   - Tokens kürzer als zwei Zeichen werden ignoriert.
   - Häufigkeiten (`frequencies`) zählen Gesamtaufkommen je Token (lowercased).
   - `termCounts` merkt pro Token, wie viele Beschreibungen es enthält.
   - `capitalizedCounts` vs. `lowercaseCounts` erkennen Großschreibung.
   - `examples` speichert die erste Beschreibung, in der ein Token auftaucht (Term, Concept-ID, Acceptability).
4. **Kategorisierung:**
   - INN-Suffixe → `inn` (z. B. `-mab`, `-nib`).
   - Lateinische Endungen → `latin` (`-itis`, `-osis`, `-um`).
   - Großgeschriebene Tokens innerhalb taxonomischer Muster → `taxon`.
   - Großgeschriebene Tokens außerhalb von Taxa → `eponym`.
   - All-Caps/Nummern → `acronym`.
   - Rest → `unknown`.
5. **Häufigkeitsschwellen:**
   - `FREQ_THRESHOLD = 3` (Tokens mit ≥3 Vorkommen landen regulär in den Allowlisten/Kategorien).
   - Tokens mit 1–2 Treffern erzeugen die Verdachtsliste (`*_suspects.tsv`).
6. **Hunspell-Abgleich:** Wird beim Schreiben optional mit einem `SpellingChecker` validiert; Wörter, die Hunspell bereits kennt, werden nicht erneut erlaubt.
7. **Dateischreiben:** Siehe nächstes Kapitel.

## Ausgabestruktur (`dictionary_<lang>/`)
| Datei | Inhalt |
| --- | --- |
| `<lang>_allowlist.txt` | Hauptliste validierter Tokens (kleingeschrieben), die nicht in speziellen Kategorien sind. |
| `<lang>_inn.txt`, `_latin.txt`, `_eponyms.txt`, `_taxons.txt`, `_acronyms.txt` | Kategorie-spezifische Tokens (INN, Latinitäten, Eigennamen, Taxa, Akronyme). Eigennamen/Taxa behalten Originalschreibweise. |
| `<lang>_typos.txt` | Optional; manuelle Korrekturpaare `fehler=korrektur`. Wird vom Checker eingelesen. |
| `<lang>_suspects.tsv` | Tokens mit geringer Frequenz. Spalten: Token, absolute Frequenz, Anzahl betroffener Begriffe, Kategorie, Beispielterm, Concept-ID, Acceptability. |
| `<lang>_stats.txt` | Kennzahlen: Anzahl Beschreibungen, einzigartige Tokens, High-/Low-Frequency-Tokens, Kategorieverteilung. |

Alle Dateien sind UTF-8 ohne BOM und werden durch `writeOutputs(...)` erzeugt.

## Sprachspezifische Hinweise
### Deutsch (`DeLexiconBuilder`)
- Tokenizer: berücksichtigt `'s` (z. B. "Müller's" → "Müller"), spaltet zusammengesetzte Hyphen-Terms.
- Kategorien:
  - **`inn`** pharmazeutische International Nonproprietary Names anhand regulärer Ausdrücke.
  - **`latin`** medizinisch-lateinische Endungen.
  - **`eponym`** Großschreibung ohne Taxonmuster.
  - **`taxon`** Großschreibung + folgendes kleingeschriebenes Token (z. B. "Escherichia coli").
  - **`acronym`** rein alphanumerische Großbuchstabenkombinationen.
- `CAT_DE` selbst wird derzeit nur intern genutzt; Ausgabedateien heißen `de_*`.
- Output-Verzeichnis: `dictionary_de/` mit `de_allowlist.txt`, `de_inn.txt`, etc.

### Italienisch (`ItLexiconBuilder`)
- Tokenizer: entfernt Elisionspräfixe (`l'`, `dell'`, `all'`, `gl'`, `dov'`, etc.) vor der Tokenisierung.
- Kategorien identisch zu Deutsch (REGEX wiederverwendet).
- Spezifische Verbraucher:
  - `ItalianTranslationRuleChecker` lädt `HunspellSpellingChecker("it")`, der wiederum `dictionary_it` nutzt.
  - Regeln wie `ss4`, `um4`, `um5` etc. verlassen sich auf korrekte Tokenisierung (z. B. Erkennung mehrfacher Leerzeichen, Zahl/Einheiten-Trennung, Mikro-Suffixe).
- Output-Verzeichnis: `dictionary_it/` mit analogen Dateien (`it_allowlist.txt`, `it_inn.txt`, …).

## Einsatz im Laufzeitsystem
1. **HunspellSpellingChecker**
   - Konstruktor erwartet optional `extraAllowListFile`, `extraTypoFile`, `lexiconRoot`.
   - Wenn `lexiconRoot` gesetzt ist, wird dort `dictionary_<lang>/` gesucht (sonst werden eingebettete Ressourcen verwendet).
   - Der Checker kombiniert Hunspell (`*.aff/*.dic`) mit den Ebene-1-Listen (`allowListCi`, `allowListCs`) und Typos.
   - Property `levi.spelling.lexiconDir` legt den Root für alle Sprachen fest (z. B. `-Dlevi.spelling.lexiconDir=/data/lexika`).

2. **TranslationRuleCheckers.forLanguage(...)**
   - Gibt für `"it"` den `ItalianTranslationRuleChecker` zurück.
   - Dieser ruft `HunspellSpellingChecker("it")` auf und nutzt das Lexikon innerhalb seiner Regeln (`ss4` etc.).

3. **GUI / CLI-Verbrauch**
   - `CompareManager`, `HunspellSpellingChecker` oder GUI-Controller setzen `levi.spelling.lexiconDir` vor dem Start oder konfigurieren Pfade über Properties.
   - Nutzer übergeben denselben Lexikon-Ordner an Batch-Tools wie `SnomedComparator` oder `CompareManager`, damit die Laufzeitprüfung identische Daten verwendet wie der Build.

## Typische Workflows
1. **Lexikon neu generieren**
   1. Verbindung zur SNOMED-Datenbank prüfen.
   2. `DeLexiconBuilder` und/oder `ItLexiconBuilder` mit gewünschter Sprache/Refset starten.
   3. Output-Verzeichnis in Versionskontrolle oder Artefaktablage übernehmen (z. B. `levi-core/src/main/resources/spelling/dictionary_de`).
   4. Optional Typos/Allowlists manuell kuratieren (z. B. seltene Token aus `*_suspects.tsv` prüfen und verschieben).

2. **Lexikon in Tests verwenden**
   - Unit-Tests (`DeLexiconBuilderTest`, `ItLexiconBuilderTest`, Tokenizer-Tests) prüfen Tokenisierung und Kategorie-Logik isoliert.
   - Für Integrations- oder Regressionstests den generierten Ordner als temporären `lexiconRoot` mounten.

3. **Fehlerdiagnose**
   - **Falsche Treffer**: Prüfen, ob Token in `*_allowlist.txt` fehlt oder Hunspell es bereits korrekt erkennt.
   - **Nicht erkannte INN/Latein**: Regex anpassen (`INN_SUFFIX`, `LATIN_SUFFIX`).
   - **Performanz**: Statistikdatei (`*_stats.txt`) liefert Anzahl verarbeiteter Beschreibungen und Token – Grundlage für Vergleiche zwischen Releases.

## Hintergrundlogik für KI-Erklärungen
Wenn die KI einem Nutzer erklärt, warum ein bestimmter Begriff akzeptiert oder beanstandet wird, sollte sie auf folgende Mechanismen verweisen:
- **Tokenisierung & Normalisierung**: Beschreiben, dass Akzente (NFC), Apostrophe/Elisionen und Hyphen vor der Analyse bereinigt werden.
- **Frequenzbasierte Entscheidung**: Nur Tokens, die in mindestens drei Beschreibungen vorkommen, werden automatisch akzeptiert; seltene Tokens landen auf einer Verdachtsliste.
- **Kategorien**: Bestimmte Endungen oder Großschreibung weisen auf pharmazeutische Namen, lateinische Begriffe, Eigennamen oder biologische Taxa hin und erhalten eigene Listen.
- **Hunspell + Ebene 1**: Das System kombiniert standardisierte Hunspell-Wörterbücher (`*.aff`/`*.dic`) mit projektspezifischen Allowlisten, um medizinische Fachausdrücke abzudecken.
- **Typo-Korrektur**: `*_typos.txt` und das `typoMap` des Checkers erlauben automatische Korrekturvorschläge (definitive vs. unsichere Befunde).
- **Integration in Regelprüfungen**: Die italienische Regel-Engine nutzt die Lexika, um z. B. Mehrfachleerzeichen, Dezimaltrennzeichen, Einheitenformatierung und Rechtschreibung zu validieren.

Mit diesen Informationen kann eine KI präzise erklären, wie Begriffe analysiert, kategorisiert und letztlich validiert werden – unabhängig davon, ob ein Nutzer das Lexikon neu erstellt oder nur einen Check in der Oberfläche ausführt.
