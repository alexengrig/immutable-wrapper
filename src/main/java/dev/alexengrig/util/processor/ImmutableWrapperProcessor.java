package dev.alexengrig.util.processor;

import com.google.auto.service.AutoService;
import dev.alexengrig.util.annotation.ImmutableWrapper;
import dev.alexengrig.util.context.ImmutableWrapperContext;
import dev.alexengrig.util.generator.ImmutableWrapperSourceGenerator;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Completion;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Set;

@AutoService(Processor.class)
public class ImmutableWrapperProcessor extends AbstractProcessor {

    private static final Class<ImmutableWrapper> ANNOTATION_TYPE = ImmutableWrapper.class;

    private ImmutableWrapperSourceGenerator sourceGenerator;

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
    public Iterable<? extends Completion> getCompletions(Element element, AnnotationMirror annotation, ExecutableElement member, String userText) {
        return Collections.emptySet();
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
        ImmutableWrapperContext context = createContext(typeElement);
        String source = createSource(context);
        JavaFileObject file = createFile(context);
        writeSourceToFile(source, file);
    }

    private ImmutableWrapperContext createContext(TypeElement typeElement) {
        return new ImmutableWrapperContext(processingEnv, typeElement);
    }

    private String createSource(ImmutableWrapperContext context) {
        return sourceGenerator.generateSource(context);
    }

    private JavaFileObject createFile(ImmutableWrapperContext context) {
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
}
