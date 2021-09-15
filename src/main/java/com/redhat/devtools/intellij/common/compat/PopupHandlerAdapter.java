/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.common.compat;

import com.intellij.ui.PopupHandler;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComponent;
import java.lang.reflect.InvocationTargetException;

public class PopupHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PopupHandlerAdapter.class);

    private PopupHandlerAdapter() {
    }

    public static void install(@NotNull JComponent component, @NotNull String groupId, @NotNull String place) throws IllegalAccessException, InvocationTargetException {
        try {
            // < IC-2022.1
            MethodUtils.invokeStaticMethod(PopupHandler.class, "installPopupHandler",
                    new Object[]{component, groupId, place},
                    new Class[]{JComponent.class, String.class, String.class});
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            // >= IC-2022.1
            try {
                MethodUtils.invokeStaticMethod(PopupHandler.class, "installPopupMenu",
                        new Object[]{component, groupId, place},
                        new Class[]{JComponent.class, String.class, String.class});
            } catch (NoSuchMethodException ex) {
                LOGGER.warn("Could not install pop up handler: neither PopupHandler.installPopupHandler nor PopupHandler.installPopupMenu found", e);
            }
        }
    }
}
