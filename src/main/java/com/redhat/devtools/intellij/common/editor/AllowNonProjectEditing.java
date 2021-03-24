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
package com.redhat.devtools.intellij.common.editor;

import com.intellij.openapi.fileEditor.impl.NonProjectFileWritingAccessExtension;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class AllowNonProjectEditing implements NonProjectFileWritingAccessExtension {

    /**
     * Prevents the dialog "Non-Projects File Protection" when editing files that do not belong the the current project.
     */
    public static final Key<Boolean> ALLOW_NON_PROJECT_EDITING = new Key("ALLOW_EDITING");

    @Override
    public boolean isWritable(@NotNull VirtualFile file) {
        // allow writing if file has 'true' in user data
        return Boolean.TRUE.equals(file.getUserData(ALLOW_NON_PROJECT_EDITING));
    }
}