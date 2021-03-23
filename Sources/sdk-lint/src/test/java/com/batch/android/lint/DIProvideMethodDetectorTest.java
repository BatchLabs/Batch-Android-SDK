package com.batch.android.lint;

import com.android.tools.lint.checks.infrastructure.LintDetectorTest;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Issue;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class DIProvideMethodDetectorTest extends LintDetectorTest
{

    @Test
    public void testMethodCall()
    {
        lint().files(
                java("package com.batch.android;\n" +
                        "public class TestModule {\n" +
                        "    @com.batch.android.processor.Provide" +
                        "    public static void provide() {\n" +
                        "        return;" +
                        "    }" +
                        "}"),
                java("package com.batch.android;\n" +
                        "public class TestClass {\n" +
                        "    public static void testMethod() {\n" +
                        "        TestModule.provide();" +
                        "    }" +
                        "}"))
                .run()
                .expect("src/com/batch/android/TestClass.java:4: Error: Incorrect call to method annotated with @Provide [DIProvideMethodDetector]\n" +
                        "        TestModule.provide();    }}\n" +
                        "        ~~~~~~~~~~~~~~~~~~~~\n" +
                        "1 errors, 0 warnings\n");
    }

    @Override
    protected Detector getDetector()
    {
        return new DIProvideMethodDetector();
    }

    @Override
    protected List<Issue> getIssues()
    {
        return Collections.singletonList(DIProvideMethodDetector.ISSUE);
    }
}