package dev.alexengrig.util.context;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ImmutableWrapperContext {
    private static final String WRAPPER_TYPE_PREFIX = "Immutable";
    private static final String JAVA_OBJECT_TYPE_NAME = Object.class.getName();

    private final ProcessingEnvironment environment;
    private final TypeElement domainTypeElement;

    private transient String domainSimpleClassName;
    private transient String targetSimpleClassName;

    private transient String domainClassName;
    private transient String targetClassName;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private transient Optional<String> packageNameOptional;

    private transient Collection<ExecutableElement> allMethods;
    private transient Set<ExecutableElement> immutableMethods;

    public ImmutableWrapperContext(ProcessingEnvironment environment, TypeElement domainTypeElement) {
        this.environment = environment;
        this.domainTypeElement = domainTypeElement;
    }

    public String getDomainSimpleClassName() {
        if (domainSimpleClassName == null) {
            domainSimpleClassName = domainTypeElement.getSimpleName().toString();
        }
        return domainSimpleClassName;
    }

    public String getTargetSimpleClassName() {
        if (targetSimpleClassName == null) {
            targetSimpleClassName = WRAPPER_TYPE_PREFIX.concat(getDomainSimpleClassName());
        }
        return targetSimpleClassName;
    }

    public String getDomainClassName() {
        if (domainClassName == null) {
            domainClassName = domainTypeElement.getQualifiedName().toString();
        }
        return domainClassName;
    }

    public String getTargetClassName() {
        if (targetClassName == null) {
            String className = getTargetSimpleClassName();
            Optional<String> packageNameOptional = getPackageName();
            targetClassName = packageNameOptional
                    .map(packageName -> packageName.concat(".").concat(className))
                    .orElse(className);
        }
        return targetClassName;
    }

    @SuppressWarnings("OptionalAssignedToNull")
    public Optional<String> getPackageName() {
        if (packageNameOptional == null) {
            String qualifiedName = getDomainClassName();
            int lastIndexOfDot = qualifiedName.lastIndexOf('.');
            if (lastIndexOfDot == -1) {
                packageNameOptional = Optional.empty();
            } else {
                packageNameOptional = Optional.of(qualifiedName.substring(0, lastIndexOfDot));
            }
        }
        return packageNameOptional;
    }

    public Collection<ExecutableElement> getImmutableMethods() {
        if (immutableMethods == null) {
            immutableMethods = getAllMethods().stream()
                    .filter(m -> m.getSimpleName().toString().startsWith("set"))
                    .collect(Collectors.toSet());
        }
        return immutableMethods;
    }

    public Collection<ExecutableElement> getOtherMethods() {
        return getAllMethods().stream()
                .filter(m -> !immutableMethods.contains(m))
                .collect(Collectors.toList());
    }

    private Collection<ExecutableElement> getAllMethods() {
        if (allMethods == null) {
            Map<String, ExecutableElement> methods = getMethods(domainTypeElement)
                    .collect(Collectors.toMap(this::getKey, Function.identity()));
            Optional<TypeElement> parent = getParent(domainTypeElement);
            if (parent.isPresent()) {
                for (TypeElement element = parent.get(); element != null; element = getParent(element).orElse(null)) {
                    getMethods(element)
                            .filter(executableElement -> !methods.containsKey(getKey(executableElement)))
                            .forEach(executableElement -> methods.put(getKey(executableElement), executableElement));
                }
            }
            allMethods = methods.values();
        }
        return allMethods;
    }

    private Stream<ExecutableElement> getMethods(TypeElement typeElement) {
        return typeElement.getEnclosedElements().stream()
                .filter(e -> ElementKind.METHOD.equals(e.getKind()))
                .map(ExecutableElement.class::cast)
                .filter(m -> !m.getModifiers().contains(Modifier.STATIC))
                .filter(m -> !m.getModifiers().contains(Modifier.PRIVATE));
    }

    private Optional<TypeElement> getParent(TypeElement typeElement) {
        return environment.getTypeUtils().directSupertypes(typeElement.asType()).stream()
                .limit(1) // parent class is first
                .map(DeclaredType.class::cast)
                .map(DeclaredType::asElement)
                .filter(element -> element.getKind().isClass())
                .filter(element -> !JAVA_OBJECT_TYPE_NAME.equals(element.toString()))
                .map(TypeElement.class::cast)
                .findFirst();
    }

    private String getKey(ExecutableElement m) {
        return m.toString();
    }
}
