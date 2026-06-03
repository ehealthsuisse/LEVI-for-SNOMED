package translation.check;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileWriterUtil {

    private static final Logger logger = LoggerFactory.getLogger(FileWriterUtil.class);
	
	public void writeToFile(String filePath, List<List<String>> data) throws IOException {
		File file = new File(filePath);
		try (BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
			for (List<String> line : data) {
				writer.write(String.join("\t", line));
				writer.newLine();
			}
		}
		logger.info("File written: {} ({} lines)", filePath, data == null ? 0 : data.size());
	}
}