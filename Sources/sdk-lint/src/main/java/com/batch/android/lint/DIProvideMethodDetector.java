package com.batch.android.lint;

import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiMethod;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UCallExpression;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

public class DIProvideMethodDetector extends Detector implements Detector.UastScanner
{

    static final Issue ISSUE = Issue.create(
            "DIProvideMethodDetector",
            "Incorrect call to method annotated with @Provide",
            "Use the Provider#get() method instead.",
            Category.CORRECTNESS,
            8,
            Severity.ERROR,
            new Implementation(DIProvideMethodDetector.class, EnumSet.of(Scope.JAVA_FILE)));


    @Nullable
    @Override
    public List<String> getApplicableMethodNames()
    {
        return Collections.singletonList("provide");
    }


    @Override
    public void visitMethodCall(@NotNull JavaContext context,
                                @NotNull UCallExpression node,
                                @NotNull PsiMethod method)
    {
        super.visitMethodCall(context, node, method);

        PsiAnnotation[] annotations = method.getAnnotations();
        for (PsiAnnotation annotation : annotations) {
            System.out.println("ANNOTATION: " + annotation.getQualifiedName());
            if (annotation.hasQualifiedName("com.batch.android.processor.Provide")) {
                context.report(
                        ISSUE,
                        context.getCallLocation(node, true, true),
                        "Incorrect call to method annotated with @Provide",
                        null
                );
            }
        }
    }
}
