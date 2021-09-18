package dev.alexengrig.util.immutablewrapper.inheritance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GrandFatherDomainTest {
    @Test
    void test() {
        GrandFatherDomain domain = new GrandFatherDomain();
        domain.setGrandFatherInt(1);
        assertEquals(1, domain.getGrandFatherInt());
        GrandFatherDomain immutable = new ImmutableGrandFatherDomain(domain);
        assertThrows(UnsupportedOperationException.class, () -> immutable.setGrandFatherInt(10));
        assertEquals(1, immutable.getGrandFatherInt());
        domain.setGrandFatherInt(10);
        assertEquals(10, domain.getGrandFatherInt());
        assertEquals(10, immutable.getGrandFatherInt());
    }
}