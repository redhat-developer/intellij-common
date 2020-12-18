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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.intellij.openapi.ui.TestDialog;

public class MessagesHelper {
    private static Method method;

    /*
     * Pre 2020.3: Messages.setDialog(TestDialog testDialog) Post 2020.3:
     * TestDialogManager.setTestDialog(TestDialog testDialog)
     * 
     */
    static {
        try {
            Class clazz = Class.forName("com.intellij.openapi.ui.TestDialogManager");
            method = clazz.getMethod("setTestDialog", new Class[] { TestDialog.class });
        } catch (ClassNotFoundException e) {
            try {
                Class clazz = Class.forName("com.intellij.openapi.ui.Messages");
                method = clazz.getMethod("setTestDialog", new Class[] { TestDialog.class });
            } catch (ClassNotFoundException | NoSuchMethodException | SecurityException e1) {
                throw new RuntimeException(e1);
            }
        } catch (NoSuchMethodException | SecurityException e) {
            throw new RuntimeException(e);
        }
    }

    public static TestDialog setTestDialog(TestDialog testDialog) {
        try {
            return (TestDialog) method.invoke(null, testDialog);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
