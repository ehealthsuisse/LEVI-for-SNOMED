package translation.check;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

public class DescriptionAdditionLoader {
	
	//TODO: Add check if the file is empty or has no rows


	public void loadAndInsertExcel(Sheet sheet, ResultCollector collector, String releaseType) {
        int rowCount = sheet.getPhysicalNumberOfRows();
        for (int i = 1; i < rowCount; i++) { // skip header
            Row row = sheet.getRow(i);
            if (row == null) continue;

            String conceptId = getCellAsString(row.getCell(0));
            String fsn = getCellAsString(row.getCell(1));
            String pt = getCellAsString(row.getCell(2));
            String term = getCellAsString(row.getCell(3));
            String languageCode = getCellAsString(row.getCell(4));
            String caseSignificance = getCellAsString(row.getCell(5));
            String type = getCellAsString(row.getCell(6));
            String language_reference_set = getCellAsString(row.getCell(7));
			String acceptabilityId = getCellAsString(row.getCell(8));
			String language_reference_set2 = getCellAsString(row.getCell(9));
			String acceptabilityId2 = getCellAsString(row.getCell(10));
			String language_reference_set3 = getCellAsString(row.getCell(11));
			String acceptabilityId3 = getCellAsString(row.getCell(12));
			String language_reference_set4 = getCellAsString(row.getCell(13));
			String acceptabilityId4 = getCellAsString(row.getCell(14));
			String language_reference_set5 = getCellAsString(row.getCell(15));
			String acceptabilityId5 = getCellAsString(row.getCell(16));
            String notes = getCellAsString(row.getCell(17));

            if(releaseType.equals("previous")) {
            	collector.setFullNewTranslationPrevious(
                        conceptId, fsn, pt, term, languageCode, caseSignificance, type,
                        language_reference_set, acceptabilityId,
                        language_reference_set2, acceptabilityId2,
                        language_reference_set3, acceptabilityId3,
                        language_reference_set4, acceptabilityId4,
                        language_reference_set5, acceptabilityId5,
                        notes
                    );
			} else {
				collector.setFullNewTranslationCurrent(
	                    conceptId, fsn, pt, term, languageCode, caseSignificance, type,
	                    language_reference_set, acceptabilityId,
	                    language_reference_set2, acceptabilityId2,
	                    language_reference_set3, acceptabilityId3,
	                    language_reference_set4, acceptabilityId4,
	                    language_reference_set5, acceptabilityId5,
	                    notes
	                );
			}   
        }
    }

    
    private String getCellAsString(Cell cell) {
	    if (cell == null) {
	        return "";
	    }
	    switch (cell.getCellType()) {
	        case STRING:
	            return cell.getStringCellValue();
	        case NUMERIC:
	            return String.valueOf(cell.getNumericCellValue());
	        case BOOLEAN:
	            return String.valueOf(cell.getBooleanCellValue());
	        case FORMULA:
	            return cell.getCellFormula();
	        default:
	            return "";
	    }
	}
}
