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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Before;
import org.junit.Test;

public final class PreferExceptionsPreconditionsTests {
    private CompilationTestHelper compilationHelper;

    @Before
    public void before() {
        compilationHelper = CompilationTestHelper.newInstance(PreferExceptionsPreconditions.class, getClass());
    }

    @Test
    public void testPreconditionsCheckArgumentConstantMessage() {
        passGuava("Preconditions.checkArgument(param != \"string\", \"message\");");
    }

    @Test
    public void testPreconditionsCheckArgumentParams() {
        passGuava("Preconditions.checkArgument(param != \"string\", \"message\", \"param\");");
    }

    @Test
    public void testPreconditionsCheckArgumentMessageWithStringConcat() {
        failGuava("Preconditions.checkArgument(param != \"string\", \"message\" + param);");
    }

    @Test
    public void testPreconditionsCheckArgumentMessageWithSideEffects() {
        passGuava("Preconditions.checkArgument(param != \"string\", param += \"message\");");
    }

    @Test
    public void testPreconditionsCheckStateConstantMessage() {
        passGuava("Preconditions.checkState(param != \"string\", \"message\");");
    }

    @Test
    public void testPreconditionsCheckStateParams() {
        passGuava("Preconditions.checkState(param != \"string\", \"message\", \"param\");");
    }

    @Test
    public void testPreconditionsCheckStateMessageWithStringConcat() {
        failGuava("Preconditions.checkState(param != \"string\", \"message\" + param);");
    }

    @Test
    public void testPreconditionsCheckStateMessageWithSideEffects() {
        passGuava("Preconditions.checkState(param != \"string\", param += \"message\");");
    }

    @Test
    public void testPreconditionsCheckNotNullConstantMessage() {
        passGuava("Preconditions.checkNotNull(param, \"message\");");
    }

    @Test
    public void testPreconditionsCheckNotNullParams() {
        passGuava("Preconditions.checkNotNull(param, \"message\", \"param\");");
    }

    @Test
    public void testPreconditionsCheckNotNullMessageWithStringConcat() {
        failGuava("Preconditions.checkNotNull(param, \"message\" + param);");
    }

    @Test
    public void testPreconditionsCheckNotNullMessageWithSideEffects() {
        passGuava("Preconditions.checkNotNull(param, param += \"message\");");
    }

    @Test
    public void testObjectRequireNonNullConstantMessage() {
        passObjects("Objects.requireNonNull(param, \"message\");");
    }

    @Test
    public void testObjectRequireNonNullSupplier() {
        passObjects("Objects.requireNonNull(param, () -> \"message\");");
    }

    @Test
    public void testObjectRequireNonNullMessageWithStringConcat() {
        failObjects("Objects.requireNonNull(param, \"message\" + param);");
    }

    @Test
    public void testObjectRequireNonNullMessageWithSideEffects() {
        passObjects("Objects.requireNonNull(param, param += \"message\");");
    }

    @Test
    public void testValidateIsTrueConstantMessage() {
        passValidate("Validate.isTrue(param != \"string\", \"message\");");
    }

    @Test
    public void testValidateIsTrueParams() {
        passValidate("Validate.isTrue(param != \"string\", \"message\", \"param\");");
    }

    @Test
    public void testValidateIsTrueMessageWithStringConcat() {
        failValidate("Validate.isTrue(param != \"string\", \"message\" + param);");
    }

    @Test
    public void testValidateIsTrueMessageWithSideEffects() {
        passValidate("Validate.isTrue(param != \"string\", param += \"message\");");
    }

    @Test
    public void testValidateValidStateConstantMessage() {
        passValidate("Validate.validState(param != \"string\", \"message\");");
    }

    @Test
    public void testValidateValidStateParams() {
        passValidate("Validate.validState(param != \"string\", \"message\", \"param\");");
    }

    @Test
    public void testValidateValidStateMessageWithStringConcat() {
        failValidate("Validate.validState(param != \"string\", \"message\" + param);");
    }

    @Test
    public void testValidateValidStateMessageWithSideEffects() {
        passValidate("Validate.validState(param != \"string\", param += \"message\");");
    }

    @Test
    public void testValidateNotNullConstantMessage() {
        passValidate("Validate.notNull(param, \"message\");");
    }

    @Test
    public void testValidateNotNullParams() {
        passValidate("Validate.notNull(param, \"message\", \"param\");");
    }

    @Test
    public void testValidateNotNullMessageWithStringConcat() {
        failValidate("Validate.notNull(param, \"message\" + param);");
    }

    @Test
    public void testValidateNotNullMessageWithSideEffects() {
        passValidate("Validate.notNull(param, param += \"message\");");
    }

    @Test
    public void testPreconditionsFixes() {
        BugCheckerRefactoringTestHelper.newInstance(new PreferExceptionsPreconditions(), getClass()).addInputLines(
                "Test.java",
                "import com.google.common.base.Preconditions;",
                "class Test {",
                "  void f(String param) {",
                "    Preconditions.checkArgument(param != \"string\", \"constant\" + param);",
                "    Preconditions.checkState(param != \"string\", \"constant\" + param);",
                "    Preconditions.checkNotNull(param, \"constant\" + param);",
                "  }",
                "}").addOutputLines(
                "Test.java",
                "import com.google.common.base.Preconditions;",
                "class Test {",
                "  void f(String param) {",
                "    if (!(param != \"string\")) throw new IllegalArgumentException(\"constant\" + param);",
                "    if (!(param != \"string\")) throw new IllegalStateException(\"constant\" + param);",
                "    if (param == null) throw new NullPointerException(\"constant\" + param);",
                "  }",
                "}").doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
    }

    @Test
    public void testObjectsFixes() {
        BugCheckerRefactoringTestHelper.newInstance(new PreferExceptionsPreconditions(), getClass()).addInputLines(
                "Test.java",
                "import java.util.Objects;",
                "class Test {",
                "  void f(String param) {",
                "    Objects.requireNonNull(param, \"constant\" + param);",
                "  }",
                "}").addOutputLines(
                "Test.java",
                "import java.util.Objects;",
                "class Test {",
                "  void f(String param) {",
                "    if (param == null) throw new NullPointerException(\"constant\" + param);",
                "  }",
                "}").doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
    }

    @Test
    public void testValidateFixes() {
        BugCheckerRefactoringTestHelper.newInstance(new PreferExceptionsPreconditions(), getClass()).addInputLines(
                "Test.java",
                "import org.apache.commons.lang3.Validate;",
                "class Test {",
                "  void f(String param) {",
                "    Validate.isTrue(param != \"string\", \"constant\" + param);",
                "    Validate.validState(param != \"string\", \"constant\" + param);",
                "    Validate.notNull(param, \"constant\" + param);",
                "  }",
                "}").addOutputLines(
                "Test.java",
                "import org.apache.commons.lang3.Validate;",
                "class Test {",
                "  void f(String param) {",
                "    if (!(param != \"string\")) throw new IllegalArgumentException(\"constant\" + param);",
                "    if (!(param != \"string\")) throw new IllegalStateException(\"constant\" + param);",
                "    if (param == null) throw new NullPointerException(\"constant\" + param);",
                "  }",
                "}").doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
    }

    private void passObjects(String precondition) {
        compilationHelper.addSourceLines(
                "Test.java",
                "import java.util.Objects;",
                "class Test {",
                "  void f(String param) {",
                "    " + precondition,
                "  }",
                "}")
                .doTest();
    }

    private void passGuava(String precondition) {
        compilationHelper.addSourceLines(
                "Test.java",
                "import com.google.common.base.Preconditions;",
                "class Test {",
                "  void f(String param) {",
                "    " + precondition,
                "  }",
                "}")
                .doTest();
    }

    private void passValidate(String precondition) {
        compilationHelper.addSourceLines(
                "Test.java",
                "import org.apache.commons.lang3.Validate;",
                "class Test {",
                "  void f(String param) {",
                "    " + precondition,
                "  }",
                "}")
                .doTest();
    }

    private void failObjects(String precondition) {
        compilationHelper.addSourceLines(
                "Test.java",
                "import java.util.Objects;",
                "class Test {",
                "  void f(String param) {",
                "    // BUG: Diagnostic contains: call can be replaced",
                "    " + precondition,
                "  }",
                "}")
                .doTest();
    }

    private void failGuava(String precondition) {
        compilationHelper.addSourceLines(
                "Test.java",
                "import com.google.common.base.Preconditions;",
                "class Test {",
                "  void f(String param) {",
                "    // BUG: Diagnostic contains: call can be replaced",
                "    " + precondition,
                "  }",
                "}")
                .doTest();
    }

    private void failValidate(String precondition) {
        compilationHelper.addSourceLines(
                "Test.java",
                "import org.apache.commons.lang3.Validate;",
                "class Test {",
                "  void f(String param) {",
                "    // BUG: Diagnostic contains: call can be replaced",
                "    " + precondition,
                "  }",
                "}")
                .doTest();
    }
}
