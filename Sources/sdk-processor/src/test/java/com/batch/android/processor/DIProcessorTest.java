package com.batch.android.processor;

import com.google.common.base.Joiner;
import com.google.common.truth.Truth;
import com.google.testing.compile.JavaFileObjects;
import com.google.testing.compile.JavaSourcesSubjectFactory;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import javax.tools.JavaFileObject;

public class DIProcessorTest
{
    @Test
    public void testBasicModule()
    {
        final JavaFileObject input = JavaFileObjects.forSourceString(
                "com.batch.android.TestModule",
                Joiner.on("\n").join(
                        "package com.batch.android;",
                        "",
                        "import com.batch.android.processor.Module;",
                        "",
                        "@Module",
                        "public class TestModule {}"
                )
        );

        final JavaFileObject output = JavaFileObjects.forSourceString(
                "com.batch.android.di.providers.TestModuleProvider",
                Joiner.on("\n").join(
                        "package com.batch.android.di.providers;",
                        "",
                        "import androidx.annotation.NonNull;",
                        "import com.batch.android.TestModule;",
                        "",
                        "public final class TestModuleProvider {",
                        "   @NonNull",
                        "   public static final TestModule get() {",
                        "       return new TestModule();",
                        "   }",
                        "}"
                )
        );

        Truth.assert_()
                .about(JavaSourcesSubjectFactory.javaSources())
                .that(Collections.singletonList(input))
                .processedWith(new DIProcessor())
                .compilesWithoutError()
                .and()
                .generatesSources(output);
    }

    @Test
    public void testBasicSingletonModule()
    {
        final JavaFileObject input = JavaFileObjects.forSourceString(
                "com.batch.android.TestModule",
                Joiner.on("\n").join(
                        "package com.batch.android;",
                        "",
                        "import com.batch.android.processor.Module;",
                        "import com.batch.android.processor.Singleton;",
                        "",
                        "@Module",
                        "@Singleton",
                        "public class TestModule {}"
                )
        );

        final JavaFileObject output = JavaFileObjects.forSourceString(
                "com.batch.android.di.providers.TestModuleProvider",
                Joiner.on("\n").join(
                        "package com.batch.android.di.providers;",
                        "",
                        "import androidx.annotation.NonNull;",
                        "import androidx.annotation.Nullable;",
                        "import com.batch.android.TestModule;",
                        "import com.batch.android.di.DI;",
                        "",
                        "public final class TestModuleProvider {",
                        "   @NonNull",
                        "   public static final TestModule get() {",
                        "       TestModule instance = DI.getInstance().getSingletonInstance(TestModule.class);",
                        "       if (instance != null) {",
                        "          return instance;",
                        "       }",
                        "       instance = new TestModule();",
                        "       DI.getInstance().addSingletonInstance(TestModule.class, instance);",
                        "       return instance;",
                        "   }",
                        "",
                        "   @Nullable",
                        "   public static final TestModule getSingleton() {",
                        "      return DI.getInstance().getSingletonInstance(TestModule.class);",
                        "   }",
                        "}"
                )
        );

        Truth.assert_()
                .about(JavaSourcesSubjectFactory.javaSources())
                .that(Arrays.asList(input, getDIFile()))
                .processedWith(new DIProcessor())
                .compilesWithoutError()
                .and()
                .generatesSources(output);
    }

    @Test
    public void testConstructorsModule()
    {
        final JavaFileObject input = JavaFileObjects.forSourceString(
                "com.batch.android.TestModule",
                Joiner.on("\n").join(
                        "package com.batch.android;",
                        "",
                        "import com.batch.android.processor.Module;",
                        "import androidx.annotation.NonNull;",
                        "",
                        "@Module",
                        "public class TestModule {",
                        "   public TestModule(@NonNull String arg1, Integer arg2) {}",
                        "",
                        "   public TestModule(@NonNull String arg1, String arg2) {}",
                        "}"
                )
        );

        final JavaFileObject output = JavaFileObjects.forSourceString(
                "com.batch.android.di.providers.TestModuleProvider",
                Joiner.on("\n").join(
                        "package com.batch.android.di.providers;",
                        "",
                        "import androidx.annotation.NonNull;",
                        "import com.batch.android.TestModule;",
                        "import java.lang.Integer;",
                        "import java.lang.String;",
                        "",
                        "public final class TestModuleProvider {",
                        "   @NonNull",
                        "   public static final TestModule get(@NonNull String arg1, Integer arg2) {",
                        "       return new TestModule(arg1, arg2);",
                        "   }",
                        "",
                        "   @NonNull",
                        "   public static final TestModule get(@NonNull String arg1, String arg2) {",
                        "       return new TestModule(arg1, arg2);",
                        "   }",
                        "}"
                )
        );

        Truth.assert_()
                .about(JavaSourcesSubjectFactory.javaSources())
                .that(Collections.singletonList(input))
                .processedWith(new DIProcessor())
                .compilesWithoutError()
                .and()
                .generatesSources(output);
    }

    @Test
    public void testConstructorsSingletonModule()
    {
        final JavaFileObject input = JavaFileObjects.forSourceString(
                "com.batch.android.TestModule",
                Joiner.on("\n").join(
                        "package com.batch.android;",
                        "",
                        "import androidx.annotation.NonNull;",
                        "import com.batch.android.processor.Module;",
                        "import com.batch.android.processor.Singleton;",
                        "",
                        "@Module",
                        "@Singleton",
                        "public class TestModule {",
                        "   public TestModule(@NonNull String arg1, Integer arg2) {}",
                        "",
                        "   public TestModule(@NonNull String arg1, String arg2) {}",
                        "}"
                )
        );

        final JavaFileObject output = JavaFileObjects.forSourceString(
                "com.batch.android.di.providers.TestModuleProvider",
                Joiner.on("\n").join(
                        "package com.batch.android.di.providers;",
                        "",
                        "import androidx.annotation.NonNull;",
                        "import androidx.annotation.Nullable;",
                        "import com.batch.android.TestModule;",
                        "import com.batch.android.di.DI;",
                        "import java.lang.Integer;",
                        "import java.lang.String;",
                        "",
                        "public final class TestModuleProvider {",
                        "   @NonNull",
                        "   public static final TestModule get(@NonNull String arg1, Integer arg2) {",
                        "       TestModule instance = DI.getInstance().getSingletonInstance(TestModule.class);",
                        "       if (instance != null) {",
                        "          return instance;",
                        "       }",
                        "       instance = new TestModule(arg1, arg2);",
                        "       DI.getInstance().addSingletonInstance(TestModule.class, instance);",
                        "       return instance;",
                        "   }",
                        "",
                        "   @NonNull",
                        "   public static final TestModule get(@NonNull String arg1, String arg2) {",
                        "       TestModule instance = DI.getInstance().getSingletonInstance(TestModule.class);",
                        "       if (instance != null) {",
                        "          return instance;",
                        "       }",
                        "       instance = new TestModule(arg1, arg2);",
                        "       DI.getInstance().addSingletonInstance(TestModule.class, instance);",
                        "       return instance;",
                        "   }",
                        "",
                        "   @Nullable",
                        "   public static final TestModule getSingleton() {",
                        "      return DI.getInstance().getSingletonInstance(TestModule.class);",
                        "   }",
                        "}"
                )
        );

        Truth.assert_()
                .about(JavaSourcesSubjectFactory.javaSources())
                .that(Arrays.asList(input, getDIFile()))
                .processedWith(new DIProcessor())
                .compilesWithoutError()
                .and()
                .generatesSources(output);
    }

    @Test
    public void testProvideMethodsModule()
    {
        final JavaFileObject input = JavaFileObjects.forSourceString(
                "com.batch.android.TestModule",
                Joiner.on("\n").join(
                        "package com.batch.android;",
                        "",
                        "import com.batch.android.processor.Module;",
                        "import com.batch.android.processor.Provide;",
                        "import androidx.annotation.NonNull;",
                        "",
                        "@Module",
                        "public class TestModule {",
                        "   @Provide",
                        "   public static TestModule provide(@NonNull String arg1, Integer arg2) {",
                        "      return new TestModule();",
                        "   }",
                        "",
                        "   @Provide",
                        "   public static TestModule provide(@NonNull String arg1, String arg2) {",
                        "      return new TestModule();",
                        "   }",
                        "}"
                )
        );

        final JavaFileObject output = JavaFileObjects.forSourceString(
                "com.batch.android.di.providers.TestModuleProvider",
                Joiner.on("\n").join(
                        "package com.batch.android.di.providers;",
                        "",
                        "import androidx.annotation.NonNull;",
                        "import com.batch.android.TestModule;",
                        "import java.lang.Integer;",
                        "import java.lang.String;",
                        "",
                        "public final class TestModuleProvider {",
                        "   @NonNull",
                        "   public static final TestModule get(@NonNull String arg1, Integer arg2) {",
                        "       return TestModule.provide(arg1, arg2);",
                        "   }",
                        "",
                        "   @NonNull",
                        "   public static final TestModule get(@NonNull String arg1, String arg2) {",
                        "       return TestModule.provide(arg1, arg2);",
                        "   }",
                        "}"
                )
        );

        Truth.assert_()
                .about(JavaSourcesSubjectFactory.javaSources())
                .that(Collections.singletonList(input))
                .processedWith(new DIProcessor())
                .compilesWithoutError()
                .and()
                .generatesSources(output);
    }

    @Test
    public void testProvideMethodsSingletonModule()
    {
        final JavaFileObject input = JavaFileObjects.forSourceString(
                "com.batch.android.TestModule",
                Joiner.on("\n").join(
                        "package com.batch.android;",
                        "",
                        "import androidx.annotation.NonNull;",
                        "import com.batch.android.processor.Module;",
                        "import com.batch.android.processor.Singleton;",
                        "import com.batch.android.processor.Provide;",
                        "",
                        "@Module",
                        "@Singleton",
                        "public class TestModule {",
                        "   @Provide",
                        "   public static TestModule provide(@NonNull String arg1, Integer arg2) {",
                        "      return new TestModule();",
                        "   }",
                        "",
                        "   @Provide",
                        "   public static TestModule provide(@NonNull String arg1, String arg2) {",
                        "      return new TestModule();",
                        "   }",
                        "}"
                )
        );

        final JavaFileObject output = JavaFileObjects.forSourceString(
                "com.batch.android.di.providers.TestModuleProvider",
                Joiner.on("\n").join(
                        "package com.batch.android.di.providers;",
                        "",
                        "import androidx.annotation.NonNull;",
                        "import androidx.annotation.Nullable;",
                        "import com.batch.android.TestModule;",
                        "import com.batch.android.di.DI;",
                        "import java.lang.Integer;",
                        "import java.lang.String;",
                        "",
                        "public final class TestModuleProvider {",
                        "   @NonNull",
                        "   public static final TestModule get(@NonNull String arg1, Integer arg2) {",
                        "       TestModule instance = DI.getInstance().getSingletonInstance(TestModule.class);",
                        "       if (instance != null) {",
                        "          return instance;",
                        "       }",
                        "       instance = TestModule.provide(arg1, arg2);",
                        "       DI.getInstance().addSingletonInstance(TestModule.class, instance);",
                        "       return instance;",
                        "   }",
                        "",
                        "   @NonNull",
                        "   public static final TestModule get(@NonNull String arg1, String arg2) {",
                        "       TestModule instance = DI.getInstance().getSingletonInstance(TestModule.class);",
                        "       if (instance != null) {",
                        "          return instance;",
                        "       }",
                        "       instance = TestModule.provide(arg1, arg2);",
                        "       DI.getInstance().addSingletonInstance(TestModule.class, instance);",
                        "       return instance;",
                        "   }",
                        "",
                        "   @Nullable",
                        "   public static final TestModule getSingleton() {",
                        "      return DI.getInstance().getSingletonInstance(TestModule.class);",
                        "   }",
                        "}"
                )
        );

        Truth.assert_()
                .about(JavaSourcesSubjectFactory.javaSources())
                .that(Arrays.asList(input, getDIFile()))
                .processedWith(new DIProcessor())
                .compilesWithoutError()
                .and()
                .generatesSources(output);
    }

    @Test
    public void testModuleProviderPriority()
    {
        final JavaFileObject input = JavaFileObjects.forSourceString(
                "com.batch.android.TestModule",
                Joiner.on("\n").join(
                        "package com.batch.android;",
                        "",
                        "import androidx.annotation.NonNull;",
                        "import com.batch.android.processor.Module;",
                        "import com.batch.android.processor.Singleton;",
                        "import com.batch.android.processor.Provide;",
                        "",
                        "@Module",
                        "@Singleton",
                        "public class TestModule {",
                        "   public TestModule(@NonNull String arg1) {}",
                        "",
                        "   @Provide",
                        "   public static TestModule provide(@NonNull String arg1, Integer arg2) {",
                        "      return new TestModule(arg1);",
                        "   }",
                        "}"
                )
        );

        final JavaFileObject output = JavaFileObjects.forSourceString(
                "com.batch.android.di.providers.TestModuleProvider",
                Joiner.on("\n").join(
                        "package com.batch.android.di.providers;",
                        "",
                        "import androidx.annotation.NonNull;",
                        "import androidx.annotation.Nullable;",
                        "import com.batch.android.TestModule;",
                        "import com.batch.android.di.DI;",
                        "import java.lang.Integer;",
                        "import java.lang.String;",
                        "",
                        "public final class TestModuleProvider {",
                        "   @NonNull",
                        "   public static final TestModule get(@NonNull String arg1, Integer arg2) {",
                        "       TestModule instance = DI.getInstance().getSingletonInstance(TestModule.class);",
                        "       if (instance != null) {",
                        "          return instance;",
                        "       }",
                        "       instance = TestModule.provide(arg1, arg2);",
                        "       DI.getInstance().addSingletonInstance(TestModule.class, instance);",
                        "       return instance;",
                        "   }",
                        "",
                        "   @Nullable",
                        "   public static final TestModule getSingleton() {",
                        "      return DI.getInstance().getSingletonInstance(TestModule.class);",
                        "   }",
                        "}"
                )
        );

        Truth.assert_()
                .about(JavaSourcesSubjectFactory.javaSources())
                .that(Arrays.asList(input, getDIFile()))
                .processedWith(new DIProcessor())
                .compilesWithoutError()
                .and()
                .generatesSources(output);
    }

    @Test
    public void testNotPublicModule()
    {
        final JavaFileObject input = JavaFileObjects.forSourceString(
                "com.batch.android.TestModule",
                Joiner.on("\n").join(
                        "package com.batch.android;",
                        "",
                        "import com.batch.android.processor.Module;",
                        "",
                        "@Module",
                        "class TestModule {}"
                )
        );

        Truth.assert_()
                .about(JavaSourcesSubjectFactory.javaSources())
                .that(Collections.singletonList(input))
                .processedWith(new DIProcessor())
                .compilesWithoutError()
                .withWarningContaining("class annotated with @Module must be public");
    }

    @Test
    public void testNotPublicConstructorModule()
    {
        final JavaFileObject input = JavaFileObjects.forSourceString(
                "com.batch.android.TestModule",
                Joiner.on("\n").join(
                        "package com.batch.android;",
                        "",
                        "import com.batch.android.processor.Module;",
                        "",
                        "@Module",
                        "public class TestModule {",
                        "   private TestModule() {}",
                        "}"
                )
        );

        Truth.assert_()
                .about(JavaSourcesSubjectFactory.javaSources())
                .that(Collections.singletonList(input))
                .processedWith(new DIProcessor())
                .compilesWithoutError()
                .withWarningContaining(
                        "class annotated with @Module must have at least one public constructor or one @Provide method");
    }

    /**
     * Create a fake DI class matching the one in the SDK, to allow the processor to run
     *
     * @return
     */
    private JavaFileObject getDIFile()
    {
        return JavaFileObjects.forSourceString(
                "com.batch.android.di.DI",
                Joiner.on("\n").join(
                        "package com.batch.android.di;",
                        "",
                        "public final class DI {",
                        "   public static DI getInstance() {",
                        "       return null;",
                        "   }",
                        "",
                        "   public synchronized <T> T getSingletonInstance(Class<T> key) {",
                        "      return null;",
                        "   }",
                        "",
                        "   public synchronized <T> void addSingletonInstance(Class<T> key, T instance) {}",
                        "}"
                ));
    }

}
