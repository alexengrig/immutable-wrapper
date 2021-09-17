package dev.alexengrig.util.immutablewrapper.simple;

import dev.alexengrig.util.annotation.ImmutableWrapper;

@ImmutableWrapper
public class SimpleDomain {
    private String string;

    public static void main(String[] args) {
        SimpleDomain domain = new SimpleDomain();
        domain.setString("old");
        if (!"old".equals(domain.getString())) {
            throw new AssertionError();
        }
        SimpleDomain immutableDomain = new ImmutableSimpleDomain(domain);
        immutableDomain.setString("throw");
    }

    public String getString() {
        return string;
    }

    public void setString(String string) {
        this.string = string;
    }
}
