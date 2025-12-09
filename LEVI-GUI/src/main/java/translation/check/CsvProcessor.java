package translation.check;

import java.io.IOException;

import com.opencsv.CSVReader;

public abstract class CsvProcessor {
	protected CSVReader csvReader;

    public CsvProcessor(CSVReader csvReader) {
        this.csvReader = csvReader;
    }

    public abstract void process() throws IOException;
}
