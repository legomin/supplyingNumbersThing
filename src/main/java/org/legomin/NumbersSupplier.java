package org.legomin;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NumbersSupplier {
  private static final Logger log = LoggerFactory.getLogger(NumbersSupplier.class);

  private Avg avg;
  private Double max;
  private Double min;

  public void supply(final Number number) {
    try {
      final double doubleValue = number.doubleValue();
      calculateAvg(doubleValue);
      calculateMax(doubleValue);
      calculateMin(doubleValue);
    } catch (final NullPointerException e) {
      log.warn("Ignored illegal null input number parameter");
    } catch (final Exception e) {
      log.warn("Ignored illegal `Number` class implementation parameter");
    }
  }

  public Number getMax() {
    return max;
  }

  public Number getMin() {
    return min;
  }

  public Number getAvg() {
    return avg == null ? null : avg.getAvg();
  }

  private synchronized void calculateMax(final double doubleValue) {
    max = max == null ? doubleValue : Math.max(doubleValue, max);
  }

  private synchronized void calculateMin(final double doubleValue) {
    min = min == null ? doubleValue : Math.min(doubleValue, min);
  }

  private synchronized void calculateAvg(final double doubleValue) {
    avg = avg == null ? new Avg(BigDecimal.valueOf(doubleValue), 1L) : avg.add(doubleValue);
  }

  private static class Avg {
    private final BigDecimal sum;
    private final long count;

    public Avg(BigDecimal sum, long count) {
      this.sum = sum;
      this.count = count;
    }

    double getAvg() {
      final BigDecimal divisor = BigDecimal.valueOf(count == 0L ? 1L : count);
      try {
        return sum.divide(divisor).doubleValue();
      } catch (final ArithmeticException e) {
        return sum.divide(divisor, RoundingMode.HALF_UP).doubleValue();
      }
    }

    Avg add(final double doubleValue) {
      return new Avg(sum.add(BigDecimal.valueOf(doubleValue)), count + 1);
    }
  }

}
