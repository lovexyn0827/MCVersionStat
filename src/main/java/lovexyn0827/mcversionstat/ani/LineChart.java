package lovexyn0827.mcversionstat.ani;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import lovexyn0827.mcversionstat.Pair;

public class LineChart {
	private final List<DataItem> data;
	private final Set<String> labels = new HashSet<>();
	private final BiFunction<String, Measurable<?>, Measurable<?>> interpolator;
	private Map<String, Color> colors;
	private List<YearMonth> timeLabels = new ArrayList<>();
	
	public LineChart(List<DataItem> data) {
		Collections.sort(data);
		this.data = data;
		data.stream().map(DataItem::columns).map(Map::keySet).forEach((s) -> s.forEach(this.labels::add));
		this.interpolator = null;// TODO generateInterpolation(data);
		int colCnt = this.labels.size();
		Map<String, Color> colors = new HashMap<>();
		Iterator<String> lItr = this.labels.iterator();
		for (int i = 0; i < colCnt; i++) {
			colors.put(lItr.next(), Color.getHSBColor((float) i / colCnt, 1, 1));
		}
		
		this.colors = colors;
		for (int i = 2009; i <= 2024; i++) {
			timeLabels.add(YearMonth.of(i, 1));
			timeLabels.add(YearMonth.of(i, 4));
			timeLabels.add(YearMonth.of(i, 7));
			timeLabels.add(YearMonth.of(i, 10));
		}
	}
	
	public void drawCurves(BufferedImage im, int x, int y, int w, int h, Measurable<?> start, Measurable<?> end) throws IOException {
		//FileWriter fw = new FileWriter(new File("D:/.minecraft/curve" + System.currentTimeMillis() + ".csv"));
		//fw.write(String.format("%d", null))
		Graphics2D g = im.createGraphics();
		g.setColor(Color.red);
		double scaleX = (end.measure().doubleValue() - start.measure().doubleValue()) / w;
		double startN = start.measure().doubleValue();
		Map<String, double[]> values = new HashMap<>();
		Map<String, Double> maxs = new HashMap<>();
		this.labels.forEach((l) -> maxs.put(l, 0.0));
		double max = 0;
		for (int i = 0; i < w; i++) {
			Measurable<Double> cur = Measurable.of(startN + scaleX * i);
			for (String label : this.labels) {
				double[] yByDx = values.computeIfAbsent(label, (k) -> new double[w]);
				Measurable<?> interpolated = this.interpolator.apply(label, cur);
				if (interpolated == null) {
					yByDx[i] = Double.NaN;
					continue;
				}
				
				double val = interpolated.measure().doubleValue();
				yByDx[i] = val;
				if (val != Double.NaN && val > max) {
					max = val;
				}
				
				if (val != Double.NaN && val > maxs.get(label)) {
					maxs.put(label, val);
				}
			}
		}

		g.setColor(Color.blue);
		double scaleY = h / max;
		for (int i = 1; i < w; i++) {
			for (String label : labels) {
				scaleY = h / maxs.get(label);
				double[] yByDx = values.get(label);
				if (!Double.isFinite(yByDx[i - 1]) || !Double.isFinite(yByDx[i])) {
					continue;
				}
				
				g.drawLine(x + i - 1, y + h - (int) (y + scaleY * yByDx[i - 1]), 
						x + i, y + h - (int) (y + scaleY * yByDx[i]));
			}
		}
	}
	
	public void drawLines(BufferedImage im, int x, int y, int w, int h, 
			Measurable<?> start, Measurable<?> end, Measurable<?> stop) {
		Map<String, List<Point>> verticesR = new HashMap<>();
		this.labels.forEach((l) -> verticesR.put(l, new ArrayList<>()));
		int limit = this.data.size();
		double startV = start.doubleVal();
		double endV = end.doubleVal();
		double stopV = stop.doubleVal();// TODO
		double scaleX = (double) w / (endV - startV);
		// Calculate vertices
		double max = Double.NEGATIVE_INFINITY;
		//int k = 0;
		Map<String, Measurable<?>> termainals = new HashMap<>();
		String lastRelease = "?";
		String lastRelClass = "?";
		int stopDX = w;
		for (int i = 0; i < limit; i++) {
			if (i > 0 && this.data.get(i - 1).time().compareTo(stop) > 0
					|| i < limit - 1 && this.data.get(i + 1).time().compareTo(start) < 0) {
				continue;
			}

			//k++;
			DataItem cur = this.data.get(i);
			Function<String, Measurable<?>> getter;
			int dx;
			int j = i;	// final local
			if (cur.time().compareTo(start) < 0) {
				dx = 0;
				getter = (l) -> {
					if (j + 1 >= this.data.size()) {
						return Measurable.of(0);
					}
					
					DataItem next = this.data.get(j + 1);
					double grad = (next.columns().get(l).doubleVal() - cur.columns().get(l).doubleVal()) 
							/ (next.time().doubleVal() - cur.time().doubleVal());
					int deltaTime = (int) (next.time().doubleVal() - startV);
					return Measurable.of(next.columns().get(l).doubleVal() - deltaTime * grad);
				};
			} else if (cur.time().compareTo(stop) > 0) {
				stopDX = dx = (int) ((w - 1) * (stopV - startV) / (endV - startV));
				getter = (l) -> {
					if (j < 1) {
						return Measurable.of(0);
					}

					DataItem prev = this.data.get(j - 1);
					double grad = (prev.columns().get(l).doubleVal() - cur.columns().get(l).doubleVal()) 
							/ (prev.time().doubleVal() - cur.time().doubleVal());
					int prevDX = (int) (scaleX * (prev.time().doubleVal() - startV));
					Measurable<?> yRaw = Measurable.replaceValue(prev.columns().get(l), 
							prev.columns().get(l).doubleVal() + (dx - prevDX) * grad / scaleX);
					termainals.put(l, yRaw);
					return yRaw;
				};
			} else {
				getter = (l) -> cur.columns().get(l);
				dx = (int) (scaleX * (cur.time().doubleVal() - startV));
				lastRelease = cur.name();
				lastRelClass = cur.tag();
			}
			
			for (String l : this.labels) {
				Measurable<?> yRaw = getter.apply(l);
				if (!this.shouldEmitVertex(cur, l, yRaw)) {
					continue;
				}
				verticesR.get(l).add(new Point(dx, yRaw.doubleVal()));
				if (yRaw.doubleVal() > max) {
					max = yRaw.doubleVal();
				}
			}
		}
		
		//System.out.println("LOC: " + verticesR.get("loc").size());
		//System.out.println("K: " + k);
		Graphics2D g = im.createGraphics();
		g.setFont(Font.getFont("Mojangles"));
		g.setFont(g.getFont().deriveFont(AffineTransform.getScaleInstance(2, 2)));
		g.setBackground(Color.gray);
		g.clearRect(0, 0, im.getWidth(), im.getHeight());
		double yScale = h / max;
		for (String label : this.labels) {
			g.setColor(this.colors.get(label));
			List<Point> vertices = verticesR.get(label);
			int limit2 = vertices.size() - 1;
			for (int i = 0; i < limit2; i++) {
				Point p1 = vertices.get(i);
				Point p2 = vertices.get(i + 1);
				if (p2.y() <= 0) {
					continue;
				}
				
				g.drawLine((int) (x + p1.x()), (int) (y + h - p1.y() * yScale), 
						(int) (x + p2.x()), (int) (y + h - p2.y() * yScale));
				g.drawLine((int) (x + p1.x()), (int) (y + h - p1.y() * yScale) + 1, 
						(int) (x + p2.x()), (int) (y + h - p2.y() * yScale) + 1);
			}
		}
		
		for (String label : this.labels) {
			Measurable<?> yPos = termainals.get(label);
			if (yPos == null || yPos.measure().doubleValue() <= 0) {
				continue;
			}
			
			g.setColor(this.colors.get(label));
			g.fillOval(x + stopDX - 8, (int) (y + h - yPos.doubleVal() * yScale) - 8, 16, 16);
			g.setColor(this.colors.get(label).darker().darker());
			g.drawOval(x + stopDX - 8, (int) (y + h - yPos.doubleVal() * yScale) - 8, 16, 16);
		}
		
		AtomicInteger lOrd = new AtomicInteger(0);
		//termainals.clear();
		this.data.get(this.data.size() - 1).columns().forEach((k, v) -> {
			termainals.put(k, v);
		});
		termainals.entrySet().stream()
				.sorted(Comparator.comparing((e) -> -e.getValue().doubleVal()))
				.forEach((e) -> {
					int lY = lOrd.getAndIncrement() * 32 + y;
					Measurable<?> yPos = e.getValue();
					if (yPos == null || yPos.measure().doubleValue() <= 0) {
						return;
					}

					g.setColor(this.colors.get(e.getKey()));
					g.drawString(e.getKey() + ": " + yPos, x + w + 40, lY);
				});

		g.setColor(Color.black);
		for (YearMonth t : this.timeLabels) {
			long epoch = t.atDay(1).atStartOfDay().toEpochSecond(ZoneOffset.UTC);
			if (epoch > end.doubleVal() || epoch < start.doubleVal()) {
				continue;
			}
			
			//g.drawString(t.toString().substring(0, 7), (int) (x + scaleX * (epoch - startV)), y + h + 40);
		}
		
		g.setFont(g.getFont().deriveFont(AffineTransform.getScaleInstance(3, 3)));
		g.drawString(lastRelease, 169, 191);
		String loc = termainals.get("代码行数") != null ? termainals.get("代码行数").toString() : "(?)";
		g.drawString(loc.substring(loc.indexOf('(') + 1, loc.lastIndexOf(')')) + "行代码", x + w + 100, y + h - 408);
		g.drawString(lastRelClass, x + w + 100, y + h - 311);
	}
	
	private boolean shouldEmitVertex(DataItem cur, String l, Measurable<?> yRaw) {
		return cur.emitsVertex() && (!l.contains("体积") || yRaw.doubleVal() > 0);
	}

	// Awful solution...
	// But it works...
	public void drawLinesOld(BufferedImage im, int x, int y, int w, int h, Measurable<?> start, Measurable<?> end) {
		List<DataItem> copy = new ArrayList<>();
		copy.addAll(this.data);
		copy.removeIf((item) -> {
			return item.time().compareTo(start) < 0 || item.time().compareTo(end) > 0;
		});
		double scaleX = (double) w / (end.measure().doubleValue() - start.measure().doubleValue());
		Map<String, Double> maxs = new HashMap<>();
		this.labels.forEach((l) -> maxs.put(l, 0.0));
		for (DataItem item : copy) {
			item.columns().forEach((l, v) -> {
				if (v.measure().doubleValue() > maxs.get(l)) {
					maxs.put(l, v.measure().doubleValue());
				}
			});
		}

		Graphics2D g = im.createGraphics();
		g.setBackground(Color.gray);
		g.clearRect(0, 0, im.getWidth(), im.getHeight());
		for (int i = 0; i < copy.size() - 1; i++) {
			Map<String, Measurable<?>> c1 = copy.get(i).columns();
			Map<String, Measurable<?>> c2 = copy.get(i + 1).columns();
			int x1 = (int) (scaleX * (copy.get(i).time().measure().doubleValue() 
					- start.measure().doubleValue()) + x);
			int x2 = (int) (scaleX * (copy.get(i + 1).time().measure().doubleValue() 
					- start.measure().doubleValue()) + x);
			for (String label : this.labels) {
				g.setColor(this.colors.get(label));
				double y1Raw = c1.getOrDefault(label, Measurable.of(Double.NaN)).measure().doubleValue();
				double y2Raw = c2.getOrDefault(label, Measurable.of(Double.NaN)).measure().doubleValue();
				if (y1Raw == Double.NaN || y2Raw == Double.NaN) {
					continue;
				}
				
				g.drawLine(x1, y + h - (int) (y1Raw * h / maxs.get(label)), 
						x2, y + h - (int) (y2Raw * h / maxs.get(label)));
			}
		}
	}
	
	private static BiFunction<String, Measurable<?>, Measurable<?>> generateInterpolation(List<DataItem> data) {
		Map<String, List<Pair<Measurable<?>, Measurable<?>>>> valueRef = new HashMap<>();
		Map<String, Pair<Measurable<?>, Double>> lastGrads = new HashMap<>();
		Map<String, List<Pair<Measurable<?>, Function<Measurable<?>, Measurable<?>>>>> segments = new HashMap<>();
		for (DataItem item : data) {
			for (Entry<String, Measurable<?>> col : item.columns().entrySet()) {
				List<Pair<Measurable<?>, Measurable<?>>> vals = 
						valueRef.computeIfAbsent(col.getKey(), (k) -> new ArrayList<>());
				vals.add(new Pair<>(item.time(), col.getValue()));
				if (vals.size() == 3) {
					Measurable<?> startTime = vals.get(0).a();
					Pair<Measurable<?>, Double> lastGrad = lastGrads.get(col.getKey());
					if (lastGrad == null) {
						lastGrad = new Pair<>(startTime, 0.0);
					}
					
					double[] xs = new double[4];
					double[] ys = new double[4];
					for (int i = 1; i < 3; i++) {
						Pair<Measurable<?>, Measurable<?>> pair = vals.get(i - 1);
						xs[i] = pair.a().measure().doubleValue();
						ys[i] = pair.b().measure().doubleValue();
					}
					
					xs[0] = lastGrad.a().measure().doubleValue();
					ys[0] = lastGrad.b();
					PolyD3 iFunc = PolyD3.from(xs, ys);
					Pair<Measurable<?>, Measurable<?>> last = vals.get(2);
					vals.clear();
					vals.add(last);
					lastGrads.put(col.getKey(), new Pair<>(last.a(), iFunc.dy(last.a().measure().doubleValue())));
					segments.computeIfAbsent(col.getKey(), (k) -> new ArrayList<>()).add(new Pair<>(startTime, iFunc));
					//System.out.printf("%f: %s\n", startTime.measure().doubleValue(), iFunc);
				}
			}
		}
		
		// TODO size % 3 != 0
		segments.values().forEach((l) -> {
			Collections.sort(l, Comparator.comparing(Pair::a));
		});
		AtomicInteger ai = new AtomicInteger(0);
		segments.get("loc").forEach((p) -> {
			System.out.printf("%d: %s\n", p.a().measure().longValue(), p.b());
			if (ai.getAndIncrement() <= 5) {
				//System.out.printf("如果(x>%d,(%s),", p.a().measure().longValue(), p.b());
				// GeoGebra crashed...
			}
		});

		for (int i = 0; i < 5; i++) {
			//System.out.print(")");
		}
		return (name, x) -> {
			List<Pair<Measurable<?>, Function<Measurable<?>, Measurable<?>>>> funcs = segments.get(name);
			if (funcs == null) {
				return null;
			}
			
			for (int i = 0; i < funcs.size(); i++) {
				if (funcs.get(i).a().compareTo(x) < 0) {
					//System.out.printf("%s < %s: true\n", funcs.get(i).a().measure(), x);
					return funcs.get(i).b().apply(x);
				} else {
					//System.out.printf("%s < %s: false\n", funcs.get(i).a().measure(), x);
				}
			}
			
			return null;
			/*int left = 0;
			int right = funcs.size() - 1;
			while (true) {
				int mid = (left + right) / 2;
				int delta = funcs.get(mid).a().compareTo(x);
				if (delta > 0) {
					right  = mid;
				} else if (delta < 0) {
					left = mid;
				} else {
					return funcs.get(mid).b().apply(x);
				}
				
				if (right - left <= 1) {
					return funcs.get(mid).b().apply(x);
				}
			}*/
		};
	}
	
	//public static void  main(String[] args) {
		//System.out.println(PolyD3.from(new double[] {1, 2, 3, 4}, new double[] {-1, 2, 3, 4}));
	//}
	
	public BufferedImage draw(Measurable<?> start, Measurable<?> end) {
		return null;
	}
	
	private static record PolyD3(double a, double b, double c, double d) 
			implements Function<Measurable<?>, Measurable<?>> {
		double y(double x) {
			return a * x * x * x + b * x * x + c * x + d;
		}
		
		double dy(double x) {
			return 3 * a * x * x + 2 * b * x + c;
		}
		
		public String toString() {
			return String.format("%sx^3+%sx^2+%sx+%s", this.a, this.b, this.c, this.d);
		}
		
		@Override
		public Measurable<?> apply(Measurable<?> t) {
			return Measurable.of(this.y(t.measure().doubleValue()));
		}
		
		/**
		 * @param xs x0 x1 x2 x3
		 * @param ys dy/dx|x0 y|x1 y|x2 y|x3
		 * @return
		 */
		static PolyD3 from(double[] xs, double[] ys) {
			double[][] matrix = new double[4][4];
			for (int i = 1; i <= 3; i++) {
				matrix[i][3] = 1;
				for (int j = 2; j >= 0; j--) {
					matrix[i][j] = matrix[i][j + 1] * xs[i];
				}
			}
			
			double x0 = xs[0];
			matrix[0][0] = 3 * x0 * x0;
			matrix[0][1] = 2 * x0;
			matrix[0][2] = 1;
			matrix[0][3] = 0;
			//double[] cofs = solveLinearEqus4(matrix);
			RealMatrix matrixObj = new Array2DRowRealMatrix(matrix, false);
			DecompositionSolver solver = new LUDecomposition(matrixObj).getSolver();
			double[] cofs = solver.solve(new ArrayRealVector(ys)).toArray();
			return new PolyD3(cofs[0], cofs[1], cofs[2], cofs[3]);
		}
		
		/*private static double[] solveLinearEqus4(double[][] matrix) {
			double[] solution = new double[4];
			double cDet = det4(matrix);
			double[][] buf = new double[4][5];
			for (int i = 0; i < 4; i++) {
				System.arraycopy(matrix, 0, buf, 0, 4);
				for (int j = 0; j < 4; j++) {
					buf[j][i] = buf[j][4];
				}
				
				solution[i] = det4(buf) / cDet;
			}
			
			return solution;
		}
		
		private static double det4(double[][] det) {
			return (1) * (det[0][3] + det[1][2] + det[2][1] + det[3][0])
					+ (-1) * (det[0][2] + det[1][3] + det[2][1] + det[3][0])
					+ (1) * (det[0][2] + det[1][1] + det[2][3] + det[3][0])
					+ (-1) * (det[0][2] + det[1][1] + det[2][0] + det[3][3])
					+ (-1) * (det[0][3] + det[1][1] + det[2][2] + det[3][0])
					+ (1) * (det[0][1] + det[1][3] + det[2][2] + det[3][0])
					+ (-1) * (det[0][1] + det[1][2] + det[2][3] + det[3][0])
					+ (1) * (det[0][1] + det[1][2] + det[2][0] + det[3][3])
					+ (1) * (det[0][3] + det[1][1] + det[2][0] + det[3][2])
					+ (-1) * (det[0][1] + det[1][3] + det[2][0] + det[3][2])
					+ (1) * (det[0][1] + det[1][0] + det[2][3] + det[3][2])
					+ (-1) * (det[0][1] + det[1][0] + det[2][2] + det[3][3])
					+ (-1) * (det[0][3] + det[1][2] + det[2][0] + det[3][1])
					+ (1) * (det[0][2] + det[1][3] + det[2][0] + det[3][1])
					+ (-1) * (det[0][2] + det[1][0] + det[2][3] + det[3][1])
					+ (1) * (det[0][2] + det[1][0] + det[2][1] + det[3][3])
					+ (1) * (det[0][3] + det[1][0] + det[2][2] + det[3][1])
					+ (-1) * (det[0][0] + det[1][3] + det[2][2] + det[3][1])
					+ (1) * (det[0][0] + det[1][2] + det[2][3] + det[3][1])
					+ (-1) * (det[0][0] + det[1][2] + det[2][1] + det[3][3])
					+ (-1) * (det[0][3] + det[1][0] + det[2][1] + det[3][2])
					+ (1) * (det[0][0] + det[1][3] + det[2][1] + det[3][2])
					+ (-1) * (det[0][0] + det[1][1] + det[2][3] + det[3][2])
					+ (1) * (det[0][0] + det[1][1] + det[2][2] + det[3][3]);
		}*/
	}
}
