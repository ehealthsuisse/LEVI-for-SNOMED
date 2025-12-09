package translation.check;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class FileWriterUtil {
	
	public void writeToFile(String filePath, List<List<String>> data) throws IOException {
		File file = new File(filePath);
		try (BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
			for (List<String> line : data) {
				writer.write(String.join("\t", line));
				writer.newLine();
			}
		}
	}
}
