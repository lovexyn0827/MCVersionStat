package lovexyn0827.mcversionstat;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.objectweb.asm.ClassReader;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class TaskDispatcher {
	private static final byte[] CLASS_MAGIC = new byte[] {(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE};
	private static final AtomicInteger TASK_CNT = new AtomicInteger(0);
	private final ForkJoinPool pool = ForkJoinPool.commonPool();
	
	public void run() throws MalformedURLException, IOException {
		List<VersionInfo> versions = Collections.synchronizedList(new ArrayList<>());
		URLConnection conn = new URL("https://launchermeta.mojang.com/mc/game/version_manifest.json")
				.openConnection();
		JsonArray vers = JsonParser.parseString(new String(conn.getInputStream().readAllBytes()))
				.getAsJsonObject().getAsJsonArray("versions");
		//JsonArray vers = new JsonArray();	// Lazy
		for (String f : new File("D:/.minecraft").list()) {
			if (!f.endsWith(".jar")) {
				continue;
			}
			
			String id = f.substring(0, f.lastIndexOf('.'));
			JsonObject jo = new JsonObject();
			jo.addProperty("id", id);
			vers.add(jo);
		}
		
		for (JsonElement v : vers) {
			VersionInfo.Builder vInf = new VersionInfo.Builder();
			TASK_CNT.incrementAndGet();
			this.pool.execute(() -> {
				String name = v.getAsJsonObject().get("id").getAsString();
				Path jarPath = Paths.get("D:/.minecraft/" + name + ".jar");
				Path jsonPath = Paths.get("D:/.minecraft/" + name + ".json");
				vInf.name = name;
				try {
					if (!Files.exists(jsonPath) || Files.size(jsonPath) == 0) {
						URLConnection jsonConn = new URL(v.getAsJsonObject().get("url").getAsString())
								.openConnection();
						Files.copy(jsonConn.getInputStream(), jsonPath, StandardCopyOption.REPLACE_EXISTING);
					}
					
					JsonObject json = JsonParser.parseReader(new FileReader(jsonPath.toFile())).getAsJsonObject();
					vInf.json = json;
					vInf.release = Instant.parse(json.getAsJsonObject().get("releaseTime").getAsString());
					if (json.has("downloads") && json.getAsJsonObject("downloads").has("client")) {
						JsonObject clientDown = json.getAsJsonObject("downloads").getAsJsonObject("client");
						String jarUrl = clientDown.get("url").getAsString();
						// Ignoring for now ... String sha1 = clientDown.get("sha1").getAsString();
						int jarSize = clientDown.get("size").getAsInt();
						vInf.jarBytes = jarSize;
						
						// Calculate installation size
						vInf.installationBytes += jarSize;
						JsonObject assetsJson = json.getAsJsonObject("assetIndex");
						vInf.installationBytes += assetsJson.get("size").getAsInt();
						vInf.installationBytes += assetsJson.get("totalSize").getAsInt();
						checkLibLoop:
						for (JsonElement libElement : json.getAsJsonArray("libraries")) {
							JsonObject lib = libElement.getAsJsonObject();
							if (lib.has("rules")) {
								for (JsonElement rule : lib.getAsJsonArray("rules")) {
									if (rule.getAsJsonObject().has("os") 
											&& "allow".equals(rule.getAsJsonObject().get("action").getAsString())
											&& rule.getAsJsonObject().getAsJsonObject("os").get("name")
													.getAsString().equals("windows")) {
										continue checkLibLoop;
									}
								}
							}
							
							JsonObject libDown = lib.getAsJsonObject("downloads");
							if (libDown == null) {
								continue;
							}
							
							if (libDown.has("artifact")) {
								vInf.installationBytes += lib.getAsJsonObject("downloads")
										.getAsJsonObject("artifact").get("size").getAsInt();
							}
							
							if (libDown.has("classifiers")) {
								if (libDown.getAsJsonObject("classifiers").has("natives-windows")) {
									vInf.installationBytes += libDown.getAsJsonObject("classifiers")
											.getAsJsonObject("natives-windows").get("size").getAsInt();
								}
							}
						}
						
						if (json.has("logging")) {
							vInf.installationBytes += json.getAsJsonObject("logging")
									.getAsJsonObject("client").getAsJsonObject("file").get("size").getAsInt();
						}
						
						if (!(Files.exists(jarPath) && Files.size(jarPath) == jarSize || jarSize <= 0)) {
							System.out.println("Downloading " + name);
							Files.copy(new URL(jarUrl).openStream(), jarPath, StandardCopyOption.REPLACE_EXISTING);
						}
					} else {
						vInf.installationBytes = 0;
					}
					
					this.pool.execute(() -> {
						//if (true) return;	// Avoid OOM...
						System.out.println("Processing " + name);
						ZipFile file;
						try {
							file = new ZipFile(jarPath.toFile());
							ZipInputStream in = new ZipInputStream(new FileInputStream(jarPath.toFile()));
							while (in.available() > 0) {
								ZipEntry entry = in.getNextEntry();
								if (entry == null) continue;
								if(!entry.getName().endsWith(".class")) {
									continue;
								}
								
								InputStream classIn = file.getInputStream(entry);
								byte[] bytes = classIn.readAllBytes();
								classIn.close();
								if (!Arrays.equals(bytes, 0, 3, CLASS_MAGIC, 0, 3)) {
									continue;
								}
								
								ClassHandler ch = new ClassHandler(vInf);
								ClassReader cr = new ClassReader(bytes);
								cr.accept(ch, ClassReader.SKIP_FRAMES);
								Clazz cl = ch.clazzBuilder.build();
								vInf.classesByEnclosingClass
										.computeIfAbsent(cl.outerClazz() == null ? cl.name() : cl.outerClazz(), 
												(k) -> new HashSet<>())
										.add(cl);
							}
							
							file.close();
							in.close();
							TASK_CNT.decrementAndGet();
							versions.add(vInf.build());
						} catch (Exception e) {
							System.err.printf("Failed to process %s, due to %s\n", name, e);
							e.printStackTrace();
						}
					});
				} catch (Exception e) {
					System.err.printf("Failed to download %s, due to %s\n", name, e);
					e.printStackTrace();
				}
			});
		}
		
		while (TASK_CNT.get() != 0);
		Field[] fields = VersionInfo.class.getDeclaredFields();
		CsvWriter.Builder csvBuilder = new CsvWriter.Builder();
		Arrays.stream(fields).map(Field::getName).forEach(csvBuilder::addColumn);
		Collections.sort(versions, Comparator.comparing(VersionInfo::release));
		try (CsvWriter csv = csvBuilder.build(
				new FileWriter("D:/.minecraft/versionStat" + System.currentTimeMillis() + ".csv"))) {
			versions.forEach((v) -> {
				csv.println(Arrays.stream(fields).map((f) -> {
					try {
						f.setAccessible(true);
						return f.get(v);
					} catch (Exception e) {
						e.printStackTrace();
						return "ERROR";
					}
				}).toArray());
			});
		}
		System.out.print(versions);
	}
}
