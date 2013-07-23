package sirius.kernel.commons;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test for {@link Value}
 *
 * @author Andreas Haufler (aha@scireum.de)
 */
public class TestValue {
    @Test
    public void isFilled() {
        assertTrue(Value.of(1).isFilled());
        assertTrue(Value.of(" ").isFilled());
        assertFalse(Value.of("").isFilled());
        assertFalse(Value.of(null).isFilled());
    }
}
