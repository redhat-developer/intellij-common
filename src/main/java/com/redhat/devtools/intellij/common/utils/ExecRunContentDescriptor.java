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
package com.redhat.devtools.intellij.common.utils;

import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.execution.ui.RunContentDescriptor;
import javax.swing.JComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExecRunContentDescriptor extends RunContentDescriptor {

    public ExecRunContentDescriptor(@Nullable ExecutionConsole executionConsole, @Nullable ProcessHandler processHandler, @NotNull JComponent component, String displayName) {
        super(executionConsole, processHandler, component, displayName);
    }

    @Override
    public boolean isContentReuseProhibited() {
        return true;
    }
}
