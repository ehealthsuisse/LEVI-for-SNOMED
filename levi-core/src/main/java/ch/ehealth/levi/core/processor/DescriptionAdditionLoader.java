package ch.ehealth.levi.core.processor;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import ch.ehealth.levi.core.export.ResultCollector;

public class DescriptionAdditionLoader {
	
	//TODO: Add check if the file is empty or has no rows


	public void loadAndInsertExcel(Sheet sheet, ResultCollector collector, String releaseType) {
        int rowCount = sheet.getPhysicalNumberOfRows();
        for (int i = 1; i < rowCount; i++) { // skip header
            Row row = sheet.getRow(i);
            if (row == null) continue;

            String conceptId = ExcelCellUtil.getCellAsString(row.getCell(0));
            String fsn = ExcelCellUtil.getCellAsString(row.getCell(1));
            String pt = ExcelCellUtil.getCellAsString(row.getCell(2));
            String term = ExcelCellUtil.getCellAsString(row.getCell(3));
            String languageCode = ExcelCellUtil.getCellAsString(row.getCell(4));
            String caseSignificance = ExcelCellUtil.getCellAsString(row.getCell(5));
            String type = ExcelCellUtil.getCellAsString(row.getCell(6));
            String language_reference_set = ExcelCellUtil.getCellAsString(row.getCell(7));
			String acceptabilityId = ExcelCellUtil.getCellAsString(row.getCell(8));
			String language_reference_set2 = ExcelCellUtil.getCellAsString(row.getCell(9));
			String acceptabilityId2 = ExcelCellUtil.getCellAsString(row.getCell(10));
			String language_reference_set3 = ExcelCellUtil.getCellAsString(row.getCell(11));
			String acceptabilityId3 = ExcelCellUtil.getCellAsString(row.getCell(12));
			String language_reference_set4 = ExcelCellUtil.getCellAsString(row.getCell(13));
			String acceptabilityId4 = ExcelCellUtil.getCellAsString(row.getCell(14));
			String language_reference_set5 = ExcelCellUtil.getCellAsString(row.getCell(15));
			String acceptabilityId5 = ExcelCellUtil.getCellAsString(row.getCell(16));
            String notes = ExcelCellUtil.getCellAsString(row.getCell(17));

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

}
