package com.batch.android.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import com.tschuchort.compiletesting.kspWithCompilation
import org.assertj.core.api.AbstractAssert
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.lang.reflect.Method
import kotlin.jvm.java
import kotlin.reflect.KClass

@OptIn(ExperimentalCompilerApi::class)
class DIProcessorTest {

    // Requires to be in sources to avoid compilation error from generated provider files
    // since DI.java is not in the class loader
    private val diSourceFile =
        SourceFile.fromPath(File("../sdk/src/main/java/com/batch/android/di/DI.java"))

    @Test
    fun testBasicJavaModule() {

        val javaSource = SourceFile.java(
            "TestModule.java", """
        package com.batch.android;
        
        import com.batch.android.processor.Module;
        
                           
        @Module
        public class TestModule {}
    """
        )

        val result = KotlinCompilation().apply {
            sources = listOf(javaSource)
            inheritClassPath = true
            kspWithCompilation = true
            compilerPluginRegistrars
            symbolProcessorProviders = listOf(DIProcessorProvider())
        }.compile()

        Assert.assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val providerClass =  result.classLoader.loadClass("com.batch.android.di.providers.TestModuleProvider")
        assertThat(providerClass).hasDeclaredMethods("get")
    }

    @Test
    fun testBasicKotlinModule() {

        val sourceObjectModule = SourceFile.kotlin(
            "TestObjectModule.kt", """
            package com.batch.android
            
            import com.batch.android.processor.Module
    
            @Module
            object TestObjectModule
        """
        )

        val sourceClassModule = SourceFile.kotlin(
            "TestClassModule.kt", """
            package com.batch.android
            
            import com.batch.android.processor.Module
    
            @Module
            class TestClassModule
        """
        )


        val result = KotlinCompilation().apply {
            sources = listOf(diSourceFile, sourceObjectModule, sourceClassModule)
            inheritClassPath = true
            kspWithCompilation = true
            compilerPluginRegistrars
            symbolProcessorProviders = listOf(DIProcessorProvider())
        }.compile()

        Assert.assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val providerClass =  result.classLoader.loadClass("com.batch.android.di.providers.TestClassModuleProvider")
        assertThat(providerClass).hasDeclaredMethods("get")

        val providerObject =  result.classLoader.loadClass("com.batch.android.di.providers.TestObjectModuleProvider")
        assertThat(providerObject).hasDeclaredMethods("get")
    }

    @Test
    fun testBasicJavaSingletonModule() {

        val javaSource = SourceFile.java(
            "TestModule.java", """
        package com.batch.android;
        
        import com.batch.android.processor.Module;
        import com.batch.android.processor.Singleton;

        @Module
        @Singleton
        public class TestModule {}
        """
        )

        val result = KotlinCompilation().apply {
            sources = listOf(diSourceFile, javaSource)
            inheritClassPath = true
            kspWithCompilation = true
            symbolProcessorProviders = listOf(DIProcessorProvider())
        }.compile()

        Assert.assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val providerClass =
            result.classLoader.loadClass("com.batch.android.di.providers.TestModuleProvider")
        assertThat(providerClass).hasDeclaredMethods("get", "getSingleton")
    }

    @Test
    fun testBasicKotlinSingletonModule() {

        val sourceObjectModule = SourceFile.kotlin(
            "TestObjectModule.kt", """
            package com.batch.android
            
            import com.batch.android.processor.Module
            import com.batch.android.processor.Singleton
    
            @Module
            @Singleton
            object TestObjectModule
        """
        )

        val sourceClassModule = SourceFile.kotlin(
            "TestClassModule.kt", """
            package com.batch.android
            
            import com.batch.android.processor.Module
            import com.batch.android.processor.Singleton
    
            @Module
            @Singleton
            class TestClassModule
        """
        )

        val result = KotlinCompilation().apply {
            sources = listOf(diSourceFile, sourceObjectModule, sourceClassModule)
            inheritClassPath = true
            kspWithCompilation = true
            compilerPluginRegistrars
            symbolProcessorProviders = listOf(DIProcessorProvider())
        }.compile()

        Assert.assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val providerClass =  result.classLoader.loadClass("com.batch.android.di.providers.TestClassModuleProvider")
        assertThat(providerClass).hasDeclaredMethods("get", "getSingleton")

        val providerObject =  result.classLoader.loadClass("com.batch.android.di.providers.TestObjectModuleProvider")
        assertThat(providerObject).hasDeclaredMethods("get", "getSingleton")
    }


    @Test
    fun testJavaConstructorsSingletonModule() {

        val javaSource = SourceFile.java(
            "TestModule.java", """
        package com.batch.android;
        
        import com.batch.android.processor.Module;
        import com.batch.android.processor.Singleton;
        import androidx.annotation.NonNull;

        @Module
        @Singleton
        public class TestModule {
           public TestModule(@NonNull String arg1, Integer arg2) {}
    
           public TestModule(@NonNull String arg1, String arg2) {}
        }
        """
        )

        val result = KotlinCompilation().apply {
            sources = listOf(diSourceFile, javaSource)
            inheritClassPath = true
            kspWithCompilation = true
            symbolProcessorProviders = listOf(DIProcessorProvider())
        }.compile()

        Assert.assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val providerClass = result.classLoader.loadClass("com.batch.android.di.providers.TestModuleProvider")
        assertThat(providerClass).hasDeclaredMethods("get", "getSingleton")
        MethodAssert.assertThatClass(providerClass).hasMethod("get", String::class, Int::class)
        MethodAssert.assertThatClass(providerClass).hasMethod("get", String::class, String::class)
    }

    @Test
    fun testKotlinConstructorsSingletonModule() {

        val sourceClassModule = SourceFile.kotlin(
            "TestClassModule.kt", """
            package com.batch.android
            
            import com.batch.android.processor.Module
            import com.batch.android.processor.Singleton

            @Module
            @Singleton
            class TestClassModule(val arg1: String, val arg2: String) {
            
                constructor(arg1: String, arg2: Int) : this(arg1, arg2.toString())
            }
        """
        )

        val result = KotlinCompilation().apply {
            sources = listOf(diSourceFile, sourceClassModule)
            inheritClassPath = true
            kspWithCompilation = true
            compilerPluginRegistrars
            symbolProcessorProviders = listOf(DIProcessorProvider())
        }.compile()

        Assert.assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val providerClass =  result.classLoader.loadClass("com.batch.android.di.providers.TestClassModuleProvider")
        assertThat(providerClass).hasDeclaredMethods("get", "getSingleton")
        MethodAssert.assertThatClass(providerClass).hasMethod("get", String::class, Int::class)
        MethodAssert.assertThatClass(providerClass).hasMethod("get", String::class, String::class)
    }

    @Test
    fun testJavaProvideMethodsModule() {

        val javaSource = SourceFile.java(
            "TestModule.java", """
        package com.batch.android;
        
        import com.batch.android.processor.Module;
        import com.batch.android.processor.Provide;
        import androidx.annotation.NonNull;
        
        @Module
        public class TestModule {
            @Provide
            public static TestModule provide(@NonNull String arg1, Integer arg2) {
                return new TestModule();
            }
        
            @Provide
            public static TestModule provide(@NonNull String arg1, String arg2) {
                return new TestModule();
            }
        }
        """
        )

        val result = KotlinCompilation().apply {
            sources = listOf(diSourceFile, javaSource)
            inheritClassPath = true
            kspWithCompilation = true
            symbolProcessorProviders = listOf(DIProcessorProvider())
        }.compile()

        Assert.assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val providerClass = result.classLoader.loadClass("com.batch.android.di.providers.TestModuleProvider")
        MethodAssert.assertThatClass(providerClass).hasMethod("get", String::class, Int::class)
        MethodAssert.assertThatClass(providerClass).hasMethod("get", String::class, String::class)
    }

    @Test
    fun testKotlinProvideMethodsModule() {

        val kotlinObjectSource = SourceFile.kotlin(
            "TestObjectModule.kt", """
        package com.batch.android
        
        import com.batch.android.processor.Module
        import com.batch.android.processor.Provide
        import androidx.annotation.NonNull
        
        @Module
        object TestObjectModule {
            @Provide
            fun provide(arg1: String, arg2: Int) : TestObjectModule {
                return TestObjectModule
            }
        
            @Provide
            fun provide(arg1: String, arg2: String) : TestObjectModule {
                return TestObjectModule
            }
        }
        """
        )

        val kotlinClassSource = SourceFile.kotlin(
            "TestClassModule.kt", """
        package com.batch.android
        
        import com.batch.android.processor.Module
        import com.batch.android.processor.Provide
        import androidx.annotation.NonNull
        
        @Module
        class TestClassModule {
            companion object { 
                @Provide
                fun provide(arg1: String, arg2: Int) : TestClassModule {
                    return TestClassModule()
                }
            
                @Provide
                fun provide(arg1: String, arg2: String) : TestClassModule {
                    return TestClassModule()
                }
            }
        }
        """
        )

        val result = KotlinCompilation().apply {
            sources = listOf(diSourceFile, kotlinObjectSource, kotlinClassSource)
            inheritClassPath = true
            kspWithCompilation = true
            symbolProcessorProviders = listOf(DIProcessorProvider())
        }.compile()

        Assert.assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val providerObject = result.classLoader.loadClass("com.batch.android.di.providers.TestObjectModuleProvider")
        MethodAssert.assertThatClass(providerObject).hasMethod("get", String::class, Int::class)
        MethodAssert.assertThatClass(providerObject).hasMethod("get", String::class, String::class)

        val providerClass = result.classLoader.loadClass("com.batch.android.di.providers.TestClassModuleProvider")
        MethodAssert.assertThatClass(providerClass).hasMethod("get", String::class, Int::class)
        MethodAssert.assertThatClass(providerClass).hasMethod("get", String::class, String::class)
    }

    @Test
    fun testJavaProvideMethodsSingletonModule() {

        val javaSource = SourceFile.java(
            "TestModule.java", """
        package com.batch.android;
        
        import com.batch.android.processor.Module;
        import com.batch.android.processor.Provide;
        import com.batch.android.processor.Singleton;
        import androidx.annotation.NonNull;
        
        @Module
        @Singleton
        public class TestModule {
            @Provide
            public static TestModule provide(@NonNull String arg1, Integer arg2) {
                return new TestModule();
            }
        
            @Provide
            public static TestModule provide(@NonNull String arg1, String arg2) {
                return new TestModule();
            }
        }
        """
        )

        val result = KotlinCompilation().apply {
            sources = listOf(diSourceFile, javaSource)
            inheritClassPath = true
            kspWithCompilation = true
            symbolProcessorProviders = listOf(DIProcessorProvider())
        }.compile()

        Assert.assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val providerClass = result.classLoader.loadClass("com.batch.android.di.providers.TestModuleProvider")
        assertThat(providerClass).hasDeclaredMethods("get", "getSingleton")
        MethodAssert.assertThatClass(providerClass).hasMethod("get", String::class, Int::class)
        MethodAssert.assertThatClass(providerClass).hasMethod("get", String::class, String::class)
    }

    @Test
    fun testKotlinProvideMethodsSingletonModule() {


        val kotlinObjectSource = SourceFile.kotlin(
            "TestObjectModule.kt", """
        package com.batch.android
        
        import com.batch.android.processor.Module
        import com.batch.android.processor.Provide
        import com.batch.android.processor.Singleton
        import androidx.annotation.NonNull
        
        @Module
        @Singleton
        object TestObjectModule {
            @Provide
            fun provide(arg1: String, arg2: Int) : TestObjectModule {
                return TestObjectModule
            }
        
            @Provide
            fun provide(arg1: String, arg2: String) : TestObjectModule {
                return TestObjectModule
            }
        }
        """
        )

        val kotlinClassSource = SourceFile.kotlin(
            "TestClassModule.kt", """
        package com.batch.android
        
        import com.batch.android.processor.Module
        import com.batch.android.processor.Provide
        import com.batch.android.processor.Singleton
        import androidx.annotation.NonNull
        
        @Module
        @Singleton
        class TestClassModule {
            companion object { 
                @Provide
                fun provide(arg1: String, arg2: Int) : TestClassModule {
                    return TestClassModule()
                }
            
                @Provide
                fun provide(arg1: String, arg2: String) : TestClassModule {
                    return TestClassModule()
                }
            }
        }
        """
        )

        val result = KotlinCompilation().apply {
            sources = listOf(diSourceFile, kotlinObjectSource, kotlinClassSource)
            inheritClassPath = true
            kspWithCompilation = true
            symbolProcessorProviders = listOf(DIProcessorProvider())
        }.compile()

        Assert.assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val providerObject = result.classLoader.loadClass("com.batch.android.di.providers.TestObjectModuleProvider")
        assertThat(providerObject).hasDeclaredMethods("get", "getSingleton")
        MethodAssert.assertThatClass(providerObject).hasMethod("get", String::class, Int::class)
        MethodAssert.assertThatClass(providerObject).hasMethod("get", String::class, String::class)

        val providerClass = result.classLoader.loadClass("com.batch.android.di.providers.TestClassModuleProvider")
        assertThat(providerClass).hasDeclaredMethods("get", "getSingleton")
        MethodAssert.assertThatClass(providerClass).hasMethod("get", String::class, Int::class)
        MethodAssert.assertThatClass(providerClass).hasMethod("get", String::class, String::class)
    }


    @Test
    fun testJavaNotPublicModule() {
        val javaSource = SourceFile.java(
            "TestModule.java", """
        package com.batch.android;
        
        import com.batch.android.processor.Module;
        
                           
        @Module
        class TestModule {}
        """
        )

        val result = KotlinCompilation().apply {
            sources = listOf(diSourceFile, javaSource)
            inheritClassPath = true
            kspWithCompilation = true
            symbolProcessorProviders = listOf(DIProcessorProvider())
        }.compile()

        Assert.assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        assertThat(result.messages).contains("TestModule annotated with @Module must be public")
    }

    @Test
    fun testKotlinNotPublicModule() {
        val kotlinClassSource = SourceFile.kotlin(
            "TestClassModule.kt", """
        package com.batch.android
        
        import com.batch.android.processor.Module

        @Module
        private class TestClassModule
        """
        )

        val kotlinObjectSource = SourceFile.kotlin(
            "TestObjectModule.kt", """
        package com.batch.android
        
        import com.batch.android.processor.Module

        @Module
        private object TestObjectModule
        """
        )

        val result = KotlinCompilation().apply {
            sources = listOf(diSourceFile, kotlinClassSource, kotlinObjectSource)
            inheritClassPath = true
            kspWithCompilation = true
            symbolProcessorProviders = listOf(DIProcessorProvider())
        }.compile()

        Assert.assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        assertThat(result.messages).contains("TestClassModule annotated with @Module must be public", "TestObjectModule annotated with @Module must be public")
    }

    @Test
    fun testJavaNotPublicConstructorModule() {
        val javaSource = SourceFile.java(
            "TestModule.java", """
        package com.batch.android;
        
        import com.batch.android.processor.Module;
        
                           
        @Module
        public class TestModule {
            private TestModule() {}
        }
        """
        )

        val result = KotlinCompilation().apply {
            sources = listOf(diSourceFile, javaSource)
            inheritClassPath = true
            kspWithCompilation = true
            symbolProcessorProviders = listOf(DIProcessorProvider())
        }.compile()

        Assert.assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        assertThat(result.messages).contains("class annotated with @Module must have at least one public constructor or one @Provide method")
    }

    @Test
    fun testKotlinNotPublicConstructorModule() {
        val kotlinSource = SourceFile.kotlin(
            "TestModule.kt", """
        package com.batch.android
        
        import com.batch.android.processor.Module

        @Module
        class TestClassModule private constructor()
        """
        )

        val result = KotlinCompilation().apply {
            sources = listOf(diSourceFile, kotlinSource)
            inheritClassPath = true
            kspWithCompilation = true
            symbolProcessorProviders = listOf(DIProcessorProvider())
        }.compile()

        Assert.assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        assertThat(result.messages).contains("class annotated with @Module must have at least one public constructor or one @Provide method")
    }

}



/**
 * Custom AssertJ assertion for classes.
 *
 * Assert that a class has a method with the given name and parameter types.
 * since AssertJ does not for us ...
 */
class MethodAssert(actual: Class<*>) : AbstractAssert<MethodAssert, Class<*>>(actual, MethodAssert::class.java) {

    fun hasMethod(methodName: String, vararg parameterTypes: KClass<*>): MethodAssert {
        isNotNull

        val foundMethods = actual.methods.filter { it.name == methodName }
        if (foundMethods.isEmpty()) {
            failWithMessage("Expected class <%s> to have method <%s> but it does not", actual, methodName)
        }
        val foundMethod = foundMethods.firstOrNull { doesMethodMatchParameterTypes(it, parameterTypes) }

        if (foundMethod == null) {
            failWithMessage(
                "Expected class <%s> to have method <%s> with parameter types <%s> but it does not",
                actual,
                methodName,
                parameterTypes.joinToString { it.qualifiedName ?: "null" }
            )
        }

        return this
    }

    private fun doesMethodMatchParameterTypes(method: Method, parameterTypes: Array<out KClass<*>>): Boolean {
        val methodParameterTypes = method.parameterTypes
        if (methodParameterTypes.size != parameterTypes.size) {
            return false
        }

        for (i in methodParameterTypes.indices) {
            if (!methodParameterTypes[i].isAssignableFrom(parameterTypes[i].java)) {
                return false
            }
        }
        return true
    }

    companion object {
        fun assertThatClass(actual: Class<*>): MethodAssert {
            return MethodAssert(actual)
        }
    }
}