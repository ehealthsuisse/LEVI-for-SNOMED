package translation.check;

import java.io.IOException;
import java.util.List;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

public class PropCsvProcessor extends CsvProcessor{
	
	private ResultCollector collector;
	private static Conf conf = new Conf();
	
	public PropCsvProcessor(CSVReader csvReader, ResultCollector collector) {
        super(csvReader);
        this.collector = collector;
    }

	 @Override
	    public void process() throws IOException {
	        // Implementiere die Logik für .propcsv.csv
	        List<String[]> rows = null;
			try {
				rows = csvReader.readAll();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (CsvException e) {
				e.printStackTrace();
			}
			
			boolean isFirstRow = true;
	        for (String[] row : rows) {
	        	
	        	if (isFirstRow) {
		        	isFirstRow = false;
		        	continue; // Skip the header row
	        	}
	        	
	    		// Determine the language based on cell content
				String languageCode = null;
				String conceptId = row[1];
				String fsn = row[2];
				String term = null;
				String languageReferenceSet = null;
				for (String cell : row) {
				    // Überprüfen, ob der String einen Sprachcode enthält
				    if (cell.contains("|")) {
				        String[] parts = cell.split("\\|");
				        
				        languageCode = parts[0];
				        
				        // Extrahiere nur den Basis-Sprachcode (z.B. "de" aus "de-CH")
				        String baseLanguageCode = languageCode.split("-")[0];
				        languageReferenceSet = conf.getLanguageRefSetId(baseLanguageCode);
				        term = parts[2]; 
				        collector.setFullNewTranslationCurrent(
				        		conceptId, fsn, 
				        		"", //placeholder for pt,
				        		term, languageCode,
				        		"", // placeholder for caseSignificance
				        		"", // placeholder for type
				        		languageReferenceSet,
				        		"", // placeholder for acceptabilityId
				        		"", // placeholder for language_reference_set2
				        		"", // placeholder for acceptabilityId2
				        		"", // placeholder for language_reference_set3
				        		"", // placeholder for acceptabilityId3
				        		"", // placeholder for language_reference_set4
				        		"", // placeholder for acceptabilityId4
				        		"", // placeholder for language_reference_set5
				        		"", // placeholder for acceptabilityId5
				        		"" // placeholder for notes
				        		);
				    }
				}
	        }
	    }
}
