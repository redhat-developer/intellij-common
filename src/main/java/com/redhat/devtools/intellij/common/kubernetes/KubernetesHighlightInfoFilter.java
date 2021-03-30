/*******************************************************************************
 * Copyright (c) 2021 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc.
 ******************************************************************************/
package com.redhat.devtools.intellij.common.kubernetes;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.HighlightInfoFilter;
import com.intellij.psi.PsiFile;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class KubernetesHighlightInfoFilter implements HighlightInfoFilter {
    /*
     * as there is no way to find the originating plugin from an highlight info, we are looking at the
     * tooltip that relates to the inspection tool that generated it.
     */
    private static final Pattern TOOLTIP_REGEXP = Pattern.compile(".*href=\"#inspection\\/Kubernetes.*\".*");

    @Override
    public boolean accept(@NotNull HighlightInfo highlightInfo, @Nullable PsiFile file) {
        return !isKubernetesHighlight(highlightInfo) || !isCustomFile(file);
    }

    public abstract boolean isCustomFile(PsiFile file);

    private boolean isKubernetesHighlight(HighlightInfo highlightInfo) {
        return highlightInfo.getToolTip() != null && TOOLTIP_REGEXP.matcher(highlightInfo.getToolTip()).matches();
    }
}
