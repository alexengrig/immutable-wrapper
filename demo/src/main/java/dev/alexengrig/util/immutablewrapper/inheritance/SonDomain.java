package dev.alexengrig.util.immutablewrapper.inheritance;

import dev.alexengrig.util.annotation.ImmutableWrapper;

@ImmutableWrapper
public class SonDomain extends FatherDomain {
    private int sonInt;

    public int getSonInt() {
        return sonInt;
    }

    public void setSonInt(int sonInt) {
        this.sonInt = sonInt;
    }
}
