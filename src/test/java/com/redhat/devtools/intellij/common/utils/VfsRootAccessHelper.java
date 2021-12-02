/*******************************************************************************
 * Copyright (c) 2021 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.common.utils;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class VfsRootAccessHelper {
    private VfsRootAccessHelper() {
    }

    public void allowRootAccess(Disposable disposable, String ... roots) {
        if (disposable == null) {
            disposable = () -> {};
        }
        try {
            Method method = Arrays.stream(VfsRootAccess.class.getDeclaredMethods()).filter(m -> m.getName().equals("allowRootAccess")).findFirst().get();
            if (method.getParameterCount() > 1) {
                method.invoke(null, disposable , roots);
            } else {
                method.q(null, roots);
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public void allowRootAccess(String ... roots) {
        allowRootAccess(null, roots);
    }

}
