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

import java.util.ArrayList;
import java.util.List;

public class MutableModelSupport<T> implements MutableModel<T> {
    private final List<Listener<T>> listeners = new ArrayList<>();

    @Override
    public void fireAdded(T element) {
        listeners.forEach(listener -> listener.onAdded(element));
    }

    @Override
    public void fireModified(T element) {
        listeners.forEach(listener -> listener.onModified(element));
    }

    @Override
    public void fireRemoved(T element) {
        listeners.forEach(listener -> listener.onRemoved(element));
    }

    @Override
    public void addListener(Listener<T> listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(Listener<T> listener) {
        listeners.remove(listener);
    }
}
