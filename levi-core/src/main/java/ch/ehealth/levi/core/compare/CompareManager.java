package ch.ehealth.levi.core.compare;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

import ch.ehealth.levi.core.Conf;
import ch.ehealth.levi.core.check.Finding;
import ch.ehealth.levi.core.check.FrenchTranslationRuleChecker;
import ch.ehealth.levi.core.check.TranslationCheckContext;
import ch.ehealth.levi.core.check.TranslationRuleChecker;
import ch.ehealth.levi.core.check.TranslationRuleCheckers;
import ch.ehealth.levi.core.export.BatchExportService;
import ch.ehealth.levi.core.export.ResultCollector;
import ch.ehealth.levi.core.io.FileReaderUtil;
import ch.ehealth.levi.core.io.FileWriterUtil;

public class CompareManager {

	private static final Logger logger = LoggerFactory.getLogger(CompareManager.class);

	private final ResultCollector resultCollector;
	private final FileReaderUtil reader;
	private final FileWriterUtil writer;
	private final Comparator comparator;
	private final BatchExportService batchExportService;
	private final boolean groupingEnabled;
	private final Conf conf;
	
    private ProgressListener progressListener;


	// Counts populated after each run* call
	private int lastAdditionsCount;
	private int lastChangesCount;
	private int lastReactivationsCount;
	private int lastInactivationsCount;
	private int lastNotPublishedCount;

	// Counts populated after a translation-rule check run
	private int lastCheckPassCount;
	private int lastCheckUncertainCount;
	private int lastCheckFailCount;
	private int lastCheckRuleCount;
	private final Map<String, Integer> lastCheckRuleCounts = new LinkedHashMap<>();

    public CompareManager(Conf conf) {
        this.conf              = conf;
        this.resultCollector   = new ResultCollector();
        this.reader            = new FileReaderUtil(resultCollector, conf.getLanguageCodeFilter());
        this.writer            = new FileWriterUtil();
        this.comparator        = new Comparator(resultCollector, conf);
        this.batchExportService = new BatchExportService();
		this.groupingEnabled = conf.isGroupingEnabled();
    }
    
    public void setProgressListener(ProgressListener listener) {
        this.progressListener = listener;
    }

    private void reportProgress(String messageKey) {
        if (progressListener != null) {
            progressListener.onProgress(messageKey);
        }
    }   
    

	public int getLastAdditionsCount()     { return lastAdditionsCount; }
	public int getLastChangesCount()       { return lastChangesCount; }
	public int getLastReactivationsCount() { return lastReactivationsCount; }
	public int getLastInactivationsCount() { return lastInactivationsCount; }
	public int getLastNotPublishedCount()  { return lastNotPublishedCount; }

	public int getLastCheckPassCount()       { return lastCheckPassCount; }
	public int getLastCheckUncertainCount()  { return lastCheckUncertainCount; }
	public int getLastCheckFailCount()       { return lastCheckFailCount; }
	public int getLastCheckRuleCount()       { return lastCheckRuleCount; }
	public Map<String, Integer> getLastCheckRuleCounts() { return lastCheckRuleCounts; }
    

	public void runTranslationOverview(String path, String destination)
			throws IOException, ClassNotFoundException, SQLException {
		reportProgress("job.progress.reading");
		reader.readFile(path);
		
		reportProgress("job.progress.creating_overview");
		try {
			comparator.acquireDbLease();
			writer.writeToFile(destination + "TranslationOverview.tsv", comparator.createTranslationsOverview());
		} finally {
			comparator.releaseDbLease();
		}
	}

	public void runDeltaDescAdditions(String path, String destination)
			throws IOException, ClassNotFoundException, SQLException {
		reportProgress("job.progress.reading");
		reader.readFile(path);
		
		reportProgress("job.progress.generating_additions");
		try {
			comparator.acquireDbLease();
			List<List<String>> additions = comparator.generateDescriptionAdditionAndChangesDelta();
			additions = runFrenchCheckAndSplit(additions, destination, "DeltaDescAdditions");
			lastAdditionsCount = Math.max(0, additions.size() - 1);
			writer.writeToFile(destination + "DeltaDescAdditions.tsv", additions);
			if(resultCollector.containsType("TRANSLATION_CHANGES")) {
				List<List<String>> changes = comparator.generateDescriptionChangesDelta("TRANSLATION_CHANGES");
				lastChangesCount = Math.max(0, changes.size() - 1);
				writer.writeToFile(destination + "DeltaDescChanges.tsv", changes);
			}
		} finally {
			comparator.releaseDbLease();
		}
	}
	
	public void runDeltaDescInactivations(String path, String destination) throws ClassNotFoundException, IOException, SQLException {
		reportProgress("job.progress.reading");
		reader.readFile(path);
		
		reportProgress("job.progress.generating_inactivations");
		try {
			comparator.acquireDbLease();
			List<List<String>> inactivations = comparator.generateDescriptionInactivationDelta();
			lastInactivationsCount = Math.max(0, inactivations.size() - 1);
			writer.writeToFile(destination + "DeltaDescInactivations.tsv", inactivations);
		} finally {
			comparator.releaseDbLease();
		}
	}
	
	/**
	 * Generates all four delta datasets (additions, changes, inactivations,
	 * reactivations), updates the {@code lastXxxCount} fields, then delegates
	 * to {@link BatchExportService} which groups concepts by their combination
	 * of change types and writes aligned, optionally batch-split output files.
	 *
	 * <p><em>Note:</em> {@link Comparator#generateDescriptionAdditionAndChangesDelta()}
	 * must be called first because it populates the description→concept ID mapping
	 * in {@link ResultCollector} that is required for resolving concept IDs in the
	 * changes and reactivations deltas.</p>
	 */
	public void runGenerateDelta(String path, String destination) throws ClassNotFoundException, IOException, SQLException {
		reportProgress("job.progress.reading");
		reader.readFile(path);

		generateDeltaAndWrite(destination);
	}

	/**
	 * Like {@link #runGenerateDelta} but reuses the current file data already
	 * loaded into the result collector by a preceding job (e.g. not-published),
	 * skipping a second read of the large XLS.
	 */
	public void runGenerateDeltaReusingCurrent(String destination)
			throws ClassNotFoundException, IOException, SQLException {
		reportProgress("job.progress.reusing_data");
		generateDeltaAndWrite(destination);
	}

	private void generateDeltaAndWrite(String destination) throws ClassNotFoundException, IOException, SQLException {
		try {
			comparator.acquireDbLease();

			reportProgress("job.progress.generating_inactivations");
			List<List<String>> inactivations = comparator.generateDescriptionInactivationDelta();
			lastInactivationsCount = Math.max(0, inactivations.size() - 1);

	        reportProgress("job.progress.generating_additions");
			List<List<String>> additions = comparator.generateDescriptionAdditionAndChangesDelta();
			additions = runFrenchCheckAndSplit(additions, destination, "DeltaDescAdditions");
			lastAdditionsCount = Math.max(0, additions.size() - 1);

			reportProgress("job.progress.generating_changes");
			List<List<String>> changes = null;
			if (resultCollector.containsType("TRANSLATION_CHANGES")) {
				changes = comparator.generateDescriptionChangesDelta("TRANSLATION_CHANGES");
				lastChangesCount = Math.max(0, changes.size() - 1);
			}

			reportProgress("job.progress.generating_reactivations");
			List<List<String>> reactivations = null;
			if (resultCollector.containsType("TRANSLATION_REACTIVATION")) {
				reactivations = comparator.generateDescriptionChangesDelta("TRANSLATION_REACTIVATION");
				lastReactivationsCount = Math.max(0, reactivations.size() - 1);
			}

	        reportProgress("job.progress.writing");
			if (groupingEnabled) {
					batchExportService.export(additions, changes, inactivations, reactivations,
							resultCollector, destination);
				} else {
					writer.writeToFile(destination + "DeltaDescAdditions.tsv", additions);
					if (changes != null) {
						writer.writeToFile(destination + "DeltaDescChanges.tsv", changes);
					}
					if (reactivations != null) {
						writer.writeToFile(destination + "DeltaDescReactivation.tsv", reactivations);
					}
					writer.writeToFile(destination + "DeltaDescInactivations.tsv", inactivations);
				}
		} finally {
			comparator.releaseDbLease();
		}
	}
	
	public void runCheckEszettInExtension(String destination) throws ClassNotFoundException, IOException, SQLException {
		reportProgress("job.progress.checking_eszett");
		String fileName = "EszettInactivations.tsv";
		int i = 0;
		
		try {
			comparator.acquireDbLease();
			for (List<List<String>> entry : comparator.checkEszettInExtension()) {
				if (i > 0) {
					fileName = "EszettAdditions.tsv";
				}
				writer.writeToFile(destination + fileName, entry);
				i++;
			}
		} finally {
			comparator.releaseDbLease();
		}
	}

	public void runDeltaNotPublishedTranslations (String pathCurrent, String pathPrevious, String destination) throws IOException, ClassNotFoundException, SQLException {
		reportProgress("job.progress.reading_current");
		reader.readFile(pathCurrent, "current");
		
		reportProgress("job.progress.reading_previous");
		reader.readFile(pathPrevious, "previous");

		reportProgress("job.progress.finding_unpublished");
		reportProgress("job.progress.writing");
		try {
			comparator.acquireDbLease();
			List<List<String>> delta = comparator.generateDeltaOfNotPublishedTranslations();
			lastNotPublishedCount = Math.max(0, delta.size() - 1);
			writer.writeToFile(destination + "DeltaNotPublishedTranslations.tsv", delta);
		} finally {
			comparator.releaseDbLease();
		}
	}

	/**
	 * Like {@link #runDeltaNotPublishedTranslations} but skips loading the current
	 * file because its data (NEW_TRANSLATION_CURRENT, TRANSLATION_INACTIVATION_CURRENT)
	 * is already present in the resultCollector from a preceding job (e.g. translate-delta).
	 * Only the previous file is loaded, avoiding a second in-memory copy of the large XLS.
	 */
	public void runDeltaNotPublishedTranslationsReusingCurrent(String pathPrevious, String destination)
			throws IOException, ClassNotFoundException, SQLException {
        reportProgress("job.progress.reusing_data");
		reader.readFile(pathPrevious, "previous");
		
		reportProgress("job.progress.finding_unpublished");
		reportProgress("job.progress.writing");
		try {
			comparator.acquireDbLease();
			List<List<String>> delta = comparator.generateDeltaOfNotPublishedTranslations();
			lastNotPublishedCount = Math.max(0, delta.size() - 1);
			writer.writeToFile(destination + "DeltaNotPublishedTranslations.tsv", delta);
		} finally {
			comparator.releaseDbLease();
		}
	}
	
	public void runCheckDuplicateTerms(String destination) 
	        throws IOException, ClassNotFoundException, SQLException {
		try {
			comparator.acquireDbLease();
	    	writer.writeToFile(destination + "DuplicateTerms.tsv", 
	    	        comparator.checkDuplicateTerms());
		} finally {
			comparator.releaseDbLease();
		}
	}

	/**
	 * Runs a French-vs-Swiss SNOMED extension comparison (ported from LexSync-SCT)
	 * and produces the same output files as LexSync-SCT, plus split into the
	 * LEVI G1–G15 change-type groups with batch splitting.
	 *
	 * <p>The five LexSync-SCT output files are written first:
	 * {@code FR_DescriptionChanges.tsv}, {@code FR_DescriptionsAdditions.tsv},
	 * {@code FR_DescriptionInactivations_INACTIVE_IN_FR_Inactivations.tsv},
	 * {@code FR_DescriptionsAdditions_MISSING_IN_FR.tsv} and
	 * {@code FR_DescriptionReactivations.tsv}.</p>
	 *
	 * <p>When grouping is enabled ({@link Conf#isGroupingEnabled()}) the same
	 * comparison results are additionally exported into G1–G15 group files with
	 * optional batch splitting (see {@link SnomedBatchExporter}).</p>
	 *
	 * @param frDescPath path to the FR sct2_Description file
	 * @param chDescPath path to the CH sct2_Description file
	 * @param frLangPath path to the FR der2_cRefset_Language file
	 * @param chLangPath path to the CH der2_cRefset_Language file
	 * @param destination output directory
	 */
	public void runSnomedComparison(String frDescPath, String chDescPath,
			String frLangPath, String chLangPath, String destination)
			throws IOException {
		reportProgress("job.progress.reading");
		SnomedLoader loader = new SnomedLoader();

		var descFR = loader.loadDescriptions(Path.of(frDescPath));
		var descCH = loader.loadDescriptions(Path.of(chDescPath));
		var langFR = loader.loadLanguageRefset(Path.of(frLangPath));
		var langCH = loader.loadLanguageRefset(Path.of(chLangPath));

		var frFull = loader.enrich(descFR, langFR);
		var chFull = loader.enrich(descCH, langCH);

		reportProgress("job.progress.generating_additions");
		var comparator = new SnomedComparator();
		var results = comparator.compare(frFull, chFull);

		// LexSync-SCT style flat output files
		var exporter = new CsvExporter();
		exporter.exportDescriptionChangesDelta(results, destination + "FR_DescriptionChanges.tsv");

		// New French descriptions: run the full French translation-rule check.
		// Rows that pass are written as before; rows with findings go into a
		// separate manual-review file (<base>_toCheck.tsv).
		List<MatchResult> additionsResults = results.stream()
				.filter(r -> isAdditionStatus(r.status()))
				.collect(Collectors.toList());

		List<List<String>> additionsRows = MatchResultDeltas.buildAdditions(additionsResults);
		List<List<String>> passedAdditions;
		try {
			passedAdditions = runFrenchCheckAndSplit(additionsRows, destination, "FR_DescriptionsAdditions");
		} catch (ClassNotFoundException | SQLException e) {
			throw new IOException("French lexicon check failed: " + e.getMessage(), e);
		}
		writer.writeToFile(destination + "FR_DescriptionsAdditions.tsv", passedAdditions);

		exporter.exportActiveCHInactiveFRDelta(results, destination + "FR_DescriptionInactivations.tsv");
		exporter.exportReactivationDelta(results, destination + "FR_DescriptionReactivations.tsv");

		// LEVI G1–G15 grouping + batch splitting (when enabled). Only additions
		// that passed the French check are grouped; all other results unchanged.
		reportProgress("job.progress.writing");
		if (groupingEnabled) {
			Set<String> passedKeys = new HashSet<>();
			for (int i = 1; i < passedAdditions.size(); i++) {
				List<String> row = passedAdditions.get(i);
				if (row != null && !row.isEmpty()) {
					passedKeys.add(row.get(0) + "\t" + row.get(3));
				}
			}
			List<MatchResult> groupable = results.stream()
					.filter(r -> !isAdditionStatus(r.status()) || passedKeys.contains(resultKey(r)))
					.collect(Collectors.toList());
			new SnomedBatchExporter().exportGrouped(groupable, resultCollector, destination);
		}

		reportProgress("job.progress.done");
	}

	/**
	 * Runs the translation-rule check (e.g. French) over an additions TSV and
	 * writes three files:
	 * <ul>
	 * <li>{@code <base>_reviewed.tsv} – all rows; rule findings are appended to
	 * the Notes column as {@code status:rule_id:message} tokens.</li>
	 * <li>{@code <base>_clean.tsv} – only the rows that passed without any fail
	 * or uncertain finding.</li>
	 * <li>{@code <base>_triage.tsv} – one row per fail/uncertain finding for
	 * manual review.</li>
	 * </ul>
	 *
	 * <p>The base file name is derived from the input file (e.g.
	 * {@code FR_DescriptionsAdditions.tsv} → {@code FR_DescriptionsAdditions}).</p>
	 *
	 * @param path         the additions TSV to review
	 * @param destination  output directory
	 * @param languageCode the language of the checker to use (e.g. "fr")
	 */
	public void runTranslationCheck(String path, String destination, String languageCode)
			throws IOException, ClassNotFoundException, SQLException {
		reportProgress("job.progress.reading");
		reader.readFile(path);

		TranslationRuleChecker checker = TranslationRuleCheckers.forLanguage(languageCode);
		List<List<String>> rows = resultCollector.getDataByType("NEW_TRANSLATION_CURRENT");
		logger.info("Translation check ({}): reviewing {} rows.", languageCode, rows.size());

		Set<String> conceptIds = new LinkedHashSet<>();
		for (List<String> row : rows) {
			if (!row.isEmpty()) {
				conceptIds.add(row.get(0));
			}
		}

		reportProgress("job.progress.fetching_db");
		Map<String, String[]> enTerms = new HashMap<>();
		try {
			comparator.acquireDbLease();
			try {
				enTerms = comparator.fetchEnglishFsnAndPt(conceptIds);
			} catch (SQLException | RuntimeException e) {
				logger.warn("Could not fetch English FSN/PT from the database: {}", e.getMessage());
			}
		} catch (ClassNotFoundException e) {
			logger.warn("Could not connect to the database for English FSN/PT: {}", e.getMessage());
		} finally {
			comparator.releaseDbLease();
		}

		reportProgress("job.progress.checking");
		String base = baseName(path);
		List<List<String>> reviewed = new ArrayList<>();
		List<List<String>> clean = new ArrayList<>();
		List<List<String>> triage = new ArrayList<>();

		lastCheckRuleCounts.clear();
		lastCheckPassCount = 0;
		lastCheckUncertainCount = 0;
		lastCheckFailCount = 0;
		lastCheckRuleCount = 0;

		String headerAdditions = String.join("\t", DeltaColumns.ADDITIONS);
		String[] header = headerAdditions.split("\t");

		for (List<String> row : rows) {
			while (row.size() < 18) {
				row.add("");
			}
			String conceptId = row.get(0);
			String term = row.get(3);
			String acc = row.get(8);
			String existingNotes = row.get(17) == null ? "" : row.get(17);

			String[] en = enTerms.get(conceptId);
			String fsn = en == null || en[0] == null ? "" : en[0];
			String pt = en == null || en[1] == null ? "" : en[1];

			TranslationCheckContext ctx = new TranslationCheckContext(
					conceptId, fsn, pt, term, row.get(4), row.get(5), row.get(6), acc);

			List<Finding> findings = checker.check(ctx);
			List<String> tokens = new ArrayList<>();
			for (Finding f : findings) {
				if (f.needsNote()) {
					tokens.add(f.status() + ":" + f.ruleId() + ":" + f.message());
					lastCheckRuleCounts.merge(f.ruleId(), 1, Integer::sum);
					lastCheckRuleCount++;
					if ("fail".equals(f.status())) {
						lastCheckFailCount++;
					} else {
						lastCheckUncertainCount++;
					}
					triage.add(List.of(conceptId, acc == null ? "" : acc, term == null ? "" : term,
							f.status(), f.ruleId(), f.severity(), f.message(),
							String.valueOf(f.pdfPage()), f.section()));
				}
			}

			// Enrich the English reference columns only if the DB provided them.
			List<String> reviewedRow = new ArrayList<>(row);
			if (!fsn.isEmpty()) {
				reviewedRow.set(1, fsn);
				row.set(1, fsn);
			}
			if (!pt.isEmpty()) {
				reviewedRow.set(2, pt);
				row.set(2, pt);
			}
			if (!tokens.isEmpty()) {
				String joined = String.join(" ; ", tokens);
				reviewedRow.set(17, existingNotes.trim().isEmpty()
						? joined
						: existingNotes.trim() + " | " + joined);
				row.set(17, reviewedRow.get(17));
			}
			reviewed.add(reviewedRow);

			if (tokens.isEmpty()) {
				lastCheckPassCount++;
				clean.add(row);
			}
		}

		// Prepend headers
		reviewed.add(0, java.util.Arrays.asList(header));
		clean.add(0, java.util.Arrays.asList(header));
		triage.add(0, List.of("concept_id", "acceptability", "term_fr", "status", "rule_id",
				"severity", "message", "pdf_page", "section"));

		reportProgress("job.progress.writing");
		if (!destination.endsWith("/") && !destination.endsWith("\\")) {
			destination += "/";
		}
		writer.writeToFile(destination + base + "_reviewed.tsv", reviewed);
		writer.writeToFile(destination + base + "_clean.tsv", clean);
		writer.writeToFile(destination + base + "_triage.tsv", triage);
		logger.info("Translation check finished: pass={}, uncertain={}, fail={}, rules={}",
				lastCheckPassCount, lastCheckUncertainCount, lastCheckFailCount, lastCheckRuleCount);
	}

	private String baseName(String path) {
		String name = path;
		int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
		if (slash >= 0) {
			name = name.substring(slash + 1);
		}
		int dot = name.lastIndexOf('.');
		if (dot >= 0) {
			name = name.substring(0, dot);
		}
		return name;
	}

	/**
	 * Runs the full French translation-rule check (all rules incl. the ss4
	 * lexicon/spelling check) over the French rows of an additions delta. Rows
	 * that pass are returned (with the header row); rows that produce any
	 * fail/uncertain finding are written to {@code <destination><base>_toCheck.tsv}
	 * (same column layout as the additions delta, findings appended to the Notes
	 * column) for manual review.
	 *
	 * @param additions   the additions delta (header at index 0)
	 * @param destination output directory
	 * @param base        base file name (e.g. {@code DeltaDescAdditions} or
	 *                    {@code FR_DescriptionsAdditions})
	 * @return the passed additions delta (header + passing rows)
	 */
	private List<List<String>> runFrenchCheckAndSplit(
			List<List<String>> additions, String destination, String base)
			throws IOException, SQLException, ClassNotFoundException {
		reportProgress("job.progress.lexicon_check");

		String lexiconDir = conf.getLexiconDir();
		if (lexiconDir != null && !lexiconDir.isBlank()) {
			System.setProperty("levi.spelling.lexiconDir", lexiconDir);
		}
		TranslationRuleChecker checker = TranslationRuleCheckers.forLanguage("fr");

		Set<String> conceptIds = new LinkedHashSet<>();
		for (int i = 1; i < additions.size(); i++) {
			List<String> row = additions.get(i);
			if (row != null && !row.isEmpty()) {
				conceptIds.add(row.get(0));
			}
		}

		Map<String, String[]> enTerms = new HashMap<>();
		try {
			comparator.acquireDbLease();
			try {
				enTerms = comparator.fetchEnglishFsnAndPt(conceptIds);
			} catch (SQLException | RuntimeException e) {
				logger.warn("Could not fetch English FSN/PT from the database: {}", e.getMessage());
			}
		} catch (ClassNotFoundException | SQLException | RuntimeException e) {
			logger.warn("Could not connect to the database for English FSN/PT: {}", e.getMessage());
		} finally {
			comparator.releaseDbLease();
		}

		if (!destination.endsWith("/") && !destination.endsWith("\\")) {
			destination += "/";
		}

		List<List<String>> passed = new ArrayList<>();
		List<List<String>> toCheck = new ArrayList<>();
		passed.add(new ArrayList<>(additions.get(0)));
		toCheck.add(new ArrayList<>(additions.get(0)));

		lastCheckRuleCounts.clear();
		lastCheckPassCount = 0;
		lastCheckUncertainCount = 0;
		lastCheckFailCount = 0;
		lastCheckRuleCount = 0;

		for (int i = 1; i < additions.size(); i++) {
			List<String> row = new ArrayList<>(additions.get(i));
			while (row.size() < 18) {
				row.add("");
			}
			String languageCode = row.get(4) == null ? "" : row.get(4).trim().toLowerCase(Locale.ROOT);
			if (!"fr".equals(languageCode)) {
				passed.add(row);
				continue;
			}

			String conceptId = row.get(0);
			String term = row.get(3);
			String acc = row.get(8);
			String existingNotes = row.get(17) == null ? "" : row.get(17);

			String[] en = enTerms.get(conceptId);
			String fsn = en == null || en[0] == null ? "" : en[0];
			String pt = en == null || en[1] == null ? "" : en[1];

			TranslationCheckContext ctx = new TranslationCheckContext(
					conceptId, fsn, pt, term, row.get(4), row.get(5), row.get(6), acc);

			List<String> tokens = new ArrayList<>();
			boolean hasAnyFinding = false;
			for (Finding f : checker.check(ctx)) {
				hasAnyFinding = true;
				if (f.needsNote()) {
					tokens.add(f.status() + ":" + f.ruleId() + ":" + f.message());
					lastCheckRuleCounts.merge(f.ruleId(), 1, Integer::sum);
					lastCheckRuleCount++;
					if ("fail".equals(f.status())) {
						lastCheckFailCount++;
					} else {
						lastCheckUncertainCount++;
					}
				}
			}

			if (!hasAnyFinding) {
				lastCheckPassCount++;
				passed.add(row);
			} else {
				String joined = String.join(" ; ", tokens);
				row.set(17, existingNotes.trim().isEmpty()
						? joined
						: existingNotes.trim() + " | " + joined);
				toCheck.add(row);
			}
		}

		if (toCheck.size() > 1) {
			writer.writeToFile(destination + base + "_toCheck.tsv", toCheck);
			logger.info("French lexicon check: {} row(s) need manual review ({}_toCheck.tsv).",
					toCheck.size() - 1, base);
		}
		return passed;
	}

	/** True when the given comparison status represents a new-description addition. */
	private static boolean isAdditionStatus(MatchStatus status) {
		return status == MatchStatus.MISSING_IN_CH
				|| status == MatchStatus.TERM_ON_DIFFERENT_CONCEPT
				|| status == MatchStatus.MISSING_IN_FR;
	}

	/** Stable key (conceptId + term) for a comparison result, matching the additions row layout. */
	private static String resultKey(MatchResult r) {
		String conceptId = r.conceptId_FR() != null ? r.conceptId_FR()
				: (r.conceptId_CH() != null ? r.conceptId_CH() : "");
		return (conceptId == null ? "" : conceptId) + "\t" + (r.term() == null ? "" : r.term());
	}
}
