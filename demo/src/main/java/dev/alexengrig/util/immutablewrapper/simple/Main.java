package dev.alexengrig.util.immutablewrapper.simple;

public class Main {
    public static void main(String[] args) {
        SimpleDomain domain = new SimpleDomain();
        domain.setString("old");
        if (!"old".equals(domain.getString())) {
            throw new AssertionError();
        }
        ImmutableSimpleDomain immutableDomain = new ImmutableSimpleDomain(domain);
        immutableDomain.setString("new");
    }
}
