package dev.alexengrig.util.generator;

import dev.alexengrig.util.context.ImmutableWrapperContext;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class ImmutableWrapperSourceGenerator {

    public String generateSource(ImmutableWrapperContext context) {
        StringJoiner joiner = new StringJoiner("\n");
        context.getPackageName().ifPresent(packageName ->
                joiner.add("package " + packageName + ";"));
        String className = context.getTargetSimpleClassName();
        String parentClassName = context.getDomainClassName();
        joiner.add("public class " + className + " extends " + parentClassName + " {");
        joiner.add("    private final " + parentClassName + " target;");
        joiner.add("    public " + className + "(" + parentClassName + " target) {");
        joiner.add("        this.target = target;");
        joiner.add("    }");
        joiner.add("    // Immutable methods");
        for (ExecutableElement method : context.getImmutableMethods()) {
            joiner.add("    @java.lang.Override");
            String accessModifier = getAccessModifier(method)
                    .map(a -> a.concat(" "))
                    .orElse("");
            String returnType = getReturnType(method);
            String name = getMethodName(method);
            String parameters = getParameters(method);
            joiner.add("    " + accessModifier + returnType + " " + name + "(" + parameters + ") {");
            joiner.add("        throw new UnsupportedOperationException();");
            joiner.add("    }");
        }
        joiner.add("    // Other methods");
        for (ExecutableElement method : context.getOtherMethods()) {
            joiner.add("    @java.lang.Override");
            String accessModifier = getAccessModifier(method)
                    .map(a -> a.concat(" "))
                    .orElse("");
            String returnType = getReturnType(method);
            String name = getMethodName(method);
            String parameters = getParameters(method);
            joiner.add("    " + accessModifier + returnType + " " + name + "(" + parameters + ") {");
            String callPrefix = "void".equals(returnType) ? "" : "return ";
            String arguments = getArguments(method);
            joiner.add("        " + callPrefix + "target." + name + "(" + arguments + ");");
            joiner.add("    }");
        }
        joiner.add("}");
        return joiner.toString();
    }

    private Optional<String> getAccessModifier(ExecutableElement executableElement) {
        Set<Modifier> modifiers = executableElement.getModifiers();
        if (modifiers.contains(Modifier.PUBLIC)) {
            return Optional.of("public");
        } else if (modifiers.contains(Modifier.PROTECTED)) {
            return Optional.of("protected");
        }
        return Optional.empty();
    }

    private String getReturnType(ExecutableElement executableElement) {
        return executableElement.getReturnType().toString();
    }

    private String getMethodName(ExecutableElement executableElement) {
        return executableElement.getSimpleName().toString();
    }

    private String getParameters(ExecutableElement executableElement) {
        return executableElement.getParameters().stream()
                .map(p -> p.asType() + " " + p.getSimpleName())
                .collect(Collectors.joining(", "));
    }

    private String getArguments(ExecutableElement executableElement) {
        return executableElement.getParameters().stream()
                .map(VariableElement::getSimpleName)
                .collect(Collectors.joining(", "));
    }
}
