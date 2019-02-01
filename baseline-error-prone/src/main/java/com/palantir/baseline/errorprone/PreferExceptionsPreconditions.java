/*
 * (c) Copyright 2019 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.baseline.errorprone;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.CompileTimeConstantExpressionMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.matchers.method.MethodMatchers;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.SideEffectAnalysis;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@AutoService(BugChecker.class)
@BugPattern(
        name = "PreferExceptionsPreconditions",
        link = "https://github.com/palantir/gradle-baseline#baseline-error-prone-checks",
        linkType = BugPattern.LinkType.CUSTOM,
        category = BugPattern.Category.ONE_OFF,
        severity = BugPattern.SeverityLevel.SUGGESTION,
        summary = "Boolean precondition and similar checks with messages requiring concatenation and no params should "
                + "use if statements to avoid unnecessary string operations.")
public final class PreferExceptionsPreconditions extends BugChecker implements BugChecker.MethodInvocationTreeMatcher {

    private static final long serialVersionUID = 1L;

    private final Matcher<ExpressionTree> compileTimeConstExpressionMatcher =
            new CompileTimeConstantExpressionMatcher();

    private static final Matcher<ExpressionTree> METHOD_MATCHER =
            Matchers.anyOf(
                    MethodMatchers.staticMethod()
                            .onClass("com.google.common.base.Preconditions")
                            .withNameMatching(Pattern.compile("checkArgument|checkState|checkNotNull")),
                    MethodMatchers.staticMethod()
                            .onClass("java.util.Objects")
                            .named("requireNonNull"),
                    MethodMatchers.staticMethod()
                            .onClass("org.apache.commons.lang3.Validate")
                            .withNameMatching(Pattern.compile("isTrue|notNull|validState")));

    private static final Map<String, String> TRANSLATION_EXCEPTIONS = ImmutableMap.<String, String>builder()
            .put("checkArgument", "IllegalArgumentException")
            .put("checkState", "IllegalStateException")
            .put("checkNotNull", "NullPointerException")
            .put("requireNonNull", "NullPointerException")
            .put("isTrue", "IllegalArgumentException")
            .put("notNull", "NullPointerException")
            .put("validState", "IllegalStateException")
            .build();

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
        if (!METHOD_MATCHER.matches(tree, state)) {
            return Description.NO_MATCH;
        }

        List<? extends ExpressionTree> args = tree.getArguments();
        if (args.size() != 2) {
            return Description.NO_MATCH;
        }

        ExpressionTree messageArg = args.get(1);
        boolean isStringType = ASTHelpers.isSameType(
                ASTHelpers.getType(messageArg),
                state.getTypeFromString("java.lang.String"),
                state);
        if (!isStringType || compileTimeConstExpressionMatcher.matches(messageArg, state)) {
            return Description.NO_MATCH;
        }

        if (TestCheckUtils.isTestCode(state)) {
            return Description.NO_MATCH;
        }

        ExpressionTree conditionArg = args.get(0);
        if (SideEffectAnalysis.hasSideEffect(messageArg)) {
            return Description.NO_MATCH;
        }

        String methodName = ASTHelpers.getSymbol(tree.getMethodSelect()).name.toString();

        return buildDescription(tree)
                .setMessage("The call can be replaced with an if statement that throws the proper exception to save "
                        + "doing unnecessary string operations.")
                .addFix(SuggestedFix.builder()
                        .replace(tree, buildIfStatementReplacement(methodName, conditionArg.toString(),
                                messageArg.toString()))
                        .build())
                .build();
    }

    private String buildIfStatementReplacement(String methodName, String condition, String message) {
        String exception = TRANSLATION_EXCEPTIONS.get(methodName);
        if (methodName.toLowerCase().contains("null")) {
            return String.format("if (%s == null) throw new %s(%s)", condition, exception, message);
        } else {
            return String.format("if (!(%s)) throw new %s(%s)", condition, exception, message);
        }
    }
}
