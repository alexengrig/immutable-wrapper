# immutable-wrapper

This is an annotation processor for generating POJO's immutable wrappers.

Unmodifiable wrapping:

```java
@ImmutableWrapper
class Domain {
    String string;
  // Constructors, getters and setters
}

void use(Domain domain) {
    Domain immutableDomain = new ImmutableDomain(domain);
    mutate(immutableDomain);
}

void mutate(Domain domain) {
    String oldString = domain.getString();
    domain.setString("I mutate it: " + oldString); // throws UnsupportedOperationException
}
```

TODO: Add snapshot and deep wrapping.
