package lovexyn0827.mcversionstat.ani;

import java.util.Map;

public record DataItem(String name, Measurable<?> time, boolean emitsVertex, String tag, Map<String, Measurable<?>> columns) 
		implements Comparable<DataItem> {
	@Override
	public int compareTo(DataItem o) {
		return this.time.compareTo(o.time);
	}
}
