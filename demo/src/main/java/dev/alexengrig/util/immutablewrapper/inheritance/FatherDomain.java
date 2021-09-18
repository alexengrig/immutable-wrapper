package dev.alexengrig.util.immutablewrapper.inheritance;

import dev.alexengrig.util.annotation.ImmutableWrapper;

@ImmutableWrapper
public class FatherDomain extends GrandFatherDomain {
    private int fatherInt;

    public int getFatherInt() {
        return fatherInt;
    }

    public void setFatherInt(int fatherInt) {
        this.fatherInt = fatherInt;
    }
}
