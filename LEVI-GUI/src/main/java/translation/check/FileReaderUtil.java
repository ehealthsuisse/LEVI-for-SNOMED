package translation.check;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

public class FileReaderUtil {

	private ResultCollector resultCollector;

	public FileReaderUtil(ResultCollector collector) {
		this.resultCollector = collector;
	}

	/**
	 * Reads a CSV file and populates the `Compare` class with the extracted data.
	 *
	 * @param csvFile The file path to the CSV file.
	 * @throws IOException If an error occurs during file reading.
	 */
	public void readFile(String filePath) throws IOException {

		// Determine file type and delimiter
		Object[] fileInfo = checkFilePathExtension(filePath);
		String fileType = (String) fileInfo[0];
		char fileseparator = 0;
		if(fileInfo[1] != null) {
			fileseparator = (char) fileInfo[1];
		}
		String releaseType = (String) fileInfo[2];

		if ("Excel".equals(fileType)) {
			// Read Excel file using Apache POI
			try (FileInputStream fis = new FileInputStream(filePath);
					Workbook workbook = filePath.endsWith(".xlsx") ? new XSSFWorkbook(fis) : new HSSFWorkbook(fis)) {

				System.out.println("Processing Excel file: " + filePath);
				processExcel(workbook, releaseType);

			} catch (IOException e) {
				System.err.println("Error reading Excel file.");
				e.printStackTrace();
			}

		} else if ("JSON".equals(fileType)) {
            // Read JSON file using ValueSetProcessor
            System.out.println("Processing FHIR JSON file: " + filePath);
            try (FileReader fileReader = new FileReader(filePath)) {
                StringBuilder jsonContent = new StringBuilder();
                int ch;
                while ((ch = fileReader.read()) != -1) {
                    jsonContent.append((char) ch);
                }

                FhirJsonValueSetProcessor processor = new FhirJsonValueSetProcessor(resultCollector);
                processor.processValueSet(jsonContent.toString());
            } catch (IOException e) {
                System.err.println("Error reading JSON file.");
                e.printStackTrace();
            }

        } else { // TODO: need solution for separator for CSV/TSV files
			// Read CSV/TSV file using OpenCSV
			CSVParser parser = new CSVParserBuilder().withSeparator(fileseparator).withQuoteChar('"').withEscapeChar('\\')
					.withStrictQuotes(false).build();

			try (CSVReader csvReader = new CSVReaderBuilder(new FileReader(filePath)).withCSVParser(parser).build()) {

				switch (fileType) {
				case ".propcsv.csv":
					new PropCsvProcessor(csvReader, resultCollector).process();
					break;
				case ".termspace.csv":
					new SimpleOverview(csvReader, resultCollector).process(); //TODO: Implement Termspace CSV processing
					break;
				case "Additions.tsv":
					new SiAdditionsCsvProcessor(csvReader, resultCollector).process();
					break;
				case "Inactivations.tsv":
					System.out.println("Processing Termspace `Inactivations.tsv` file...");
					new TermspaceInactivationsCsvProcessor(csvReader, resultCollector).process();
					break;
				case ".simpleOverview.tsv":
					System.out.println("Processing `.simpleOverview.tsv` file...");
					new SimpleOverview(csvReader, resultCollector).process();
					break;
//				case ".txt": TODO: Is this needed?
//					System.out.println("Processing RF2 file...");
//					processDescriptionRF2File(csvReader);
//					break;
 				default:
					System.out.println(fileType + "Not recognized. Please check the file type.");
					break;
				}



			} catch (IOException e) {
				e.printStackTrace();
			} catch (NoSuchElementException e) {
				System.err.println("The CSV is malformed or has syntax issues.");
				System.err.println("Please delete all columns after 'Target acceptable' in the CSV and try again.");
			}
		}
	}
	

	/**
	 * Identifies the file type based on its extension.
	 *
	 * @param filePath The path of the file.
	 * @return The identified file type or "Unknown file type" if no match is found.
	 */
	public static Object[] checkFilePathExtension(String filePath) {
		Map<String, Character> fileExtensions = new HashMap<>();

		// Define known file extensions and their corresponding delimiters
		fileExtensions.put(".propcsv.csv", ';');
		fileExtensions.put(".termspace.csv", '\t');
		fileExtensions.put("Additions.tsv", '\t');
		fileExtensions.put("Inactivations.tsv", '\t');
		fileExtensions.put("Check_inactivation.tsv", '\t');
		fileExtensions.put(".txt", '\t');
		fileExtensions.put(".simpleOverview.tsv", '\t');


		String lowerCasePath = filePath.toLowerCase();

		// Determine release type
		String releaseType = null;
		if (lowerCasePath.contains("previous")) {
			releaseType = "previous";
		} else {
			releaseType = "current";
		}

		// Check if the file path ends with a known extension
		for (Map.Entry<String, Character> entry : fileExtensions.entrySet()) {
			if (filePath.endsWith(entry.getKey())) {
				return new Object[] { entry.getKey(), entry.getValue(), releaseType };
			}
		}

		// Check for Excel files (no delimiter needed)
		if (filePath.endsWith(".xlsx") || filePath.endsWith(".xls")) {
			return new Object[] { "Excel", null, releaseType };
		}
		
		// Check for JSON files
        if (filePath.endsWith(".json")) {
            return new Object[]{"JSON", null, releaseType};
        }

		// Return default for unknown file types
		return new Object[] { "Unknown file type", '\t', releaseType };
	}

	private void processExcel(Workbook workbook, String releaseType) throws IOException {
		System.out.println("Processing additions via DescriptionAdditionLoader...");
		int numberOfSheets = workbook.getNumberOfSheets();
		for (int i = 0; i < numberOfSheets; i++) {
			Sheet sheet = workbook.getSheetAt(i);
			String sheetName = sheet.getSheetName();
			switch (sheetName) {

			case "Description Additions":
				System.out.println("Starting processing description additions");
				new DescriptionAdditionLoader().loadAndInsertExcel(sheet, resultCollector, releaseType);

				break;

			case "Description Inactivations":
				System.out.println("Starting processing description inactivations");
				new DescriptionInactivationLoader().loadAndInsertExcel(sheet, resultCollector, releaseType);

				break;

			default:
				// Do nothing for unrecognized sheets
				break;
			}
		}
	}
}
