package com.batch.android.processor

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.joinToCode
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import java.io.IOException

/**
 * Processor for dependency injection using Kotlin Symbol Processing (KSP).
 */
class DIProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    companion object {
        private const val DI_PACKAGE = "com.batch.android.di"
        const val OUTPUT_PACKAGE = "$DI_PACKAGE.providers"
        val diClassName = ClassName(DI_PACKAGE, "DI")
    }

    /**
     * Entry point for the processing.
     */
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val moduleSymbols = resolver.getSymbolsWithAnnotation(Module::class.java.canonicalName)
        val ret = moduleSymbols.filter { !it.validate() }.toList()

        moduleSymbols
            .filter { it is KSClassDeclaration && it.validate() }
            .forEach { it.accept(DIClassVisitor(), Unit) }

        return ret
    }

    /**
     * Visitor for each Module class.
     */
    inner class DIClassVisitor : KSVisitorVoid() {

        @OptIn(KspExperimental::class)
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            if (!classDeclaration.isPublic()) {
                logger.warn(" ${classDeclaration.simpleName.asString()} annotated with @Module must be public", classDeclaration)
                return
            }
            val isSingleton = classDeclaration.isAnnotationPresent(Singleton::class)
            val isKotlinObject = classDeclaration.classKind == ClassKind.OBJECT
            val providerClassName = "${classDeclaration.simpleName.asString()}Provider"

            // Get companions object
            val companions = classDeclaration.declarations.filterIsInstance<KSClassDeclaration>()
                .filter { it.isCompanionObject }.toList()

            // Get provide methods
            val provideMethods = classDeclaration.declarations.filterIsInstance<KSFunctionDeclaration>()
                .filter { isProvideMethod(it, classDeclaration) }
                .toMutableList()

            // Add provide methods from companions
            companions.forEach { companion ->
                companion.declarations.filterIsInstance<KSFunctionDeclaration>()
                    .filter { isProvideMethod(it, classDeclaration) }
                    .forEach { provideMethods.add(it) }
            }

            // Get constructors
            val constructors = classDeclaration.declarations.filterIsInstance<KSFunctionDeclaration>()
                .filter { isConstructor(it) }
                .toList()

            val methodSpecs = mutableListOf<FunSpec>()
            when {
                provideMethods.isNotEmpty() -> {
                    provideMethods.forEach { provideMethod ->
                        val methodSpec = if (isSingleton) {
                            createSingletonGetMethodForProvide(provideMethod, classDeclaration)
                        } else {
                            createGetMethodForProvide(provideMethod, classDeclaration)
                        }
                        methodSpecs.add(methodSpec)
                    }
                }
                constructors.isNotEmpty() -> {
                    constructors.forEach { constructor ->
                        val methodSpec = if (isSingleton) {
                            createSingletonGetMethodForConstructor(constructor, classDeclaration, isKotlinObject)
                        } else {
                            createGetMethodForConstructor(constructor, classDeclaration, isKotlinObject)
                        }
                        methodSpecs.add(methodSpec)
                    }
                }
                else -> {
                    logger.warn("class annotated with @Module must have at least one public constructor or one @Provide method", classDeclaration)
                    return
                }
            }

            if (isSingleton) {
                methodSpecs.add(createSingletonGetInstanceMethod(classDeclaration))
            }

            val typeSpec = TypeSpec.objectBuilder(providerClassName)
                .addFunctions(methodSpecs)
                .addKdoc("isSingleton = $isSingleton")
                .build()

            val fileSpec = FileSpec.builder(OUTPUT_PACKAGE, providerClassName)
                .addType(typeSpec)
                .build()

            try {
                fileSpec.writeTo(codeGenerator, false)
            } catch (e: IOException) {
                logger.error("error when writing file", classDeclaration)
            }
        }
    }

    // Utils methods

    /**
     * Check if an element is a valid public constructor.
     */
    private fun isConstructor(function: KSFunctionDeclaration ): Boolean {
        return function.isConstructor() && function.isPublic()
    }

    /**
     * Check if an element is a valid provide method.
     */
    @OptIn(KspExperimental::class)
    private fun isProvideMethod(function: KSFunctionDeclaration, classDeclaration: KSClassDeclaration): Boolean {
       when (function.origin) {
           Origin.JAVA -> {
               return function.modifiers.contains(Modifier.PUBLIC) &&
                       function.modifiers.contains(Modifier.JAVA_STATIC) &&
                       function.simpleName.asString() == "provide" &&
                       function.isAnnotationPresent(Provide::class) &&
                       function.returnType?.resolve()?.makeNotNullable() == classDeclaration.asType(emptyList())
           }
           Origin.KOTLIN -> {
               return function.isPublic() &&
                       function.simpleName.asString() == "provide" &&
                       function.isAnnotationPresent(Provide::class) &&
                       function.returnType?.resolve()?.makeNotNullable() == classDeclaration.asType(emptyList())
           }
           else -> {
               logger.warn("unknown origin for function ${function.simpleName.asString()}")
               return false
           }
       }
    }

    /**
     * Generate the get() method of the provider when the module is not a singleton.
     * Create new instances by calling the provide static method.
     */
    private fun createGetMethodForProvide(
        element: KSFunctionDeclaration,
        returnType: KSClassDeclaration
    ): FunSpec {
        val parameters = element.parameters.map { parameter ->
            ParameterSpec.builder(parameter.name!!.asString(), parameter.type.resolve().toTypeName())
                .build()
        }
        val provideParams = element.parameters.map { CodeBlock.of("%N", it.name?.asString()) }
        val joinedParams = provideParams.joinToCode(", ")
        val returnTypeName = returnType.toClassName()

        return FunSpec.builder("get")
            .addModifiers(KModifier.PUBLIC, KModifier.FINAL)
            .returns(returnTypeName)
            .addParameters(parameters)
            .addAnnotation(NonNull::class)
            .addAnnotation(JvmStatic::class)
            .addStatement("return %T.%N(%L)", returnType.toClassName(), element.simpleName.asString(), joinedParams)
            .build()
    }

    /**
     * Generate the get() method of the provider when the module is not a singleton.
     * Create new instances by calling the module constructor directly.
     */
    private fun createGetMethodForConstructor(
        element: KSFunctionDeclaration,
        returnType: KSClassDeclaration,
        isKotlinObject: Boolean = false
    ): FunSpec {
        val parameters = element.parameters.map { parameter ->
            ParameterSpec.builder(parameter.name!!.asString(), parameter.type.resolve().toTypeName())
                .build()
        }
        val provideParams = element.parameters.map { CodeBlock.of("%N", it.name?.asString()) }
        val joinedParams = provideParams.joinToCode(", ")
        val returnTypeName = returnType.toClassName()

        val builder =  FunSpec.builder("get")
            .addModifiers(KModifier.PUBLIC, KModifier.FINAL)
            .returns(returnTypeName)
            .addParameters(parameters)
            .addAnnotation(NonNull::class)
            .addAnnotation(JvmStatic::class)

        if (isKotlinObject) {
            builder.addStatement("return %T", returnTypeName)
        } else {
            builder.addStatement("return %T(%L)", returnTypeName, joinedParams)
        }
        return builder.build()
    }

    /**
     * Create the getSingleton() method of the provider.
     */
    private fun createSingletonGetInstanceMethod(returnType: KSClassDeclaration): FunSpec {
        return FunSpec.builder("getSingleton")
            .addModifiers(KModifier.PUBLIC, KModifier.FINAL)
            .returns(returnType.toClassName().copy(true)) // workaround using copy to have nullable className
            .addAnnotation(Nullable::class)
            .addAnnotation(JvmStatic::class)
            .addStatement("return %T.getInstance().getSingletonInstance(%T::class.java)", diClassName, returnType.toClassName())
            .build()
    }

    /**
     * Generate the get() method of the provider when the module is a singleton.
     * Create new instances by calling the provide static method.
     * Create only one instance of the object.
     */
    private fun createSingletonGetMethodForProvide(
        element: KSFunctionDeclaration,
        returnType: KSClassDeclaration
    ): FunSpec {
        val parameters = element.parameters.map { parameter ->
            ParameterSpec.builder(parameter.name!!.asString(), parameter.type.resolve().toTypeName())
                .build()
        }
        val provideParams = element.parameters.map { CodeBlock.of("%N", it.name?.asString()) }
        val joinedParams = provideParams.joinToCode(", ")
        val returnTypeName = returnType.toClassName()
        val varName = CodeBlock.of("instance")

        return FunSpec.builder("get")
            .addModifiers(KModifier.PUBLIC, KModifier.FINAL)
            .returns(returnTypeName)
            .addParameters(parameters)
            .addAnnotation(NonNull::class)
            .addAnnotation(JvmStatic::class)
            .addStatement("var %L: %T? = %T.getInstance().getSingletonInstance(%T::class.java)", varName, returnTypeName, diClassName, returnTypeName)
            .beginControlFlow("if (%L != null)", varName)
            .addStatement("return %L", varName)
            .endControlFlow()
            .addStatement("%L = %T.%N(%L)", varName, returnTypeName, element.simpleName.asString(), joinedParams)
            .addStatement("%T.getInstance().addSingletonInstance(%T::class.java, %L)", diClassName, returnTypeName, varName)
            .addStatement("return %L", varName)
            .build()
    }

    /**
     * Generate the get() method of the provider when the module is a singleton.
     * Create new instance by calling the module constructor directly.
     * Create only one instance of the object.
     */
    private fun createSingletonGetMethodForConstructor(
        element: KSFunctionDeclaration,
        returnType: KSClassDeclaration,
        isKotlinObject: Boolean = false
    ): FunSpec {
        val parameters = element.parameters.map { parameter ->
            ParameterSpec.builder(parameter.name!!.asString(), parameter.type.resolve().toTypeName())
                .build()
        }
        val provideParams = element.parameters.map { CodeBlock.of("%N", it.name?.asString()) }
        val joinedParams = provideParams.joinToCode(", ")
        val returnTypeName = returnType.toClassName()
        val varName = CodeBlock.of("instance")

        val builder = FunSpec.builder("get")
            .addModifiers(KModifier.PUBLIC, KModifier.FINAL)
            .returns(returnTypeName)
            .addParameters(parameters)
            .addAnnotation(NonNull::class)
            .addAnnotation(JvmStatic::class)
            .addStatement("var %L: %T? = %T.getInstance().getSingletonInstance(%T::class.java)", varName, returnTypeName, diClassName, returnTypeName)
            .beginControlFlow("if (%L != null)", varName)
            .addStatement("return %L", varName)
            .endControlFlow()

        if (isKotlinObject) {
            builder.addStatement("%L = %T", varName, returnTypeName)
        } else {
            builder.addStatement("%L = %T(%L)", varName, returnTypeName, joinedParams)
        }

        return builder.addStatement("%T.getInstance().addSingletonInstance(%T::class.java, %L)", diClassName, returnTypeName, varName)
        .addStatement("return %L", varName)
        .build()
    }
}

class DIProcessorProvider : SymbolProcessorProvider {
    override fun create(
        environment: SymbolProcessorEnvironment
    ): SymbolProcessor {
        return DIProcessor(environment.codeGenerator, environment.logger)
    }
}