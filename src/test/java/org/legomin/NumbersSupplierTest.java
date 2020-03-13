package org.legomin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Before;
import org.junit.Test;

public class NumbersSupplierTest {

  private NumbersSupplier supplier;

  @Before
  public void setUp() {
    supplier = new NumbersSupplier();
  }

  /**
   * tests that without supplying NULLs are expected as max, min & avg
   */
  @Test
  public void noSupplyInvocationsState() {
    assertNull("Invalid initial `max` value", supplier.getMax());
    assertNull("Invalid initial `min` value", supplier.getMin());
    assertNull("Invalid initial `avg` value", supplier.getAvg());
  }

  /**
   * tests that supplying NULL is ignored
   */
  @Test
  public void supplyNullParamIgnored() {
    supplier.supply(null);
    assertNull("Invalid initial `max` value", supplier.getMax());
    assertNull("Invalid initial `min` value", supplier.getMin());
    assertNull("Invalid initial `avg` value", supplier.getAvg());
    supplySomeNumbers();
    supplier.supply(null);
    assertEquals("Invalid initial `max` value", 4., supplier.getMax());
    assertEquals("Invalid initial `min` value", 1., supplier.getMin());
    assertEquals("Invalid initial `avg` value", 2.5, supplier.getAvg());
  }

  /**
   * tests that supplying malicious `Number` implementations are ignored
   */
  @Test
  public void supplyWrongNumberImplParamIgnored() {
    final Number wrongNumber = getSomeBadGuyNumber();

    supplier.supply(wrongNumber);
    assertNull("Invalid initial `max` value", supplier.getMax());
    assertNull("Invalid initial `min` value", supplier.getMin());
    assertNull("Invalid initial `avg` value", supplier.getAvg());

    supplySomeNumbers();
    supplier.supply(wrongNumber);
    assertEquals("Invalid initial `max` value", 4., supplier.getMax());
    assertEquals("Invalid initial `min` value", 1., supplier.getMin());
    assertEquals("Invalid initial `avg` value", 2.5, supplier.getAvg());
  }

  private void supplySomeNumbers() {
    supplier.supply(1);
    supplier.supply(2);
    supplier.supply(3);
    supplier.supply(4);
  }

  private Number getSomeBadGuyNumber() {
    return new Number() { // That part could be concised by using f.e. Mockito mock ;-)

      @Override
      public int intValue() {
        return 0;
      }

      @Override
      public long longValue() {
        return 0;
      }

      @Override
      public float floatValue() {
        return 0;
      }

      @Override
      public double doubleValue() {
        throw new RuntimeException("I am the very bad guy!");
      }
    };
  }

  /**
   * tests that supplying of many big numbers doesn't break anything
   */
  @Test
  public void reallyBigNumbersSupply() {
    for (int i=0; i < 10_000; i++) {
      supplier.supply(Long.MAX_VALUE);
    }
    for (int i=0; i < 10_000; i++) {
      supplier.supply(Double.MAX_VALUE);
    }
    assertEquals("Invalid initial `max` value", Double.MAX_VALUE, supplier.getMax());
    assertEquals("Invalid initial `min` value", (double) Long.MAX_VALUE, supplier.getMin());
    assertEquals("Invalid initial `avg` value", (Double.MAX_VALUE + Long.MAX_VALUE) / 2, supplier.getAvg());
  }

  /**
   * tests on thread safe
   * there are 100 threads, each invokes supplier 1000 times
   */
  @Test
  public void multiThreadTest() {
    final ExecutorService executor = Executors.newFixedThreadPool(100);
    final CompletionService<TestSingleThreadSupplierResult> completionService = new ExecutorCompletionService<>(executor);
    for (int i = 0; i < 100; i++) {
      completionService.submit(new TestNumbersSupplierClient(supplier));
    }
    final TestSingleThreadSupplierResult expected = new TestSingleThreadSupplierResult();
    try {
      for (int i = 0; i < 100; i++) {
        final TestSingleThreadSupplierResult oneThreadResult = completionService.take().get();
        expected.merge(oneThreadResult.count, oneThreadResult.sum, oneThreadResult.max, oneThreadResult.min);
      }
      assertEquals("Unexpected `max` value", expected.max, supplier.getMax());
      assertEquals("Unexpected `min` value", expected.min, supplier.getMin());
      assertEquals("Unexpected `avg` value", expected.sum.doubleValue() / expected.count, supplier.getAvg());
    } catch (final InterruptedException | ExecutionException e) {
      fail(e.getMessage());
      Thread.currentThread().interrupt();
    }
  }

  private static class TestSingleThreadSupplierResult {
    private BigDecimal sum = BigDecimal.ZERO;
    private long count = 0L;
    private Double max = null;
    private Double min = null;

    public void merge(final long count, final BigDecimal sum, double max, double min) {
      this.count += count;
      this.sum = this.sum.add(sum);
      this.max = this.max == null ? max : Math.max(this.max, max);
      this.min = this.min == null ? min : Math.min(this.min, min);
    }

    public void add(final double value) {
      merge(1L, BigDecimal.valueOf(value), value, value);
    }
  }

  private static class TestNumbersSupplierClient implements Callable<TestSingleThreadSupplierResult> {
    private final Random random;
    private final NumbersSupplier supplier;
    private final double multiplier;

    public TestNumbersSupplierClient(NumbersSupplier supplier) {
      this.supplier = supplier;
      this.random = new Random();
      this.multiplier = random.nextDouble() * 1_000_000.;
    }

    @Override
    public TestSingleThreadSupplierResult call() {
      final TestSingleThreadSupplierResult result = new TestSingleThreadSupplierResult();
      for (int i = 0; i < 1000; i++) {
        final double nextNumber = random.nextDouble() * multiplier;
        result.add(nextNumber);
        supplier.supply(nextNumber);
      }
      return result;
    }
  }

}
