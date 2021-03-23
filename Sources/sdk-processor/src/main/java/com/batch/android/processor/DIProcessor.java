package com.batch.android.processor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

public class DIProcessor extends AbstractProcessor
{
    private static final String DI_PACKAGE = "com.batch.android.di";
    private static final String OUTPUT_PACKAGE = DI_PACKAGE + ".providers";

    private ClassName diClass;
    private Filer filer;
    private Messager messager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment)
    {
        super.init(processingEnvironment);
        this.filer = processingEnvironment.getFiler();
        this.messager = processingEnvironment.getMessager();
        diClass = ClassName.get(DI_PACKAGE, "DI");
    }

    @Override
    public Set<String> getSupportedAnnotationTypes()
    {
        return new HashSet<>(Arrays.asList(
                Module.class.getCanonicalName(),
                Singleton.class.getCanonicalName(),
                Provide.class.getCanonicalName()));
    }

    @Override
    public SourceVersion getSupportedSourceVersion()
    {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment)
    {
        Set<? extends Element> elementsToBind = roundEnvironment.getElementsAnnotatedWith(
                Module.class);

        for (Element element : elementsToBind) {
            if (!isClass(element)) {
                // The annotated element is not a public class, skipping it
                messager.printMessage(Diagnostic.Kind.WARNING,
                        "class annotated with @Module must be public",
                        element);
                continue;
            }

            List<ExecutableElement> provideMethods = new ArrayList<>();
            List<ExecutableElement> constructors = new ArrayList<>();
            boolean isSingleton = isSingleton(element);
            String providerClassName = element.getSimpleName() + "Provider";

            for (Element enclosed : element.getEnclosedElements()) {
                if (isProvideMethod(enclosed, element.asType())) {
                    // The enclosed element is a provide method that we can use
                    provideMethods.add((ExecutableElement) enclosed);
                } else if (isConstructor(enclosed)) {
                    // The enclosed element is a public constructor
                    constructors.add((ExecutableElement) enclosed);
                }
            }

            List<MethodSpec> methodSpecs = new ArrayList<>();
            if (!provideMethods.isEmpty()) {
                for (ExecutableElement provideMethod : provideMethods) {
                    MethodSpec methodSpec;
                    if (!isSingleton) {
                        methodSpec = createGetMethodForProvide(provideMethod, element.asType());
                    } else {
                        methodSpec = createSingletonGetMethodForProvide(provideMethod,
                                element.asType());
                    }

                    methodSpecs.add(methodSpec);
                }
            } else if (!constructors.isEmpty()) {
                for (ExecutableElement constructor : constructors) {
                    MethodSpec methodSpec;
                    if (!isSingleton) {
                        methodSpec = createGetMethodForConstructor(constructor, element.asType());
                    } else {
                        methodSpec = createSingletonGetMethodForConstructor(constructor,
                                element.asType());
                    }
                    methodSpecs.add(methodSpec);
                }
            } else {
                messager.printMessage(Diagnostic.Kind.WARNING,
                        "class annotated with @Module must have at least one public constructor or one @Provide method",
                        element);
            }

            if (isSingleton) {
                methodSpecs.add(createSingletonGetInstanceMethod(element.asType()));
            }

            TypeSpec builder = TypeSpec
                    .classBuilder(providerClassName)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addMethods(methodSpecs)
                    .addJavadoc("isSingleton = $L", isSingleton)
                    .build();

            JavaFile javaFile = JavaFile.builder(OUTPUT_PACKAGE, builder).build();
            try {
                javaFile.writeTo(filer);
            } catch (IOException e) {
                e.printStackTrace();
                messager.printMessage(Diagnostic.Kind.WARNING,
                        "error when writing file",
                        element);
            }
        }

        return true;
    }

    // Utils methods

    private boolean isSingleton(Element element)
    {
        return element.getAnnotation(Singleton.class) != null;
    }

    /**
     * Check if an element is a valid class
     *
     * @param element
     * @return
     */
    private boolean isClass(Element element)
    {
        if (element.getKind() != ElementKind.CLASS) {
            return false;
        }

        Set<Modifier> modifiers = element.getModifiers();
        return modifiers.contains(Modifier.PUBLIC);
    }

    /**
     * Check if an element is a valid constructor
     *
     * @param element
     * @return
     */
    private boolean isConstructor(Element element)
    {
        if (element.getKind() != ElementKind.CONSTRUCTOR) {
            return false;
        }

        Set<Modifier> modifiers = element.getModifiers();
        return modifiers.contains(Modifier.PUBLIC);
    }

    /**
     * Check if an element is a valid provide method
     *
     * @param element
     * @param returnType
     * @return
     */
    private boolean isProvideMethod(Element element, TypeMirror returnType)
    {
        if (element.getKind() != ElementKind.METHOD ||
                element.getAnnotation(Provide.class) == null ||
                !element.getSimpleName().toString().equals("provide"))
        {
            return false;
        }

        Set<Modifier> modifiers = element.getModifiers();
        if (!modifiers.contains(Modifier.PUBLIC) ||
                !modifiers.contains(Modifier.STATIC))
        {
            return false;
        }

        return ( (ExecutableElement) element ).getReturnType().equals(returnType);
    }

    /**
     * Generate the get() method of the provider when the module is not a singleton
     * Create new instances by calling the provide static method
     *
     * @param element
     * @param returnType
     * @return
     */
    private MethodSpec createGetMethodForProvide(ExecutableElement element,
                                                 TypeMirror returnType)
    {
        List<ParameterSpec> parameters = new ArrayList<>();
        List<CodeBlock> provideParams = new ArrayList<>();
        for (VariableElement param : element.getParameters()) {
            parameters.add(ParameterSpec.get(param));
            provideParams.add(CodeBlock.of(ParameterSpec.get(param).name));
        }

        CodeBlock joinedParams = CodeBlock.join(provideParams, ", ");
        TypeName returnTypeName = TypeName.get(returnType);
        return MethodSpec.methodBuilder("get")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
                .returns(returnTypeName)
                .addParameters(parameters)
                .addAnnotation(NonNull.class)
                .addStatement("return $T.$N($L)", // We return a new instance every time
                        returnType,
                        element.getSimpleName(),
                        joinedParams)
                .build();
    }

    /**
     * Generate the get() method of the provider when the module is not a singleton
     * Create new instances by calling the module constructor directly
     *
     * @param element
     * @param returnType
     * @return
     */
    private MethodSpec createGetMethodForConstructor(ExecutableElement element,
                                                     TypeMirror returnType)
    {
        List<ParameterSpec> parameters = new ArrayList<>();
        List<CodeBlock> provideParams = new ArrayList<>();
        for (VariableElement param : element.getParameters()) {
            parameters.add(ParameterSpec.get(param));
            provideParams.add(CodeBlock.of(ParameterSpec.get(param).name));
        }

        CodeBlock joinedParams = CodeBlock.join(provideParams, ", ");
        TypeName returnTypeName = TypeName.get(returnType);
        return MethodSpec.methodBuilder("get")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
                .returns(returnTypeName)
                .addParameters(parameters)
                .addAnnotation(NonNull.class)
                .addStatement("return new $T($L)", // We return a new instance every time
                        returnType,
                        joinedParams)
                .build();
    }

    /**
     * Create the getSingleton() method of the provider
     *
     * @param returnType
     * @return
     */
    private MethodSpec createSingletonGetInstanceMethod(TypeMirror returnType)
    {
        TypeName returnTypeName = TypeName.get(returnType);
        return MethodSpec.methodBuilder("getSingleton")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
                .returns(returnTypeName)
                .addAnnotation(Nullable.class)
                .addStatement("return $T.getInstance().getSingletonInstance($T.class)",
                        diClass,
                        returnType)
                .build();
    }

    /**
     * Generate the get() method of the provider when the module is a singleton
     * Create new instances by calling the provide static method
     * Create only one instance of the object
     *
     * @param element
     * @param returnType
     * @return
     */
    private MethodSpec createSingletonGetMethodForProvide(ExecutableElement element,
                                                          TypeMirror returnType)
    {
        List<ParameterSpec> parameters = new ArrayList<>();
        List<CodeBlock> provideParams = new ArrayList<>();
        for (VariableElement param : element.getParameters()) {
            parameters.add(ParameterSpec.get(param));
            provideParams.add(CodeBlock.of(ParameterSpec.get(param).name));
        }
        CodeBlock varName = CodeBlock.of("instance");

        CodeBlock joinedParams = CodeBlock.join(provideParams, ", ");
        TypeName returnTypeName = TypeName.get(returnType);
        return MethodSpec.methodBuilder("get")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
                .returns(returnTypeName)
                .addParameters(parameters)
                .addAnnotation(NonNull.class)
                .addStatement("$T $L = $T.getInstance().getSingletonInstance($T.class)",
                        // We return a new instance every time
                        returnType,
                        varName,
                        diClass,
                        returnType)
                .beginControlFlow("if ($L != null)", varName)
                .addStatement("return $L", varName)
                .endControlFlow()
                .addStatement("$L = $T.$N($L)", // We create a new instance
                        varName,
                        returnType,
                        element.getSimpleName(),
                        joinedParams)
                .addStatement("$T.getInstance().addSingletonInstance($T.class, $L)",
                        diClass,
                        returnType,
                        varName)
                .addStatement("return $L", varName)
                .build();
    }


    /**
     * Generate the get() method of the provider when the module is a singleton
     * Create new instance by calling the module constructor directly
     * Create only one instance of the object
     *
     * @param element
     * @param returnType
     * @return
     */
    private MethodSpec createSingletonGetMethodForConstructor(ExecutableElement element,
                                                              TypeMirror returnType)
    {
        List<ParameterSpec> parameters = new ArrayList<>();
        List<CodeBlock> provideParams = new ArrayList<>();
        for (VariableElement param : element.getParameters()) {
            parameters.add(ParameterSpec.get(param));
            provideParams.add(CodeBlock.of(ParameterSpec.get(param).name));
        }

        CodeBlock varName = CodeBlock.of("instance");

        CodeBlock joinedParams = CodeBlock.join(provideParams, ", ");
        TypeName returnTypeName = TypeName.get(returnType);
        return MethodSpec.methodBuilder("get")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
                .returns(returnTypeName)
                .addParameters(parameters)
                .addAnnotation(NonNull.class)
                .addStatement("$T $L = $T.getInstance().getSingletonInstance($T.class)",
                        // We return a new instance every time
                        returnType,
                        varName,
                        diClass,
                        returnType)
                .beginControlFlow("if ($L != null)", varName)
                .addStatement("return $L", varName)
                .endControlFlow()
                .addStatement("$L = new $T($L)", // We create a new instance
                        varName,
                        returnType,
                        joinedParams)
                .addStatement("$T.getInstance().addSingletonInstance($T.class, $L)",
                        diClass,
                        returnType,
                        varName)
                .addStatement("return $L", varName)
                .build();
    }
}
