package translation.check;

import java.io.IOException;
import java.util.List;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

public class SimpleOverview extends CsvProcessor{
	
	private ResultCollector collector;
	
	public SimpleOverview(CSVReader csvReader, ResultCollector collector) {
        super(csvReader);
        // Assign the injected collector so it's available in process()
        this.collector = collector;
    }

    @Override
    public void process() throws IOException {
        List<String[]> rows = null;
		try {
			rows = csvReader.readAll();
		} catch (IOException | CsvException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// defensive: if reading failed rows may be null
		if (rows == null || rows.isEmpty()) {
			return;
		}
		
		boolean isFirstRow = true;
        for (String[] row : rows) {
        	if (isFirstRow) {
				isFirstRow = false;
				continue; // Skip the header row
			}
        	String conceptId = row[0];
        	collector.setFullNewTranslationCurrent(
	        		conceptId, "", 
	        		"", //placeholder for pt,
	        		"", // placeholder for term
	        		"", // placeholder for languageCode
	        		"", // placeholder for caseSignificance
	        		"", // placeholder for type
	        		"", // placeholder for language_reference_set
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