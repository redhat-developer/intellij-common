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
package com.redhat.devtools.intellij.common.tree;

public interface MutableModel<T> {
    interface Listener<T> {
        void onAdded(T element);
        void onModified(T element);
        void onRemoved(T element);
    }

    void fireAdded(T element);
    void fireModified(T element);
    void fireRemoved(T element);
    void addListener(Listener<T> listener);
    void removeListener(Listener<T> listener);
}
