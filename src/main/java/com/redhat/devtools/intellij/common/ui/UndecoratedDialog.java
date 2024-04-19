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
package com.redhat.devtools.intellij.common.ui;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonShortcuts;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.wm.impl.IdeGlassPaneEx;
import com.intellij.ui.PopupBorder;
import com.intellij.ui.WindowMoveListener;
import com.intellij.ui.WindowResizeListener;
import com.intellij.util.Consumer;
import com.intellij.util.ui.JBUI;
import java.awt.Component;
import java.awt.Window;
import java.util.stream.Stream;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

abstract public class UndecoratedDialog extends DialogWrapper {

  public UndecoratedDialog(@Nullable Project project,
                           @Nullable Component parentComponent,
                           boolean canBeParent,
                           @NotNull IdeModalityType ideModalityType,
                           boolean createSouth) {
    super(project, parentComponent, canBeParent, ideModalityType, createSouth);
    init();
  }

  @Override
  protected void init() {
    super.init();
    setUndecorated(true);
    setBorders();
  }

  protected void closeImmediately() {
    if (isVisible()) {
      doCancelAction();
    }
  }

  protected void registerEscapeShortcut(Consumer<AnActionEvent> onEscape) {
    AnAction escape = ActionManager.getInstance().getAction("EditorEscape");
    DumbAwareAction
      .create(onEscape)
      .registerCustomShortcutSet(
        escape == null ? CommonShortcuts.ESCAPE : escape.getShortcutSet(),
        getRootPane(),
        getDisposable());
  }

  protected void setGlassPaneResizable() {
    WindowResizeListener resizeListener = new WindowResizeListener(getRootPane(), JBUI.insets(10), null);
    IdeGlassPaneEx glassPane = (IdeGlassPaneEx) getRootPane().getGlassPane();
    glassPane.addMousePreprocessor(resizeListener, getDisposable());
    glassPane.addMouseMotionPreprocessor(resizeListener, getDisposable());
  }

  protected void setMovableUsing(JComponent... movableComponents) {
    WindowMoveListener windowMoveListener = new WindowMoveListener(getRootPane());
    Stream.of(movableComponents).forEach(
      component -> component.addMouseListener(windowMoveListener));
  }

  private void setBorders() {
    getRootPane().setBorder(PopupBorder.Factory.create(true, true));
    getRootPane().setWindowDecorationStyle(JRootPane.NONE);
  }

}
