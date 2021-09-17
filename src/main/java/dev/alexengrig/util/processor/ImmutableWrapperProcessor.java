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

    private void doProcess(TypeElement typeElement) {
        String source = createSource(typeElement);
        JavaFileObject file = createFile(typeElement);
        writeSourceToFile(source, file);
    }

    private void writeSourceToFile(String source, JavaFileObject file) {
        try (PrintWriter sourcePrinter = new PrintWriter(file.openWriter())) {
            sourcePrinter.print(source);
        } catch (IOException e) {
            throw new RuntimeException("Fail on writing file", e);
        }
    }

    private String createFilename(TypeElement typeElement) {
        String prefixedClassname = getPrefixedClassName(typeElement);
        Optional<String> packageNameOptional = getPackageName(typeElement);
        return packageNameOptional
                .map(packageName -> packageName.concat(".").concat(prefixedClassname))
                .orElse(prefixedClassname);
    }

    private JavaFileObject createFile(TypeElement typeElement) {
        String filename = createFilename(typeElement);
        try {
            return processingEnv.getFiler().createSourceFile(filename);
        } catch (IOException e) {
            throw new IllegalArgumentException("Fail on creating file", e);
        }
    }

    private String createSource(TypeElement typeElement) {
        StringJoiner joiner = new StringJoiner("\n");
        getPackageName(typeElement).ifPresent(packageName ->
                joiner.add("package " + packageName + ";"));
        String className = getPrefixedClassName(typeElement);
        String parentClassName = getClassName(typeElement);
        joiner.add("public class " + className + " extends " + parentClassName + " {");
        joiner.add("    private final " + parentClassName + " target;");
        joiner.add("    public " + className + "(" + parentClassName + " target) {");
        joiner.add("        this.target = target;");
        joiner.add("    }");
        for (ExecutableElement method : getAllMethods(typeElement)) {
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

    private Collection<ExecutableElement> getAllMethods(TypeElement typeElement) {
        Map<String, ExecutableElement> methods = getMethods(typeElement)
                .collect(Collectors.toMap(this::getKey, Function.identity()));
        Optional<TypeElement> parent = getParent(typeElement);
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

    private String getKey(ExecutableElement m) {
        return m.toString();
    }

    private Optional<String> getPackageName(TypeElement typeElement) {
        String qualifiedName = typeElement.getQualifiedName().toString();
        int lastIndexOfDot = qualifiedName.lastIndexOf('.');
        if (lastIndexOfDot == -1) {
            return Optional.empty();
        }
        return Optional.of(qualifiedName.substring(0, lastIndexOfDot));
    }

    private String getClassName(TypeElement typeElement) {
        return typeElement.getQualifiedName().toString();
    }

    private String getSimpleClassName(TypeElement typeElement) {
        return typeElement.getSimpleName().toString();
    }

    private String getPrefixedClassName(TypeElement typeElement) {
        String simpleClassName = getSimpleClassName(typeElement);
        return WRAPPER_TYPE_PREFIX.concat(simpleClassName);
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
    public Iterable<? extends Completion> getCompletions(Element element, AnnotationMirror annotation, ExecutableElement member, String userText) {
        return Collections.emptySet();
    }
}
