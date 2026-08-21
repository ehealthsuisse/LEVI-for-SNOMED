package ch.ehealth.levi.core.compare;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
		writer.writeToFile(destination + "TranslationOverview.tsv", comparator.createTranslationsOverview());
	}

	public void runDeltaDescAdditions(String path, String destination)
			throws IOException, ClassNotFoundException, SQLException {
		reportProgress("job.progress.reading");
		reader.readFile(path);
		
		reportProgress("job.progress.generating_additions");
		List<List<String>> additions = comparator.generateDescriptionAdditionAndChangesDelta();
		lastAdditionsCount = Math.max(0, additions.size() - 1);
		writer.writeToFile(destination + "DeltaDescAdditions.tsv", additions);
		if(resultCollector.containsType("TRANSLATION_CHANGES")) {
			List<List<String>> changes = comparator.generateDescriptionChangesDelta("TRANSLATION_CHANGES");
			lastChangesCount = Math.max(0, changes.size() - 1);
			writer.writeToFile(destination + "DeltaDescChanges.tsv", changes);
		}
	}
	
	public void runDeltaDescInactivations(String path, String destination) throws ClassNotFoundException, IOException, SQLException {
		reportProgress("job.progress.reading");
		reader.readFile(path);
		
		reportProgress("job.progress.generating_inactivations");
		List<List<String>> inactivations = comparator.generateDescriptionInactivationDelta();
		lastInactivationsCount = Math.max(0, inactivations.size() - 1);
		writer.writeToFile(destination + "DeltaDescInactivations.tsv", inactivations);
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
		reportProgress("job.progress.generating_inactivations");
		List<List<String>> inactivations = comparator.generateDescriptionInactivationDelta();
		lastInactivationsCount = Math.max(0, inactivations.size() - 1);

        reportProgress("job.progress.generating_additions");
		List<List<String>> additions = comparator.generateDescriptionAdditionAndChangesDelta();
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
	}
	
	public void runCheckEszettInExtension(String destination) throws ClassNotFoundException, IOException, SQLException {
		reportProgress("job.progress.checking_eszett");
		String fileName = "EszettInactivations.tsv";
		int i = 0;
		
		for (List<List<String>> entry : comparator.checkEszettInExtension()) {
			if (i > 0) {
				fileName = "EszettAdditions.tsv";
			}
			writer.writeToFile(destination + fileName, entry);
			i++;
		}
	}

	public void runDeltaNotPublishedTranslations (String pathCurrent, String pathPrevious, String destination) throws IOException, ClassNotFoundException, SQLException {
		reportProgress("job.progress.reading_current");
		reader.readFile(pathCurrent, "current");
		
		reportProgress("job.progress.reading_previous");
		reader.readFile(pathPrevious, "previous");

		reportProgress("job.progress.finding_unpublished");
		reportProgress("job.progress.writing");
		List<List<String>> delta = comparator.generateDeltaOfNotPublishedTranslations();
		lastNotPublishedCount = Math.max(0, delta.size() - 1);
		writer.writeToFile(destination + "DeltaNotPublishedTranslations.tsv", delta);
		
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
		List<List<String>> delta = comparator.generateDeltaOfNotPublishedTranslations();
		lastNotPublishedCount = Math.max(0, delta.size() - 1);
		writer.writeToFile(destination + "DeltaNotPublishedTranslations.tsv", delta);
	}
	
	public void runCheckDuplicateTerms(String destination) 
	        throws IOException, ClassNotFoundException, SQLException {
	    writer.writeToFile(destination + "DuplicateTerms.tsv", 
	        comparator.checkDuplicateTerms());
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
			enTerms = comparator.fetchEnglishFsnAndPt(conceptIds);
		} catch (SQLException | RuntimeException e) {
			logger.warn("Could not fetch English FSN/PT from the database: {}", e.getMessage());
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

		String headerAdditions = "Concept ID\tGB/US FSN Term (For reference only)\tPreferred Term (For reference only)"
				+ "\tTranslated Term\tLanguage Code\tCase significance\tTypeId\tLanguage reference set\tAcceptability"
				+ "\tLanguage reference set\tAcceptability\tLanguage reference set\tAcceptability"
				+ "\tLanguage reference set\tAcceptability\tLanguage reference set\tAcceptability\tNotes";
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
}
