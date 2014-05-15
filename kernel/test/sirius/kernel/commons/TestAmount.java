package sirius.kernel.commons;

import org.junit.Test;
import sirius.kernel.Sirius;

import java.math.BigDecimal;

import static org.junit.Assert.*;

/**
 * Test for {@link Amount}
 *
 * @author Andreas Haufler (aha@scireum.de)
 */
public class TestAmount extends Sirius {
    @Test
    public void predicates() {
        Amount a = Amount.of(100);
        assertTrue(a.equals(Amount.ONE_HUNDRED));
        assertTrue(Amount.of((BigDecimal) null).isEmpty());
        assertTrue(Amount.of(10).isFilled());
        assertTrue(Amount.NOTHING.isZeroOrNull());
        assertTrue(Amount.ZERO.isZeroOrNull());
        assertTrue(Amount.ZERO.isZero());
        assertTrue(Amount.MINUS_ONE.isNonZero());
        assertTrue(Amount.MINUS_ONE.isNegative());
        assertFalse(Amount.MINUS_ONE.isPositive());
        assertTrue(Amount.TEN.compareTo(Amount.ONE) > 0);
    }

    @Test
    public void of() {
        assertEquals(Amount.ONE, Amount.of(1.0D));
        assertEquals(Amount.ONE, Amount.of(1L));
        assertEquals(Amount.ONE, Amount.of(Integer.valueOf(1)));
        assertEquals(Amount.ONE, Amount.of(Long.valueOf(1L)));
        assertEquals(Amount.ONE, Amount.of(Double.valueOf(1D)));
        assertEquals(Amount.ONE, Amount.ofMachineString("1.0"));
        assertEquals(Amount.ONE, Amount.ofUserString("1.0"));
        assertEquals(BigDecimal.ONE.setScale(Amount.SCALE), Amount.ofUserString("1.0").getAmount());
    }

    @Test
    public void computations() {
        assertEquals(Amount.ONE, Amount.ONE.add(Amount.NOTHING));
        assertEquals(Amount.NOTHING, Amount.NOTHING.add(Amount.ONE));
        assertEquals(Amount.NOTHING, Amount.NOTHING.add(Amount.NOTHING));
        assertEquals(Amount.of(2), Amount.ONE.add(Amount.ONE));
        assertEquals(Amount.MINUS_ONE, Amount.ONE.subtract(Amount.of(2)));
        assertEquals(Amount.ONE_HUNDRED, Amount.TEN.times(Amount.TEN));
        assertEquals(Amount.NOTHING, Amount.TEN.times(Amount.NOTHING));
        assertEquals(Amount.NOTHING, Amount.NOTHING.times(Amount.TEN));
        assertEquals(Amount.NOTHING, Amount.ONE.divideBy(Amount.ZERO));
        assertEquals(Amount.NOTHING, Amount.ONE.divideBy(Amount.NOTHING));
        assertEquals(Amount.NOTHING, Amount.NOTHING.divideBy(Amount.ONE));
        assertEquals(Amount.TEN, Amount.ONE_HUNDRED.divideBy(Amount.TEN));
        assertEquals(Amount.of(2), Amount.ONE.increasePercent(Amount.ONE_HUNDRED));
        assertEquals(Amount.of(0.5), Amount.ONE.decreasePercent(Amount.of(50)));
        assertEquals(Amount.of(19), Amount.TEN.multiplyPercent(Amount.TEN));
    }

    @Test
    public void misc() {
        assertEquals(Amount.TEN, Amount.NOTHING.fill(Amount.TEN));
        assertEquals(Amount.TEN, Amount.NOTHING.computeIfNull(() -> Amount.TEN));
        assertEquals(Amount.TEN, Amount.TEN.fill(Amount.ONE));
        assertEquals(Amount.TEN, Amount.TEN.percentageOf(Amount.ONE_HUNDRED));
        assertEquals(Amount.ONE_HUNDRED, Amount.TEN.percentageDifferenceOf(Amount.of(5)));
        assertEquals(Amount.of(-50), Amount.of(5).percentageDifferenceOf(Amount.TEN));
        assertEquals(Amount.of(0.5), Amount.of(50).asDecimal());
        assertEquals(Amount.ofMachineString("1.23"), Amount.ofMachineString("1.23223").round(NumberFormat.PERCENT));
        assertEquals(3, Amount.ONE_HUNDRED.getDigits());
        assertEquals(1, Amount.ONE.getDigits());
        assertEquals(2, Amount.ONE_HUNDRED.subtract(Amount.ONE).getDigits());
        assertEquals(3, Amount.of(477).getDigits());
    }

    @Test
    public void to() {
        assertEquals("10 %", Amount.ofMachineString("0.1").toPercent().toPercentString());
        assertEquals("1", Amount.of(1.23).toRoundedString());
        assertEquals("1", Amount.of(1.00).toSmartRoundedString(NumberFormat.TWO_DECIMAL_PLACES).asString());
        assertEquals("1.00", Amount.of(1.00).toString(NumberFormat.TWO_DECIMAL_PLACES).asString());
        assertEquals("1.23", Amount.of(1.23).toSmartRoundedString(NumberFormat.TWO_DECIMAL_PLACES).asString());
    }

}
