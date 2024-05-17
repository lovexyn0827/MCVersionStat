package lovexyn0827.mcversionstat;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public record Clazz(String name, String outerClazz, boolean annoymous, String superClazz, String[] interfaces, 
		int lines, Map<String, Set<String>> methodsByName) {
	public static class Builder {
		String name;
		String outerClazz;
		String superClazz;
		String[] interfaces;
		boolean annoymous = false;
		int lines;
		Map<String, Set<String>> methodsByName = new HashMap<>();
		
		public Clazz build() {
			return new Clazz(this.name, this.outerClazz, this.annoymous, this.superClazz, this.interfaces, 
					this.lines, this.methodsByName);
		}
	}
}
