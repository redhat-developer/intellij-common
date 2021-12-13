package com.redhat.devtools.intellij.common.utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.intellij.common.FixtureBaseTest;
import org.jetbrains.yaml.YAMLFileType;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class StringHelperTest extends FixtureBaseTest {

    @Test
    public void GetUnquotedValueFromPsi_DoubleQuotedValue_ValueWithoutQuotes() throws IOException {
        String content = "\"foo\"";
        PsiFile psiFile = myFixture.configureByText(YAMLFileType.YML, content);
        ApplicationManager.getApplication().runReadAction(() -> {
            PsiElement result = psiFile.getFirstChild().getLastChild();
            assertEquals("foo", StringHelper.getUnquotedValueFromPsi(result));
        });
    }

    @Test
    public void GetUnquotedValueFromPsi_SingleQuotedValue_ValueWithoutQuotes() throws IOException {
        String content = "'foo'";
        PsiFile psiFile = myFixture.configureByText(YAMLFileType.YML, content);
        ApplicationManager.getApplication().runReadAction(() -> {
            PsiElement result = psiFile.getFirstChild().getLastChild();
            assertEquals("foo", StringHelper.getUnquotedValueFromPsi(result));
        });
    }

    @Test
    public void GetUnquotedValueFromPsi_UnquotedValue_ValueAsIs() throws IOException {
        String content = "foo";
        PsiFile psiFile = myFixture.configureByText(YAMLFileType.YML, content);
        ApplicationManager.getApplication().runReadAction(() -> {
            PsiElement result = psiFile.getFirstChild().getLastChild();
            assertEquals("foo", StringHelper.getUnquotedValueFromPsi(result));
        });
    }
}
