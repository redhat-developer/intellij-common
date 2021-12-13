/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc.
 ******************************************************************************/
package com.redhat.devtools.intellij.common.utils;

import com.intellij.psi.PsiElement;
import org.jetbrains.yaml.psi.YAMLQuotedText;

public class StringHelper {

    public static String beautify(String text) {
        return text.length() > 16 ? text.substring(0, 16) + ".." : text;
    }

    public static String getPlural(String kind) {
        kind = kind.toLowerCase();
        if (kind.endsWith("s")) {
            return kind + "es";
        } else if (kind.endsWith("y")) {
            return kind.substring(0, kind.length() - 1) + "ies";
        } else {
            return kind + "s";
        }
    }

    public static String getUnquotedValueFromPsi(PsiElement element) {
        if (element instanceof YAMLQuotedText) {
            return ((YAMLQuotedText) element).getTextValue();
        }
        return element.getText();
    }
}
