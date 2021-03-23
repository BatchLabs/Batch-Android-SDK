package com.batch.android.lint;

import com.android.tools.lint.client.api.IssueRegistry;
import com.android.tools.lint.detector.api.ApiKt;
import com.android.tools.lint.detector.api.Issue;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public class BatchIssueRegistry extends IssueRegistry
{

    @Override
    public int getApi()
    {
        return ApiKt.CURRENT_API;
    }

    @NotNull
    @Override
    public List<Issue> getIssues()
    {
        return Collections.singletonList(DIProvideMethodDetector.ISSUE);
    }

}
