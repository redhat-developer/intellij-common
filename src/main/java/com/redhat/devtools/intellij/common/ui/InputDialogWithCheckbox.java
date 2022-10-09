/*******************************************************************************
 * Copyright (c) 2022 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc.
 ******************************************************************************/
package com.redhat.devtools.intellij.common.ui;

import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.NlsSafe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/**
 * This class is a copy of the private Jetbrains class InputDialogWithCheckbox
 * The difference is that this class allows to customize the behavior of the textfield based on the checkbox state
 * (enable/disable it when the checkbox is selected)
 */
public class InputDialogWithCheckbox extends Messages.InputDialog {
    private JCheckBox myCheckBox;

    public InputDialogWithCheckbox(@NlsContexts.DialogMessage String message,
                            @NlsContexts.DialogTitle String title,
                            @NlsContexts.Checkbox String checkboxText,
                            boolean checked,
                            boolean checkboxEnabled,
                            @Nullable Icon icon,
                            @Nullable @NlsSafe String initialValue,
                            @Nullable InputValidator validator) {
        super(message, title, icon, initialValue, validator);
        myCheckBox.setText(checkboxText);
        myCheckBox.setSelected(checked);
        myCheckBox.setEnabled(checkboxEnabled);
    }

    @NotNull
    @Override
    protected JPanel createMessagePanel() {
        JPanel messagePanel = new JPanel(new BorderLayout());
        if (myMessage != null) {
            JComponent textComponent = createTextComponent();
            messagePanel.add(textComponent, BorderLayout.NORTH);
        }

        myField = createTextFieldComponent();
        messagePanel.add(createScrollableTextComponent(), BorderLayout.CENTER);

        myCheckBox = new JCheckBox();
        messagePanel.add(myCheckBox, BorderLayout.SOUTH);

        return messagePanel;
    }

    public JCheckBox getMyCheckBox() {
        return myCheckBox;
    }

    /**
     * Enable the textbox field when the checkbox is checked
     */
    public void setEnableTextPanelWithCheckbox() {
        updateTextFieldState(myCheckBox.isSelected());
        myCheckBox.addItemListener(e -> updateTextFieldState(myCheckBox.isSelected()));
    }

    /**
     * Disable the textbox field when the checkbox is checked
     */
    public void setDisableTextPanelWithCheckbox() {
        updateTextFieldState(!myCheckBox.isSelected());
        myCheckBox.addItemListener(e -> updateTextFieldState(!myCheckBox.isSelected()));
    }

    private void updateTextFieldState(boolean enabled) {
        myField.setEnabled(enabled);
    }

    public Boolean isChecked() {
        return myCheckBox.isSelected();
    }
}
