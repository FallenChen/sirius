package sirius.kernel.commons;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * Test for {@link DataCollector}
 *
 * @author Andreas Haufler (aha@scireum.de)
 */
public class TestCollector {
    @Test
    public void test() {
        DataCollector<String> c = DataCollector.create();
        c.add("A");
        c.add("B");
        c.add("C");
        assertEquals(Arrays.asList("A", "B", "C"), c.getData());
        c.getData().clear();
        assertEquals(Arrays.asList("A", "B", "C"), c.getData());
        c.addAll(c.getData());
        assertEquals(Arrays.asList("A", "B", "C", "A", "B", "C"), c.getData());
    }
}
