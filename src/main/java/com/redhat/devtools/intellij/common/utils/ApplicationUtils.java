/*******************************************************************************
 * Copyright (c) 2024 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.common.utils;

import com.intellij.openapi.application.ApplicationManager;

import java.util.concurrent.Executor;

public class ApplicationUtils {

  public static final Executor UI_EXECUTOR = (Runnable runnable) ->
    ApplicationManager.getApplication().invokeLater(runnable);

  public static final Executor PLATFORM_EXECUTOR = (Runnable runnable) ->
    ApplicationManager.getApplication().executeOnPooledThread(runnable);

}
