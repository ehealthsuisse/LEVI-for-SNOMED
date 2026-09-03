package ch.ehealth.levi.core.processor;

import org.apache.poi.ss.usermodel.Cell;

/**
 * Shared POI cell → String conversion used by the Excel loaders
 * ({@link DescriptionAdditionLoader}, {@link DescriptionInactivationLoader}).
 */
public final class ExcelCellUtil {

    private ExcelCellUtil() {
        // utility class
    }

    public static String getCellAsString(Cell cell) {
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
