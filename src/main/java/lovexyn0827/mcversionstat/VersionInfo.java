package lovexyn0827.mcversionstat;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.JsonObject;

public record VersionInfo(String name, Instant release, int loc, int ordinalClasses, int methods, int indepClasses, 
		int methodImplements, int abstractClasses, int interfaces, int records, int emums, int anonymousClasses, 
		int lambdas, int fields, 
		int innerClasses, int staticMethods, int staticFields, int syncedMethods, int vioatileFields, 
		int installationBytes, int jarBytes, int insns) {
	public static final class Builder {
		public final Map<String, Set<Clazz>> classesByEnclosingClass = new HashMap<>();
		public String name;
		public Instant release;
		public JsonObject json;
		public int ordinaryClasses;
		public int abstractClasses;
		public int enumClasses;
		public int recordClasses;
		public int innerClasses;
		public int indepClasses;
		public int interfaces;
		public int anonymousClasses;
		public int abstractMethods;
		public int staticMethods;
		public int syncedMethods;
		public int methodsImplements;
		public int ordinaryFields;
		public int staticFields;
		public int vioatileFields;
		public int lambdas;
		public int installationBytes;
		public int jarBytes;
		public int insns;
		
		public VersionInfo build() {
			int loc, methods;
			loc = this.classesByEnclosingClass.values().stream()
					.mapToInt((s) -> {
						return s.stream().max(Comparator.comparing(Clazz::lines)).map(Clazz::lines).orElse(0);
					})
					.sum();
			AtomicInteger mCnt = new AtomicInteger(0);
			//this.classesByEnclosingClass.values().forEach();
			methods = this.methodsImplements;	// TODO
			return new VersionInfo(this.name, this.release, loc, this.ordinaryClasses, methods, this.indepClasses, 
					this.methodsImplements, this.abstractClasses, this.interfaces, this.recordClasses, 
					this.enumClasses, this.anonymousClasses, this.lambdas, this.ordinaryFields + this.staticFields, 
					this.innerClasses, this.staticMethods, this.staticFields, this.syncedMethods, 
					this.vioatileFields, this.installationBytes, this.jarBytes, this.insns);
		}
	}
}
