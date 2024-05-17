package lovexyn0827.mcversionstat.ani;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.function.DoublePredicate;
import java.util.function.Function;

public class CsvLoader {
	public static List<DataItem> loadCsv(File file, DoublePredicate validator, 
			Map<String, String> nameMapping, Map<String, Function<Double, String>> formatters) {
		List<DataItem> out = new ArrayList<>();
		try (Scanner s = new Scanner(new FileReader(file))) {
			String[] header = s.nextLine().split(",");
			while (s.hasNextLine()) {
				String[] line = s.nextLine().split(",");
				String name = line[0];
				Measurable<?> time = Measurable.of(Instant.parse(line[1]));
				boolean emitsVertex = Integer.parseInt(line[2]) > 0;
				String tag = line[3];
				Map<String, Measurable<?>> columns = new HashMap<>();
//				System.out.println(new File("D:/.minecraft/" + name.replace("Classic ", "c").replace("Indev ", "in-").replace("Infdev ", "inf-").replace("Alpha ", "a").replace("Beta ", "b") + ".jar").length());
				for (int i = 4; i < line.length; i++) {
					double val = Double.parseDouble(line[i]);
					if (validator.test(val)) {
						columns.put(nameMapping.getOrDefault(header[i], header[i]), 
								formatters.containsKey(header[i]) ? 
										Measurable.of(val, formatters.get(header[i])) : Measurable.of(val));
					}
				}
				
				out.add(new DataItem(name, time, emitsVertex, tag, columns));
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		
		//out.forEach(System.out::println);
		Collections.sort(out);
		DataItem latest = out.get(out.size() - 1);
		out.forEach((o) -> o.columns().replaceAll((k, v) -> {
			return Measurable.portionOf(v, latest.columns().get(k));
		}));
		return out;
	}
}
