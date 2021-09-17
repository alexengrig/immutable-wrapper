package dev.alexengrig.util.processor;

import com.google.auto.service.AutoService;
import dev.alexengrig.util.annotation.ImmutableWrapper;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Completion;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AutoService(Processor.class)
public class ImmutableWrapperProcessor extends AbstractProcessor {

    private static final Class<ImmutableWrapper> ANNOTATION_TYPE = ImmutableWrapper.class;
    private static final String WRAPPER_TYPE_PREFIX = "Immutable";
    private static final String JAVA_OBJECT_TYPE_NAME = Object.class.getName();

    // Processor

    @Override
    public Set<String> getSupportedOptions() {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(ANNOTATION_TYPE.getName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_8;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return false;
        }
        for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(ANNOTATION_TYPE)) {
            TypeElement typeElement = (TypeElement) annotatedElement;
            doProcess(typeElement);
        }
        return true;
    }

    // Implementation

    private void doProcess(TypeElement typeElement) {
        Context context = new Context(typeElement);
        String source = createSource(context);
        JavaFileObject file = createFile(context);
        writeSourceToFile(source, file);
    }

    private String createSource(Context context) {
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
        for (ExecutableElement method : context.getAllMethods()) {
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
            joiner.add("        " + callPrefix + "super." + name + "(" + arguments + ");");
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

    @Override
    public Iterable<? extends Completion> getCompletions(Element element, AnnotationMirror annotation, ExecutableElement member, String userText) {
        return Collections.emptySet();
    }

    private JavaFileObject createFile(Context context) {
        String filename = context.getTargetClassName();
        try {
            return processingEnv.getFiler().createSourceFile(filename);
        } catch (IOException e) {
            throw new IllegalArgumentException("Fail on creating file: " + filename, e);
        }
    }

    private void writeSourceToFile(String source, JavaFileObject file) {
        try (PrintWriter sourcePrinter = new PrintWriter(file.openWriter())) {
            sourcePrinter.print(source);
        } catch (IOException e) {
            throw new RuntimeException("Fail on writing file", e);
        }
    }

    private class Context {
        private final TypeElement domainTypeElement;

        private transient String domainSimpleClassName;
        private transient String targetSimpleClassName;

        private transient String domainClassName;
        private transient String targetClassName;

        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        private transient Optional<String> packageNameOptional;

        private Context(TypeElement domainTypeElement) {
            this.domainTypeElement = domainTypeElement;
        }

        String getDomainSimpleClassName() {
            if (domainSimpleClassName == null) {
                domainSimpleClassName = domainTypeElement.getSimpleName().toString();
            }
            return domainSimpleClassName;
        }

        String getTargetSimpleClassName() {
            if (targetSimpleClassName == null) {
                targetSimpleClassName = WRAPPER_TYPE_PREFIX.concat(getDomainSimpleClassName());
            }
            return targetSimpleClassName;
        }

        String getDomainClassName() {
            if (domainClassName == null) {
                domainClassName = domainTypeElement.getQualifiedName().toString();
            }
            return domainClassName;
        }

        String getTargetClassName() {
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
        Optional<String> getPackageName() {
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

        Collection<ExecutableElement> getAllMethods() {
            Map<String, ExecutableElement> methods = getMethods(domainTypeElement)
                    .collect(Collectors.toMap(this::getKey, Function.identity()));
            Optional<TypeElement> parent = getParent(domainTypeElement);
            if (!parent.isPresent()) {
                return methods.values();
            }
            for (TypeElement element = parent.get(); element != null; element = getParent(element).orElse(null)) {
                getMethods(element)
                        .filter(executableElement -> !methods.containsKey(getKey(executableElement)))
                        .forEach(executableElement -> methods.put(getKey(executableElement), executableElement));
            }
            return methods.values();
        }

        private Stream<ExecutableElement> getMethods(TypeElement typeElement) {
            return typeElement.getEnclosedElements().stream()
                    .filter(e -> ElementKind.METHOD.equals(e.getKind()))
                    .map(ExecutableElement.class::cast)
                    .filter(m -> !m.getModifiers().contains(Modifier.STATIC))
                    .filter(m -> !m.getModifiers().contains(Modifier.PRIVATE));
        }

        private Optional<TypeElement> getParent(TypeElement typeElement) {
            return processingEnv.getTypeUtils().directSupertypes(typeElement.asType()).stream()
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
}
