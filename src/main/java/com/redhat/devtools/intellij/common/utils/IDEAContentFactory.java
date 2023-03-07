/*******************************************************************************
 * Copyright (c) 2023 Red Hat, Inc.
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
import com.intellij.ui.content.ContentFactory;

public class IDEAContentFactory {

    public static final ContentFactory getInstance() {
        // IC-2022.1 introduced ContentFactory.getInstance(), deprecated ContentFactory.SERVICE.getInstance()
        // this helper can be removed once we support IC-2022.1 as the lowest version
        // and replaced by ContentFactory.getInstance()
        return ApplicationManager.getApplication().getService(ContentFactory.class);
    }

}
