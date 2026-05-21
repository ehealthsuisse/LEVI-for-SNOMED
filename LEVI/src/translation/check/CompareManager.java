package translation.check;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class CompareManager {

	private final ResultCollector resultCollector;
	private final FileReaderUtil reader;
	private final FileWriterUtil writer;
	private final Comparator comparator;

	// Counts populated after each run* call
	private int lastAdditionsCount;
	private int lastChangesCount;
	private int lastReactivationsCount;
	private int lastInactivationsCount;

    public CompareManager(Conf conf) {
        this.resultCollector = new ResultCollector();
        this.reader = new FileReaderUtil(resultCollector);
        this.writer = new FileWriterUtil();
        this.comparator = new Comparator(resultCollector, conf);
    }

	public int getLastAdditionsCount()     { return lastAdditionsCount; }
	public int getLastChangesCount()       { return lastChangesCount; }
	public int getLastReactivationsCount() { return lastReactivationsCount; }
	public int getLastInactivationsCount() { return lastInactivationsCount; }
    

	public void runTranslationOverview(String path, String destination)
			throws IOException, ClassNotFoundException, SQLException {
		reader.readFile(path);
		writer.writeToFile(destination + "\\TranslationOverview.tsv", comparator.createTranslationsOverview());
	}

	public void runDeltaDescAdditions(String path, String destination)
			throws IOException, ClassNotFoundException, SQLException {
		reader.readFile(path);
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
		reader.readFile(path);
		List<List<String>> inactivations = comparator.generateDescriptionInactivationDelta();
		lastInactivationsCount = Math.max(0, inactivations.size() - 1);
		writer.writeToFile(destination + "\\DeltaDescInactivations.tsv", inactivations);
	}
	
	public void runGenerateDelta(String path, String destination) throws ClassNotFoundException, IOException, SQLException {
		reader.readFile(path);

		List<List<String>> additions = comparator.generateDescriptionAdditionAndChangesDelta();
		lastAdditionsCount = Math.max(0, additions.size() - 1);
		writer.writeToFile(destination + "\\DeltaDescAdditions.tsv", additions);

		if(resultCollector.containsType("TRANSLATION_CHANGES")) {
			List<List<String>> changes = comparator.generateDescriptionChangesDelta("TRANSLATION_CHANGES");
			lastChangesCount = Math.max(0, changes.size() - 1);
			writer.writeToFile(destination + "\\DeltaDescChanges.tsv", changes);
		}

		if(resultCollector.containsType("TRANSLATION_REACTIVATION")) {
			List<List<String>> reactivations = comparator.generateDescriptionChangesDelta("TRANSLATION_REACTIVATION");
			lastReactivationsCount = Math.max(0, reactivations.size() - 1);
			writer.writeToFile(destination + "\\DeltaDescReactivation.tsv", reactivations);
		}

		List<List<String>> inactivations = comparator.generateDescriptionInactivationDelta();
		lastInactivationsCount = Math.max(0, inactivations.size() - 1);
		writer.writeToFile(destination + "\\DeltaDescInactivations.tsv", inactivations);
	}
	
	public void runCheckEszettInExtension(String destination) throws ClassNotFoundException, IOException, SQLException {
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
		reader.readFile(pathCurrent);
		reader.readFile(pathPrevious);
		
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
		reader.readFile(pathPrevious);
		writer.writeToFile(destination + "\\DeltaNotPublishedTranslations.tsv",
				comparator.generateDeltaOfNotPublishedTranslations());
	}
	
	public void runCheckDuplicateTerms(String destination) 
	        throws IOException, ClassNotFoundException, SQLException {
	    writer.writeToFile(destination + "\\DuplicateTerms.tsv", 
	        comparator.checkDuplicateTerms());
	}

}
