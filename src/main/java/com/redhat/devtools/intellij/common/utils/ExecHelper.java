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
package com.redhat.devtools.intellij.common.utils;

import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.terminal.JBTerminalSystemSettingsProviderBase;
import com.intellij.terminal.JBTerminalWidget;
import com.intellij.util.ui.update.UiNotifyConnector;
import com.jediterm.terminal.ProcessTtyConnector;
import com.jediterm.terminal.TtyConnector;
import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import com.redhat.devtools.intellij.common.CommonConstants;
import com.redhat.devtools.intellij.common.utils.terminal.JCommonTerminalWidget;
import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.xmlgraphics.util.WriterOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.terminal.AbstractTerminalRunner;
import org.jetbrains.plugins.terminal.LocalTerminalDirectRunner;
import org.jetbrains.plugins.terminal.TerminalOptionsProvider;
import org.jetbrains.plugins.terminal.TerminalTabState;
import org.jetbrains.plugins.terminal.TerminalView;

import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.redhat.devtools.intellij.common.CommonConstants.HOME_FOLDER;

public class ExecHelper {
  private static final ScheduledExecutorService SERVICE = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());

  public static ScheduledFuture<?> executeAfter(Runnable runnable, long delay, TimeUnit unit) {
    return SERVICE.schedule(runnable, delay, unit);
  }

  public static void submit(Runnable runnable) {
    SERVICE.submit(runnable);
  }

  /**
   * This method combine <b>out</b> and <b>err</b> outputs in result string, if you need to have them separately
   *  use @link {@link #executeWithResult(String, boolean, File, Map, String...)}
   *
   * @param executable the executable
   * @param checkExitCode if exit code should be checked
   * @param workingDirectory the working directory for the process
   * @param envs the map for the environment variables
   * @param arguments the arguments
   * @return the combined output and error stream as a String
   * @throws IOException if error during process execution
   */
  public static String execute(String executable, boolean checkExitCode, File workingDirectory, Map<String,String> envs,
                               String... arguments) throws IOException {
    DefaultExecutor executor = new DefaultExecutor() {
      @Override
      public boolean isFailure(int exitValue) {
        if (checkExitCode) {
          return super.isFailure(exitValue);
        } else {
          return false;
        }
      }
    };
    StringWriter writer = new StringWriter();
    PumpStreamHandler handler = new PumpStreamHandler(new WriterOutputStream(writer));
    executor.setStreamHandler(handler);
    executor.setWorkingDirectory(workingDirectory);
    CommandLine command = new CommandLine(executable).addArguments(arguments, false);
    Map<String, String> env = new HashMap<>(System.getenv());
    env.putAll(envs);
    try {
      executor.execute(command, env);
      return writer.toString();
    } catch (IOException e) {
      throw new IOException(e.getLocalizedMessage() + " " + writer.toString(), e);
    }
  }

  /**
   * This method combine <b>out</b> and <b>err</b> outputs in result string, if you need to have them separately
   *  use @link {@link #executeWithResult(String, boolean, File, Map, String...)}
   * @param executable the executable
   * @param workingDirectory the working directory for the process
   * @param envs the map for the environment variables
   * @param arguments the arguments
   * @return the combined output and error stream as a String
   * @throws IOException if error during process execution
   */
  public static String execute(String executable, File workingDirectory, Map<String, String> envs, String... arguments) throws IOException {
    return execute(executable, true, workingDirectory, envs, arguments);
  }

  /**
   * This method combine <b>out</b> and <b>err</b> outputs in result string, if you need to have them separately
   *  use @link {@link #executeWithResult(String, boolean, File, Map, String...)}
   * @param executable the executable
   * @param envs the map for the environment variables
   * @param arguments the arguments
   * @return the combined output and error stream as a String
   * @throws IOException if error during process execution
   */
  public static String execute(String executable, Map<String, String> envs, String... arguments) throws IOException {
    return execute(executable, true, new File(HOME_FOLDER), envs, arguments);
  }
  /**
   * This method combine <b>out</b> and <b>err</b> outputs in result string, if you need to have them separately
   *  use @link {@link #executeWithResult(String, boolean, File, Map, String...)}
   * @param executable the executable
   * @param arguments the arguments
   * @return the combined output and error stream as a String
   * @throws IOException if error during process execution
   */
  public static String execute(String executable, String... arguments) throws IOException {
    return execute(executable, Collections.emptyMap(), arguments);
  }

  /**
   * This method combine <b>out</b> and <b>err</b> outputs in result string, if you need to have them separately
   *  use @link {@link #executeWithResult(String, boolean, File, Map, String...)}
   * @param executable the executable
   * @param workingDirectory the working directory for the process
   * @param arguments the arguments
   * @return the combined output and error stream as a String
   * @throws IOException if error during process execution
   */
  public static String execute(String executable, File workingDirectory, String... arguments) throws IOException {
    return execute(executable, true, workingDirectory, Collections.emptyMap(), arguments);
  }

  /**
   * This method combine <b>out</b> and <b>err</b> outputs in result string, if you need to have them separately
   *  use @link {@link #executeWithResult(String, boolean, File, Map, String...)}
   * @param executable the executable
   * @param checkExitCode if exit code should be checked
   * @param envs the map for the environment variables
   * @param arguments the arguments
   * @return the combined output and error stream as a String
   * @throws IOException if error during process execution
   */
  public static String execute(String executable, boolean checkExitCode, Map<String, String> envs,
                               String... arguments) throws IOException {
    return execute(executable, checkExitCode, new File(HOME_FOLDER), envs, arguments);
  }

  /**
   * This method combine <b>out</b> and <b>err</b> outputs in result string, if you need to have them separately
   *  use @link {@link #executeWithResult(String, boolean, File, Map, String...)}
   * @param executable the executable
   * @param checkExitCode if exit code should be checked
   * @param arguments the arguments
   * @return the combined output and error stream as a String
   * @throws IOException if error during process execution
   */
  public static String execute(String executable, boolean checkExitCode, String... arguments) throws IOException {
    return execute(executable, checkExitCode, new File(HOME_FOLDER), Collections.emptyMap(), arguments);
  }

  public static ExecResult executeWithResult(String executable, boolean checkExitCode, File workingDirectory, Map<String,String> envs,
                                             String... arguments) throws IOException {
    DefaultExecutor executor = new DefaultExecutor() {
      @Override
      public boolean isFailure(int exitValue) {
        if (checkExitCode) {
          return super.isFailure(exitValue);
        } else {
          return false;
        }
      }
    };
    StringWriter outWriter = new StringWriter();
    StringWriter errWriter = new StringWriter();
    PumpStreamHandler handler = new PumpStreamHandler(new WriterOutputStream(outWriter), new WriterOutputStream(errWriter));
    executor.setStreamHandler(handler);
    executor.setWorkingDirectory(workingDirectory);
    CommandLine command = new CommandLine(executable).addArguments(arguments, false);
    Map<String, String> env = new HashMap<>(System.getenv());
    env.putAll(envs);
    try {
      int exitCode = executor.execute(command, env);
      return new ExecResult(outWriter.toString(), errWriter.toString(), exitCode);
    } catch (IOException e) {
      throw new IOException(e.getLocalizedMessage() + " " + errWriter.toString(), e);
    }
  }

  public static ExecResult executeWithResult(String executable, Map<String, String> envs, String... arguments) throws IOException {
    return executeWithResult(executable, true, new File(HOME_FOLDER), envs, arguments);
  }

  public static class ExecResult {
    private final String stdOut;
    private final @Nullable String stdErr;
    private final int exitCode;

    public ExecResult(String stdOut, @Nullable String stdErr, int exitCode) {
      this.stdOut = stdOut;
      this.stdErr = stdErr;
      this.exitCode = exitCode;
    }

    public String getStdOut() {
      return stdOut;
    }

    public String getStdErr() {
      return stdErr;
    }

    public int getExitCode() {
      return exitCode;
    }
  }

  private static class RedirectedStream extends FilterInputStream {
    private boolean emitLF = false;
    private final boolean redirect;
    private final boolean delay;

    private RedirectedStream(InputStream delegate, boolean redirect, boolean delay) {
      super(delegate);
      this.redirect = redirect;
      this.delay = delay;
    }

    @Override
    public synchronized int read() throws IOException {
      if (emitLF) {
        emitLF = false;
        return '\n';
      } else {
        int c = super.read();
        if (redirect && c == '\n') {
          emitLF = true;
          c = '\r';
        }
        return c;
      }
    }

    @Override
    public synchronized int read(@NotNull byte[] b) throws IOException {
      return read(b, 0, b.length);
    }

    @Override
    public synchronized int read(@NotNull byte[] b, int off, int len) throws IOException {
      if (b == null) {
        throw new NullPointerException();
      } else if (off < 0 || len < 0 || len > b.length - off) {
        throw new IndexOutOfBoundsException();
      } else if (len == 0) {
        return 0;
      }

      int c = read();
      if (c == -1) {
        if (delay) {
          try {
            Thread.sleep(60000L);
          } catch (InterruptedException e) {}
        }
        return -1;
      }
      b[off] = (byte)c;

      int i = 1;
      try {
        for (; i < len  && available() > 0; i++) {
          c = read();
          if (c == -1) {
            break;
          }
          b[off + i] = (byte)c;
        }
      } catch (IOException ee) {}
      return i;
    }
  }

  private static void executeWithTerminalInternal(Project project, String title, File workingDirectory,
                                                  boolean readOnly, boolean keepTabOpened,
                                                  Map<String, String> envs, String... command) throws IOException {
    TerminalTabState terminalTabState = new TerminalTabState();
    terminalTabState.myTabName = title;
    terminalTabState.myWorkingDirectory = workingDirectory.getPath();

    LocalTerminalDirectRunner runner = createTerminalRunner(project, readOnly, keepTabOpened, envs, command);

    ApplicationManager.getApplication().invokeLater(() -> {
      TerminalView.getInstance(project).createNewSession(runner, terminalTabState);
    });
  }

  private static LocalTerminalDirectRunner createTerminalRunner(Project project, boolean readOnly, boolean keepTabOpened, Map<String, String> customEnvs, String... command) {
    Disposable disposable = Disposer.newDisposable();
    LocalTerminalDirectRunner runner = new LocalTerminalDirectRunner(project) {
      public String[] getCommand(Map<String, String> envs) {
        envs.putAll(customEnvs);
        return command;
      }

      public List<String> getInitialCommand(@NotNull Map<String, String> envs) {
        envs.putAll(customEnvs);
        return Arrays.asList(command);
      }

      protected @NotNull JBTerminalWidget createTerminalWidget(@NotNull Disposable parent, @Nullable VirtualFile currentWorkingDirectory, boolean deferSessionUntilFirstShown) {
        JBTerminalWidget jbTerminalWidget = new JCommonTerminalWidget(project, new JBTerminalSystemSettingsProviderBase(), disposable, readOnly, keepTabOpened);
        Runnable openSession = () -> openSessionInDirectory(jbTerminalWidget, currentWorkingDirectory.getPath());
        if (deferSessionUntilFirstShown) {
          UiNotifyConnector.doWhenFirstShown(jbTerminalWidget, openSession);
        } else {
          openSession.run();
        }
        return jbTerminalWidget;
      }

      public void openSessionInDirectory(@NotNull JBTerminalWidget terminalWidget,
                                         @Nullable String directory) {
        ModalityState modalityState = ModalityState.stateForComponent(terminalWidget.getComponent());
        Dimension size = terminalWidget.getTerminalPanel().getTerminalSizeFromComponent();

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
          try {
            PtyProcessBuilder builder = new PtyProcessBuilder(command)
                    .setEnvironment(customEnvs)
                    .setDirectory(directory)
                    .setInitialColumns(size != null ? size.width : null)
                    .setInitialRows(size != null ? size.height : null);
            PtyProcess p = builder.start();
            TtyConnector connector = createTtyConnector(p);

            ApplicationManager.getApplication().invokeLater(() -> {
              try {
                terminalWidget.createTerminalSession(connector);
              } catch (Exception e) {
                Logger.getInstance(ExecHelper.class).warn("Cannot create terminal session for " + runningTargetName(), e);
              }
              try {
                terminalWidget.start();
                terminalWidget.getComponent().revalidate();
                terminalWidget.notifyStarted();
              } catch (RuntimeException e) {
                Logger.getInstance(ExecHelper.class).warn("Cannot open " + runningTargetName(), e);
              }
            }, modalityState);
          } catch (Exception e) {
            Logger.getInstance(ExecHelper.class).warn("Cannot open " + runningTargetName(), e);
          }
        });
      }
    };
    return runner;
  }

  public static void executeWithTerminal(Project project, String title, File workingDirectory,
                                         boolean waitForProcessToExit, Map<String, String> envs, String... command) throws IOException {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      execute(command[0], workingDirectory, envs, Arrays.stream(command)
              .skip(1)
              .toArray(String[]::new));
    } else {
      executeWithTerminalInternal(project, title, workingDirectory, false, true, envs, command);
    }
  }

  public static void executeWithTerminal(Project project, String title, File workingDirectory,
                                         boolean readOnly, boolean keepTabOpened, Map<String, String> envs, String... command) throws IOException {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      execute(command[0], workingDirectory, envs, Arrays.stream(command)
              .skip(1)
              .toArray(String[]::new));
    } else {
      executeWithTerminalInternal(project, title, workingDirectory, readOnly, keepTabOpened, envs, command);
    }
  }

  public static void executeWithTerminal(Project project, String title, File workingDirectory, String... command) throws IOException {
    executeWithTerminal(project, title, workingDirectory, true, Collections.emptyMap(), command);
  }

  public static void executeWithTerminal(Project project, String title, boolean waitForProcessToExit,
                                         Map<String, String> envs, String... command) throws IOException {
    executeWithTerminal(project, title, new File(HOME_FOLDER), waitForProcessToExit, envs, command);
  }

  public static void executeWithTerminal(Project project, String title, boolean waitForProcessToExit, String... command) throws IOException {
    executeWithTerminal(project, title, new File(HOME_FOLDER), true, waitForProcessToExit, Collections.emptyMap(), command);
  }

  public static void executeWithTerminal(Project project, String title, Map<String, String> envs, String... command) throws IOException {
    executeWithTerminal(project, title, new File(HOME_FOLDER), true, envs, command);
  }

  public static void executeWithTerminal(Project project, String title, String... command) throws IOException {
    executeWithTerminal(project, title, new File(HOME_FOLDER), true, Collections.emptyMap(), command);
  }

  public static void executeWithUI(Map<String, String> envs, Runnable initRunnable, Consumer<String> runnable, String... command) throws IOException {
    ProcessBuilder builder = (new ProcessBuilder(command)).directory(new File(CommonConstants.HOME_FOLDER)).redirectErrorStream(true);
    builder.environment().putAll(envs);
    Process p = builder.start();
    linkProcessToUI(p, initRunnable, runnable);
  }

  public static void executeWithUI(Map<String, String> envs, Consumer<String> runnable, String... command) throws IOException {
    executeWithUI(envs, null, runnable, command);
  }

  private static void linkProcessToUI(Process p, Runnable initRunnable, Consumer<String> runnable) {
    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      if (initRunnable != null) {
        UIHelper.executeInUI(initRunnable);
      }
      StringBuilder sb = new StringBuilder();
      String line;

      try {
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"));
        while ((line = reader.readLine()) != null) {
          sb.append(line).append("\n");

          UIHelper.executeInUI(() -> runnable.accept(sb.toString()));
        }
      }catch(IOException e) {}
    });
  }
}
