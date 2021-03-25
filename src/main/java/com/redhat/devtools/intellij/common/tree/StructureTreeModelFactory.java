/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.common.tree;

import com.intellij.ide.util.treeView.AbstractTreeStructure;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.tree.StructureTreeModel;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class StructureTreeModelFactory {

	/**
	 * Builds the {@link StructureTreeModel} through reflection as StructureTreeModel does not have a stable API.
	 *
	 * @param structure the structure to associate
	 * @param project   the IJ project
	 * @return the build model
	 * @throws IllegalAccessException if the model class could not be loaded
	 * @throws InvocationTargetException if the model class could not be loaded
	 * @throws InstantiationException if the model could not be instantiated
	 * @throws NoSuchMethodException if the model class could not be loaded
	 */
	public static StructureTreeModel<AbstractTreeStructure> create(AbstractTreeStructure structure, Project project)
			throws IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchMethodException {
		try {
			Constructor<StructureTreeModel> constructor = StructureTreeModel.class.getConstructor(AbstractTreeStructure.class);
			return constructor.newInstance(structure);
		} catch (NoSuchMethodException e) {
			// IC 2019.3+
			Constructor<StructureTreeModel> constructor = StructureTreeModel.class.getConstructor(AbstractTreeStructure.class, Disposable.class);
			return constructor.newInstance(structure, project);
		}
	}
}