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
package com.redhat.devtools.intellij.common.utils.terminal;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.terminal.JBTerminalSystemSettingsProviderBase;
import com.jediterm.terminal.ui.TerminalWidgetListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;

public class JCommonTerminalWidget extends ShellTerminalWidget {
    private boolean keepTabOpened;

    /**
     * Wrapper to change the normal behavior of shellTerminalWidget
     *
     * @param project project
     * @param settingsProvider settingProvider
     * @param parent parent
     * @param readOnly if the terminal tab has to be read-only and the command doesn't require any user interaction
     * @param keepTabOpened if true the tab will be stay opened forever, only the user can manually close it. If false
     *                      the tab will be closed as soon as the command ends its execution
     */
    public JCommonTerminalWidget(@NotNull Project project,
                                 @NotNull JBTerminalSystemSettingsProviderBase settingsProvider,
                                 @NotNull Disposable parent,
                                 boolean readOnly,
                                 boolean keepTabOpened) {
        super(project, settingsProvider, parent);
        this.keepTabOpened = keepTabOpened;
        if (readOnly) {
            setEnabled(false);
        }
    }

    @Override
    public void addListener(TerminalWidgetListener listener) {
        // This is used to override the default addListener and prevent IJ to add a listener that closes
        // the terminal tab when the process ends.
        if (!keepTabOpened) {
            super.addListener(listener);
        }
    }
}
