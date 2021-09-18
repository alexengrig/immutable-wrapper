package dev.alexengrig.util.immutablewrapper.inheritance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FatherDomainTest {
    @Test
    void test() {
        FatherDomain domain = new FatherDomain();
        domain.setGrandFatherInt(1);
        assertEquals(1, domain.getGrandFatherInt());
        domain.setFatherInt(2);
        assertEquals(2, domain.getFatherInt());
        FatherDomain immutable = new ImmutableFatherDomain(domain);
        assertThrows(UnsupportedOperationException.class, () -> immutable.setGrandFatherInt(10));
        assertEquals(1, immutable.getGrandFatherInt());
        assertThrows(UnsupportedOperationException.class, () -> immutable.setFatherInt(20));
        assertEquals(2, immutable.getFatherInt());
        domain.setGrandFatherInt(10);
        assertEquals(10, domain.getGrandFatherInt());
        assertEquals(10, immutable.getGrandFatherInt());
        domain.setFatherInt(20);
        assertEquals(20, domain.getFatherInt());
        assertEquals(20, immutable.getFatherInt());
    }
}
