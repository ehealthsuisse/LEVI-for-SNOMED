package ch.ehealth.levi.core.compare;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import ch.ehealth.levi.core.Conf;
import ch.ehealth.levi.core.export.BatchExportService;
import ch.ehealth.levi.core.export.ResultCollector;
import ch.ehealth.levi.core.io.FileReaderUtil;
import ch.ehealth.levi.core.io.FileWriterUtil;

public class CompareManager {

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
    

	public void runTranslationOverview(String path, String destination)
			throws IOException, ClassNotFoundException, SQLException {
		reportProgress("job.progress.reading");
		reader.readFile(path);
		
		reportProgress("job.progress.creating_overview");
		writer.writeToFile(destination + "\\TranslationOverview.tsv", comparator.createTranslationsOverview());
	}

	public void runDeltaDescAdditions(String path, String destination)
			throws IOException, ClassNotFoundException, SQLException {
		reportProgress("job.progress.reading");
		reader.readFile(path);
		
		reportProgress("job.progress.generating_additions");
		List<List<String>> additions = comparator.generateDescriptionAdditionAndChangesDelta();
		lastAdditionsCount = Math.max(0, additions.size() - 1);
		writer.writeToFile(destination + "\\DeltaDescAdditions.tsv", additions);
		if(resultCollector.containsType("TRANSLATION_CHANGES")) {
			List<List<String>> changes = comparator.generateDescriptionChangesDelta("TRANSLATION_CHANGES");
			lastChangesCount = Math.max(0, changes.size() - 1);
			writer.writeToFile(destination + "\\DeltaDescChanges.tsv", changes);
		}
	}
	
	public void runDeltaDescInactivations(String path, String destination) throws ClassNotFoundException, IOException, SQLException {
		reportProgress("job.progress.reading");
		reader.readFile(path);
		
		reportProgress("job.progress.generating_inactivations");
		List<List<String>> inactivations = comparator.generateDescriptionInactivationDelta();
		lastInactivationsCount = Math.max(0, inactivations.size() - 1);
		writer.writeToFile(destination + "\\DeltaDescInactivations.tsv", inactivations);
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
				writer.writeToFile(destination + "\\DeltaDescAdditions.tsv", additions);
				if (changes != null) {
					writer.writeToFile(destination + "\\DeltaDescChanges.tsv", changes);
				}
				if (reactivations != null) {
					writer.writeToFile(destination + "\\DeltaDescReactivation.tsv", reactivations);
				}
				writer.writeToFile(destination + "\\DeltaDescInactivations.tsv", inactivations);
			}
	}
	
	public void runCheckEszettInExtension(String destination) throws ClassNotFoundException, IOException, SQLException {
		reportProgress("job.progress.checking_eszett");
		String fileName = "\\EszettInactivations.tsv";
		int i = 0;
		
		for (List<List<String>> entry : comparator.checkEszettInExtension()) {
			if (i > 0) {
				fileName = "\\EszettAdditions.tsv";
			}
			writer.writeToFile(destination + fileName, entry);
			i++;
		}
	}

	public void runDeltaNotPublishedTranslations (String pathCurrent, String pathPrevious, String destination) throws IOException, ClassNotFoundException, SQLException {
		reportProgress("job.progress.reading_current");
		reader.readFile(pathCurrent);
		
		reportProgress("job.progress.reading_previous");
		reader.readFile(pathPrevious);

		reportProgress("job.progress.finding_unpublished");
		reportProgress("job.progress.writing");
		writer.writeToFile(destination + "\\DeltaNotPublishedTranslations.tsv", comparator.generateDeltaOfNotPublishedTranslations());
		
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
		reader.readFile(pathPrevious);
		
		reportProgress("job.progress.finding_unpublished");
		reportProgress("job.progress.writing");
		writer.writeToFile(destination + "\\DeltaNotPublishedTranslations.tsv",
				comparator.generateDeltaOfNotPublishedTranslations());
	}
	
	public void runCheckDuplicateTerms(String destination) 
	        throws IOException, ClassNotFoundException, SQLException {
	    writer.writeToFile(destination + "\\DuplicateTerms.tsv", 
	        comparator.checkDuplicateTerms());
	}

}
