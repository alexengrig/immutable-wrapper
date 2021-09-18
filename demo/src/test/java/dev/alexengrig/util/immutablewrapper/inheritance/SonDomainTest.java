package dev.alexengrig.util.immutablewrapper.inheritance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SonDomainTest {
    @Test
    void test() {
        SonDomain domain = new SonDomain();
        domain.setGrandFatherInt(1);
        assertEquals(1, domain.getGrandFatherInt());
        domain.setFatherInt(2);
        assertEquals(2, domain.getFatherInt());
        domain.setSonInt(3);
        assertEquals(3, domain.getSonInt());
        SonDomain immutable = new ImmutableSonDomain(domain);
        assertThrows(UnsupportedOperationException.class, () -> immutable.setGrandFatherInt(10));
        assertEquals(1, immutable.getGrandFatherInt());
        assertThrows(UnsupportedOperationException.class, () -> immutable.setFatherInt(20));
        assertEquals(2, immutable.getFatherInt());
        assertThrows(UnsupportedOperationException.class, () -> immutable.setSonInt(30));
        assertEquals(3, immutable.getSonInt());
        domain.setGrandFatherInt(10);
        assertEquals(10, domain.getGrandFatherInt());
        assertEquals(10, immutable.getGrandFatherInt());
        domain.setFatherInt(20);
        assertEquals(20, domain.getFatherInt());
        assertEquals(20, immutable.getFatherInt());
        domain.setSonInt(30);
        assertEquals(30, domain.getSonInt());
        assertEquals(30, immutable.getSonInt());
    }
}
