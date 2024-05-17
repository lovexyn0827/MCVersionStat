package lovexyn0827.mcversionstat;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import org.objectweb.asm.util.ASMifier;

import io.humble.video.Codec;
import io.humble.video.Encoder;
import io.humble.video.MediaPacket;
import io.humble.video.MediaPicture;
import io.humble.video.Muxer;
import io.humble.video.MuxerFormat;
import io.humble.video.PixelFormat;
import io.humble.video.Rational;
import io.humble.video.awt.MediaPictureConverter;
import io.humble.video.awt.MediaPictureConverterFactory;
import lovexyn0827.mcversionstat.ani.CsvLoader;
import lovexyn0827.mcversionstat.ani.LineChart;
import lovexyn0827.mcversionstat.ani.Measurable;

public class Main {
	public static void main(String[] args) throws MalformedURLException, IOException, InterruptedException {
		//new TaskDispatcher().run();
//		Arrays.stream(GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts())
//				.forEach(System.out::println);
		if (true) {
			//return;
		}
		
		final long start = 1_242_216_660L;
		final long frameDelta = 64800L;
		final long width = 86400 * 365;
		//ASMifier.main(new String[] { "C:/Users/<Guess What?>/eclipse-workspace/CodeLets/src/lovexyn0827/codelets/LambdaTest.class" });
		//new TaskDispatcher().run();
		Map<String, String> nameMapping = new HashMap<>();
		nameMapping.put("loc", "代码行数");
		nameMapping.put("methodImplements", "方法数");
		nameMapping.put("abstractClasses", "抽象类");
		nameMapping.put("interfaces", "接口");
		nameMapping.put("records", "Record");
		nameMapping.put("emums", "枚举");
		nameMapping.put("lambdas", "Lambda");
		nameMapping.put("fields", "字段");
		nameMapping.put("installationBytes", "总体积");
		nameMapping.put("jarBytes", "JAR体积");
		nameMapping.put("insns", "JVM指令数");
		nameMapping.put("classes", "类");
		Map<String, Function<Double, String>> formatters = new HashMap<>();
		nameMapping.keySet().forEach((k) -> formatters.put(k, (v) -> Long.toString(v.longValue())));
		formatters.put("installationBytes", (s) -> String.format("%.2fMB", s / 1048576));
		formatters.put("jarBytes", (s) -> String.format("%.2fMB", s / 1048576));
		formatters.put("insns", (s) -> String.format("%.2f万", s / 10000));
		LineChart chart = new LineChart(
				CsvLoader.loadCsv(new File("D:\\.minecraft\\versionStat1715166907611.csv"), 
						(v) -> v > -1E100, nameMapping, formatters));
		JFrame f = new JFrame();
		f.setSize(1920, 1080);
		BufferedImage im = new BufferedImage(1920, 1080, BufferedImage.TYPE_3BYTE_BGR);
		im.createGraphics().setColor(Color.white);
		im.getGraphics().fillRect(0, 0, 1920, 1080);
		f.setVisible(true);
		chart.drawLines(
				im, 128, 128, 1200, 800, Measurable.of(Instant.ofEpochSecond(1_500_000_000L)), 
				Measurable.of(Instant.ofEpochSecond(1_600_000_000L)), 
				Measurable.of(Instant.ofEpochSecond(1_560_000_000L)));
		//ImageIO.write(im, "PNG", new File("D:/.minecraft/" + System.currentTimeMillis() + ".PNG"));
		Muxer muxer = Muxer.make("D:/.minecraft/" + System.currentTimeMillis() + ".mp4", null, null);
		Codec fmt = Codec.findEncodingCodec(muxer.getFormat().getDefaultVideoCodecId());
		Encoder enc = Encoder.make(fmt);
		enc.setWidth(1920);
		enc.setHeight(1080);
		enc.setPixelFormat(PixelFormat.Type.PIX_FMT_YUV420P);
		Rational fps = Rational.make(1, 60);
		enc.setTimeBase(fps);
		if (muxer.getFormat().getFlag(MuxerFormat.Flag.GLOBAL_HEADER))
			enc.setFlag(Encoder.Flag.FLAG_GLOBAL_HEADER, true);
		enc.open(null, null);
		muxer.addNewStream(enc);
		muxer.open(null, null);
		MediaPictureConverter converter = null;
	    final MediaPicture picture = MediaPicture.make(
	    		enc.getWidth(),
	    		enc.getHeight(),
	    		PixelFormat.Type.PIX_FMT_YUV420P);
	    picture.setTimeBase(fps);
	    Graphics2D g = im.createGraphics();
		//g.setFont(Font.getFont("Mojangles"));
	    Font defaultFont = g.getFont();
	    final MediaPacket packet = MediaPacket.make();
	    for (int i = 90; i < 91; i++) {
	    	Instant stop = Instant.ofEpochSecond(start + i * frameDelta);
	    	Instant startTime = Instant.ofEpochSecond(Math.max(start, stop.getEpochSecond() - width));
	    	Instant end = Instant.ofEpochSecond(startTime.getEpochSecond() + width);
	    	stop = end = Instant.ofEpochSecond(1715774432);
	    	long span = (long) (width * Math.exp(i * 0.03));
	    	startTime = Instant.ofEpochSecond(1715774435 - span);
	    	startTime = Instant.ofEpochSecond(1242245460);
			try {
				chart.drawLines(
						im, 128, 128, 1200, 800, Measurable.of(startTime), 
						Measurable.of(end), 
						Measurable.of(stop));
			} catch (Exception e) {
				e.printStackTrace();
			}
		    g.setColor(Color.black);
		    g.setFont(defaultFont.deriveFont(AffineTransform.getScaleInstance(5, 5)));
	    	g.drawString(stop.toString().substring(0, 10), 128, 128);
	    	if (converter == null) {
	    		converter = MediaPictureConverterFactory.createConverter(im, picture);
	    	}
	    	
	    	converter.toPicture(picture, im, i);
	    	do {
	    		enc.encode(packet, picture);
	    		if (packet.isComplete())
	    			muxer.write(packet, false);
	    	} while (packet.isComplete());
			f.getGraphics().drawImage(im, 0, 0, f);
			ImageIO.write(im, "PNG", new File("D:/.minecraft/" + System.currentTimeMillis() + ".PNG"));
	    }

	    do {
	    	enc.encode(packet, null);
	    	if (packet.isComplete()) {
	    		muxer.write(packet,  false);
	    	}
	    } while (packet.isComplete());
	    muxer.close();
	}
}
