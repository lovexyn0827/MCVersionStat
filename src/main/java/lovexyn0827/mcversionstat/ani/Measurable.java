package lovexyn0827.mcversionstat.ani;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

public interface Measurable<T extends Number> extends Comparable<Measurable<?>> {
	T measure();
	String toString();
	String format(Number val);
	
	default double doubleVal() {
		return this.measure().doubleValue();
	}
	
	default int compareTo(Measurable<?> other) {
		long difference = this.measure().longValue() - other.measure().longValue();
		if (difference != 0) {
			return (int) difference;
		} else {
			return (int) (this.measure().doubleValue() - other.measure().doubleValue());
		}
	}
	
	public static Measurable<Long> of(Instant timestamp) {
		return new Measurable<>() {
			@Override
			public Long measure() {
				return timestamp.getEpochSecond();
			}
			
			@Override
			public String toString() {
				return timestamp.toString();
			}

			@Override
			public String format(Number val) {
				return Instant.ofEpochSecond(val.longValue()).toString();
			}
		};
	}

	public static Measurable<Long> of(Instant timestamp, DateTimeFormatter format) {
		return new Measurable<>() {
			@Override
			public Long measure() {
				return timestamp.getEpochSecond();
			}
			
			@Override
			public String toString() {
				return format.format(timestamp);
			}

			@Override
			public String format(Number val) {
				return format.format(Instant.ofEpochSecond(val.longValue()));
			}
		};
	}
	
	public static Measurable<Long> of(long tick) {
		return new Measurable<>() {
			@Override
			public Long measure() {
				return tick;
			}
			
			@Override
			public String toString() {
				return Long.toString(tick);
			}

			@Override
			public String format(Number val) {
				return val.toString();
			}
		};
	}
	
	public static Measurable<Long> of(long tick, Function<Long, String> formatter) {
		return new Measurable<>() {
			@Override
			public Long measure() {
				return tick;
			}
			
			@Override
			public String toString() {
				return formatter.apply(tick);
			}

			@Override
			public String format(Number val) {
				return formatter.apply(val.longValue());
			}
		};
	}
	
	public static Measurable<Double> of(double val) {
		return new Measurable<>() {
			@Override
			public Double measure() {
				return val;
			}
			
			@Override
			public String toString() {
				return Double.toString(val);
			}

			@Override
			public String format(Number val) {
				return val.toString();
			}
		};
	}
	
	public static Measurable<Double> of(double val, Function<Double, String> formatter) {
		return new Measurable<>() {
			@Override
			public Double measure() {
				return val;
			}
			
			@Override
			public String toString() {
				return formatter.apply(val);
			}

			@Override
			public String format(Number val) {
				return formatter.apply(val.doubleValue());
			}
		};
	}
	
	public static Measurable<Double> portionOf(Measurable<?> val, Measurable<?> unit) {
		double unitVal = unit.measure().doubleValue();
		double valD = val.measure().doubleValue();
		return new Measurable<>() {
			@Override
			public Double measure() {
				return valD / unitVal;
			}
			
			@Override
			public String toString() {
				return String.format("%.2f%%(%s)", this.measure() * 100, val);
			}

			@Override
			public String format(Number v) {
				return String.format("%.2f%%(%s)", v.doubleValue() * 100, val.format(v.doubleValue() * unitVal));
			}
		};
	}
	
	public static <T extends Number> Measurable<T> replaceValue(Measurable<?> original, T val) {
		return new Measurable<T>() {
			@Override
			public T measure() {
				return val;
			}

			@Override
			public String format(Number v) {
				return original.format(v);
			}
			
			@Override
			public String toString() {
				return this.format(val);
			}
		};
	}
}
