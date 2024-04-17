/*******************************************************************************
 * Copyright (c) 2022 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc.
 ******************************************************************************/
package com.redhat.devtools.intellij.common.utils;

import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.HyperlinkInfo;
import com.intellij.execution.process.ColoredProcessHandler;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.execution.ui.ObservableConsoleView;
import com.intellij.execution.ui.RunContentManager;
import com.intellij.icons.AllIcons;
import com.intellij.idea.ActionsBundle;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.encoding.EncodingProjectManager;
import com.intellij.terminal.AppendableTerminalDataStream;
import com.intellij.terminal.JBTerminalPanel;
import com.intellij.terminal.JBTerminalSystemSettingsProviderBase;
import com.intellij.terminal.JBTerminalWidget;
import com.intellij.terminal.ProcessHandlerTtyConnector;
import com.intellij.util.LineSeparator;
import com.jediterm.terminal.HyperlinkStyle;
import com.jediterm.terminal.Terminal;
import com.jediterm.terminal.TerminalDataStream;
import com.jediterm.terminal.TerminalStarter;
import com.jediterm.terminal.TtyConnector;
import com.jediterm.terminal.model.JediTerminal;
import com.jediterm.terminal.model.StyleState;
import com.jediterm.terminal.model.TerminalTextBuffer;
import com.jediterm.terminal.ui.settings.SettingsProvider;
import com.pty4j.PtyProcess;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.BoundedRangeModel;
import javax.swing.JComponent;
import java.awt.Color;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *  This class is similar to the TerminalExecutionConsole class by JetBrains (v. 2019.3) with the difference that can
 *  support multiple processes and print their outputs in one tab.
 *  TerminalExecutionConsole could not be extended due to its private AppendableTerminalDataStream and
 *  `printText(@NotNull String text, @Nullable ConsoleViewContentType contentType)` method
 *  which prevented to change the behavior of the terminal from the extender. This class recreates the whole
 *  terminalWidgets and streams to customize their behavior.
 */
public class CommonTerminalExecutionConsole implements ConsoleView, ObservableConsoleView {
    private static final Logger LOG = Logger.getInstance(CommonTerminalExecutionConsole.class);

    private JBTerminalWidget myTerminalWidget;
    private final Project myProject;
    private final String tabTitle;
    private final AppendableTerminalDataStream myDataStream;
    private final AtomicBoolean myAttachedToProcess = new AtomicBoolean(false);
    private volatile boolean myLastCR = false;
    private ProcessHandler processHandler;
    private ProcessAdapter processAdapter;
    private boolean contentToBeStarted = false;
    private static final int ESC = 27;

    private boolean myEnterKeyDefaultCodeEnabled = true;

    public CommonTerminalExecutionConsole(@NotNull Project project, @Nullable ProcessHandler processHandler, String tabTitle) {
        myProject = project;
        this.tabTitle = tabTitle;
        JBTerminalSystemSettingsProviderBase provider = new JBTerminalSystemSettingsProviderBase() {
            @Override
            public HyperlinkStyle.HighlightMode getHyperlinkHighlightingMode() {
                return HyperlinkStyle.HighlightMode.ALWAYS;
            }
        };
        myDataStream = new AppendableTerminalDataStream();
        myTerminalWidget = new ConsoleTerminalWidget(project, provider);
        if (processHandler != null) {
            attachToProcess(processHandler);
        }
    }

    private void printText(@NotNull String text, @Nullable ConsoleViewContentType contentType) throws IOException {
        Color foregroundColor = contentType != null ? contentType.getAttributes().getForegroundColor() : null;
        if (foregroundColor != null) {
            myDataStream.append(encodeColor(foregroundColor));
        }

        myDataStream.append(text);

        if (foregroundColor != null) {
            myDataStream.append((char) ESC + "[39m"); //restore default foreground color
        }
    }

    @NotNull
    private static String encodeColor(@NotNull Color color) {
        return ((char)ESC) + "[" + "38;2;" + color.getRed() + ";" + color.getGreen() + ";" +
                color.getBlue() + "m";
    }

    /**
     * @param enabled the auto new line flag
     * @deprecated use {@link #withEnterKeyDefaultCodeEnabled(boolean)}
     */
    @Deprecated
    public void setAutoNewLineMode(@SuppressWarnings("unused") boolean enabled) {
    }

    @NotNull
    public CommonTerminalExecutionConsole withEnterKeyDefaultCodeEnabled(boolean enterKeyDefaultCodeEnabled) {
        myEnterKeyDefaultCodeEnabled = enterKeyDefaultCodeEnabled;
        return this;
    }

    /**
     *
     * @param project the project
     * @param filter the filter to use
     * @deprecated use {{@link #addMessageFilter(Filter)}} instead
     */
    @ApiStatus.ScheduledForRemoval(inVersion = "2020.3")
    @Deprecated
    public void addMessageFilter(Project project, Filter filter) {
        myTerminalWidget.addMessageFilter(filter);
    }

    @Override
    public void print(@NotNull String text, @NotNull ConsoleViewContentType contentType) {
        // Convert line separators to CRLF to behave like ConsoleViewImpl.
        // For example, stacktraces passed to com.intellij.execution.testframework.sm.runner.SMTestProxy.setTestFailed have
        // only LF line separators on Unix.
        String textCRLF = convertTextToCRLF(text);
        try {
            printText(textCRLF, contentType);
        }
        catch (IOException e) {
            LOG.info(e);
        }
    }

    @NotNull
    private String convertTextToCRLF(@NotNull String text) {
        if (text.isEmpty()) return text;
        // Handle the case when \r and \n are in different chunks: "text1 \r" and "\n text2"
        boolean preserveFirstLF = text.startsWith(LineSeparator.LF.getSeparatorString()) && myLastCR;
        boolean preserveLastCR = text.endsWith(LineSeparator.CR.getSeparatorString());
        myLastCR = preserveLastCR;
        String textToConvert = text.substring(preserveFirstLF ? 1 : 0, preserveLastCR ? text.length() - 1 : text.length());
        String textCRLF = StringUtil.convertLineSeparators(textToConvert, LineSeparator.CRLF.getSeparatorString());
        if (preserveFirstLF) {
            textCRLF = LineSeparator.LF.getSeparatorString() + textCRLF;
        }
        if (preserveLastCR) {
            textCRLF += LineSeparator.CR.getSeparatorString();
        }
        return textCRLF;
    }

    /**
     * Clears history and screen buffers, positions the cursor at the top left corner.
     */
    @Override
    public void clear() {
        myLastCR = false;
        myTerminalWidget.getTerminalPanel().clearBuffer();
    }

    @Override
    public void scrollTo(int offset) {
    }

    @Override
    public void attachToProcess(ProcessHandler processHandler) {
        if (processHandler != null) {
            attachToProcess(processHandler, true);
        }
    }

    /**
     * @param processHandler        ProcessHandler instance wrapping underlying PtyProcess
     * @param attachToProcessOutput true if process output should be printed in the console,
     *                              false if output printing is managed externally, e.g. by testing
     *                              console {@link com.intellij.execution.testframework.ui.BaseTestsOutputConsoleView}
     */
    protected final void attachToProcess(@NotNull ProcessHandler processHandler, boolean attachToProcessOutput) {
        if (myAttachedToProcess.compareAndSet(false, true)) {
            myTerminalWidget.createTerminalSession(new ProcessHandlerTtyConnector(
                    processHandler, EncodingProjectManager.getInstance(myProject).getDefaultCharset())
            );
            myTerminalWidget.start();
            contentToBeStarted = true;
        } else {
            myTerminalWidget.setTtyConnector(new ProcessHandlerTtyConnector(
                    processHandler, EncodingProjectManager.getInstance(myProject).getDefaultCharset())
            );
        }

        updateProcessHandler(processHandler);

        processAdapter = new ProcessAdapter() {
            @Override
            public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
                if (attachToProcessOutput) {
                    try {
                        ConsoleViewContentType contentType = null;
                        if (outputType != ProcessOutputTypes.STDOUT) {
                            contentType = ConsoleViewContentType.getConsoleViewType(outputType);
                        }

                        String text = event.getText();
                        if (outputType == ProcessOutputTypes.SYSTEM) {
                            text = StringUtil.convertLineSeparators(text, LineSeparator.CRLF.getSeparatorString());
                        }
                        printText(text, contentType);
                    }
                    catch (IOException e) {
                        LOG.info(e);
                    }
                }
            }

            @Override
            public void processTerminated(@NotNull ProcessEvent event) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    JBTerminalWidget widget = myTerminalWidget;
                    if (widget != null) {
                        widget.getTerminalPanel().setCursorVisible(false);
                    }
                }, ModalityState.any());
            }
        };

        processHandler.addProcessListener(processAdapter);

        if (contentToBeStarted) {
            ApplicationManager.getApplication().invokeLater(() -> {
                ExecRunContentDescriptor contentDescriptor = new ExecRunContentDescriptor(this, processHandler, this.getComponent(), tabTitle);
                RunContentManager.getInstance(myProject).showRunContent(DefaultRunExecutor.getRunExecutorInstance(), contentDescriptor);
            });
        }
    }

    private void updateProcessHandler(ProcessHandler processHandler) {
        if (this.processHandler != null) {
            this.processHandler.removeProcessListener(processAdapter);
        }
        this.processHandler = processHandler;
    }

    @Override
    public void setOutputPaused(boolean value) {

    }

    @Override
    public boolean isOutputPaused() {
        return false;
    }

    @Override
    public boolean hasDeferredOutput() {
        return false;
    }

    @Override
    public void performWhenNoDeferredOutput(@NotNull Runnable runnable) {
    }

    @Override
    public void setHelpId(@NotNull String helpId) {
    }

    @Override
    public void addMessageFilter(@NotNull Filter filter) {
        myTerminalWidget.addMessageFilter(filter);
    }

    @Override
    public void printHyperlink(@NotNull String hyperlinkText, @Nullable HyperlinkInfo info) {

    }

    @Override
    public int getContentSize() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return false;
    }


    @Deprecated
    @NotNull
    @ApiStatus.ScheduledForRemoval(inVersion = "2020.3")
    public AnAction[] detachConsoleActions(boolean prependSeparatorIfNonEmpty) {
        return AnAction.EMPTY_ARRAY;
    }

    @NotNull
    @Override
    public AnAction[] createConsoleActions() {
        return new AnAction[]{new ScrollToTheEndAction(), new ClearAction()};
    }

    @Override
    public void allowHeavyFilters() {
    }

    @Override
    public JComponent getComponent() {
        return myTerminalWidget.getComponent();
    }

    @Override
    public JComponent getPreferredFocusableComponent() {
        return myTerminalWidget.getComponent();
    }

    @Override
    public void dispose() {
        myTerminalWidget = null;
    }

    public static boolean isAcceptable(@NotNull ProcessHandler processHandler) {
        return processHandler instanceof OSProcessHandler &&
                ((OSProcessHandler)processHandler).getProcess() instanceof PtyProcess &&
                !(processHandler instanceof ColoredProcessHandler);
    }

    @Override
    public void addChangeListener(@NotNull ObservableConsoleView.ChangeListener listener, @NotNull Disposable parent) {

    }

    private class ConsoleTerminalWidget extends JBTerminalWidget implements DataProvider {
        private ConsoleTerminalWidget(@NotNull Project project, @NotNull JBTerminalSystemSettingsProviderBase provider) {
            super(project, provider, CommonTerminalExecutionConsole.this);
        }

        @Override
        protected JBTerminalPanel createTerminalPanel(@NotNull SettingsProvider settingsProvider,
                                                      @NotNull StyleState styleState,
                                                      @NotNull TerminalTextBuffer textBuffer) {
            JBTerminalPanel panel = new JBTerminalPanel((JBTerminalSystemSettingsProviderBase)settingsProvider, textBuffer, styleState) {
                @Override
                public void clearBuffer() {
                    super.clearBuffer(false);
                }
            };

            Disposer.register(this, panel);
            return panel;
        }

        @Override
        protected TerminalStarter createTerminalStarter(JediTerminal terminal, TtyConnector connector) {
            try {
                try {
                    Constructor<TerminalStarter> constructor = TerminalStarter.class.getConstructor(Terminal.class, TtyConnector.class, TerminalDataStream.class);
                    return constructor.newInstance(terminal, connector, myDataStream);
                } catch (NoSuchMethodException e) {
                    Constructor<TerminalStarter> constructor = (Constructor<TerminalStarter>) TerminalStarter.class.getConstructors()[0];
                    return constructor.newInstance(terminal, connector, myDataStream, null);
                }
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                LOG.warn(e.getLocalizedMessage(), e);
            }
            return null;
        }

        @Nullable
        @Override
        public Object getData(@NotNull String dataId) {
            if (LangDataKeys.CONSOLE_VIEW.is(dataId)) {
                return this;
            }
            return super.getData(dataId);
        }
    }

    private class ClearAction extends DumbAwareAction {
        private ClearAction() {
            super(ExecutionBundle.message("clear.all.from.console.action.name"), "Clear the contents of the console", AllIcons.Actions.GC);
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            e.getPresentation().setEnabled(true);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            clear();
        }
    }

    private class ScrollToTheEndAction extends DumbAwareAction {
        private ScrollToTheEndAction() {
            super(ActionsBundle.message("action.EditorConsoleScrollToTheEnd.text"),
                    ActionsBundle.message("action.EditorConsoleScrollToTheEnd.text"),
                    AllIcons.RunConfigurations.Scroll_down);
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            BoundedRangeModel model = getBoundedRangeModel();
            e.getPresentation().setEnabled(model != null && model.getValue() != 0);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            BoundedRangeModel model = getBoundedRangeModel();
            if (model != null) {
                model.setValue(0);
            }
        }

      @Override
      public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
      }

      @Nullable
        private BoundedRangeModel getBoundedRangeModel() {
            return myTerminalWidget != null ? myTerminalWidget.getTerminalPanel().getBoundedRangeModel() : null;
        }
    }
}
