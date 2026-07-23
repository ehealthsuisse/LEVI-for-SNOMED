package ch.ehealth.levi.core.processor;

import java.io.IOException;

import com.opencsv.CSVReader;

public abstract class CsvProcessor {
	protected CSVReader csvReader;
	protected String languageCodeFilter;

    public CsvProcessor(CSVReader csvReader, String languageCodeFilter) {
        this.csvReader = csvReader;
        this.languageCodeFilter = languageCodeFilter;
    }

    public CsvProcessor(CSVReader csvReader) {
        this(csvReader, null);
    }

    public abstract void process() throws IOException;
}
