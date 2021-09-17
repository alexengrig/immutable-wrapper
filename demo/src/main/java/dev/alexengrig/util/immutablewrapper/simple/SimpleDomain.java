package dev.alexengrig.util.immutablewrapper.simple;

import dev.alexengrig.util.annotation.ImmutableWrapper;

@ImmutableWrapper
public class SimpleDomain {
    private String string;

    public String getString() {
        return string;
    }

    public void setString(String string) {
        this.string = string;
    }
}
