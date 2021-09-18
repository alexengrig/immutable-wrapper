package dev.alexengrig.util.immutablewrapper.inheritance;

import dev.alexengrig.util.annotation.ImmutableWrapper;

@ImmutableWrapper
public class GrandFatherDomain {
    private int grandFatherInt;

    public int getGrandFatherInt() {
        return grandFatherInt;
    }

    public void setGrandFatherInt(int grandFatherInt) {
        this.grandFatherInt = grandFatherInt;
    }
}
