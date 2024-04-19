/*******************************************************************************
 * Copyright (c) 2024 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc.
 ******************************************************************************/
package com.redhat.devtools.intellij.common.utils;

import com.intellij.openapi.actionSystem.AnActionEvent;

import java.awt.Point;
import java.awt.event.MouseEvent;

public class SwingUtils {

  private SwingUtils() {
  }

  /**
   * Returns the location of the mouse for a given {@link AnActionEvent}.
   * Returns {@code null} if the location could not be determined.
   *
   * @param actionEvent the action event to retrieve the mouse location from
   * @return the mouse location
   */
  public static Point getMouseLocation(AnActionEvent actionEvent) {
    Point location = null;
    MouseEvent event = ((MouseEvent) actionEvent.getInputEvent());
    if (event != null) {
      location = event.getLocationOnScreen();
    }
    return location;
  }
}
