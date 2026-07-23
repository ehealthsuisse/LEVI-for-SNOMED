package ch.ehealth.levi.core.processor;

import java.io.IOException;
import java.util.List;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import ch.ehealth.levi.core.export.ResultCollector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SiInactivationsCSVProcessor extends CsvProcessor{

		private static final Logger logger = LoggerFactory.getLogger(TermspaceInactivationsCsvProcessor.class);
		private ResultCollector collector;
		
		public SiInactivationsCSVProcessor(CSVReader csvReader, ResultCollector collector, String languageCodeFilter) {
	        super(csvReader, languageCodeFilter);
			this.collector = collector;
	    }
		
		 @Override
		    public void process() throws IOException {
		        List<String[]> rows = null;
		        boolean isFirstRow = true;
		        String languageCode = languageCodeFilter;
		        
		        if (languageCode == null) {
		        	logger.error("No language code filter configured for SNOMED International inactivation CSV. "
		        			+ "Set a language code filter in the configuration.");
		        	throw new IllegalStateException("Cannot process SNOMED International inactivation CSV without language code. "
		        			+ "Set a language code filter in the configuration.");
		        }
		        
				try {
					rows = csvReader.readAll();
				} catch (IOException | CsvException e) {
					logger.error("Failed to read CSV rows: {}", e.getMessage());
				}
		        for (String[] row : rows) {
		        	if (isFirstRow) {
						isFirstRow = false;
						continue;
					}
					
					String descriptionId = row[0];
					String term = row[4];
					String conceptId = row[2];
										
					collector.setFullInactivationsCurrent(
							descriptionId, term, languageCode, conceptId);
				}
		    }
	
	
	
}
