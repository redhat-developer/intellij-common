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

import com.intellij.execution.configuration.EnvironmentVariablesData;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.jediterm.terminal.ProcessTtyConnector;
import com.jediterm.terminal.TtyConnector;
import com.redhat.devtools.intellij.common.CommonConstants;
import java.io.BufferedReader;
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.output.WriterOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.terminal.AbstractTerminalRunner;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;
import org.jetbrains.plugins.terminal.TerminalOptionsProvider;
import org.jetbrains.plugins.terminal.TerminalProjectOptionsProvider;
import org.jetbrains.plugins.terminal.TerminalToolWindowFactory;
import org.jetbrains.plugins.terminal.TerminalView;


import static com.redhat.devtools.intellij.common.CommonConstants.HOME_FOLDER;

public class ExecHelper {
  private static final ScheduledExecutorService SERVICE = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());

  private ExecHelper() {}

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
    PumpStreamHandler handler = new PumpStreamHandler(new WriterOutputStream(writer, Charset.defaultCharset()));
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
    PumpStreamHandler handler = new PumpStreamHandler(
            new WriterOutputStream(outWriter, Charset.defaultCharset()),
            new WriterOutputStream(errWriter, Charset.defaultCharset()));
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

    private RedirectedStream(InputStream delegate, boolean redirect) {
      super(delegate);
      this.redirect = redirect;
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
  private static class RedirectedProcess extends Process {
    private final Process delegate;
    private final InputStream inputStream;

    private RedirectedProcess(Process delegate, boolean redirect) {
      this.delegate = delegate;
      inputStream = new RedirectedStream(delegate.getInputStream(), redirect) {};
    }

    @Override
    public OutputStream getOutputStream() {
      return delegate.getOutputStream();
    }

    @Override
    public InputStream getInputStream() {
      return inputStream;
    }

    @Override
    public InputStream getErrorStream() {
      return delegate.getErrorStream();
    }

    @Override
    public int waitFor() throws InterruptedException {
      return delegate.waitFor();
    }

    @Override
    public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
      return delegate.waitFor(timeout, unit);
    }

    @Override
    public int exitValue() {
      return delegate.exitValue();
    }

    @Override
    public void destroy() {
      delegate.destroy();
    }

    @Override
    public Process destroyForcibly() {
      return delegate.destroyForcibly();
    }

    @Override
    public boolean isAlive() {
      return delegate.isAlive();
    }
  }
  private static void executeWithTerminalInternal(Project project, String title, File workingDirectory,
                                                  boolean waitForProcessExit, Map<String, String> envs,
                                                  String... command) throws IOException {
    try {
      ProcessBuilder builder = new ProcessBuilder(command).directory(workingDirectory).redirectErrorStream(true);
      builder.environment().putAll(envs);
      Process p = builder.start();
      linkProcessToTerminal(p, project, title, waitForProcessExit);
    } catch (IOException e) {
      throw e;
    }
  }

  private static AbstractTerminalRunner createTerminalRunner(Project project, Process process, String title) {
    AbstractTerminalRunner runner = new AbstractTerminalRunner(project) {
      @Override
      public Process createProcess(@Nullable String s) {
        return process;
      }

      @Override
      protected ProcessHandler createProcessHandler(Process process) {
        return null;
      }

      @Override
      protected String getTerminalConnectionName(Process process) {
        return null;
      }

      @Override
      protected TtyConnector createTtyConnector(Process process) {
        return new ProcessTtyConnector(process, StandardCharsets.UTF_8) {
          @Override
          protected void resizeImmediately() {
          }

          @Override
          public String getName() {
            return title;
          }

          @Override
          public boolean isConnected() {
            return true;
          }
        };
      }

      @Override
      public String runningTargetName() {
        return null;
      }
    };
    return runner;
  }

  /**
   * Ensure the terminal window tab is created. This is required because some IJ editions (2018.3) do not
   * initialize this window when you create a TerminalView through {@link #linkProcessToTerminal(Process, Project, String, boolean)}
   *
   * @param project the IJ project
   */
  public static void ensureTerminalWindowsIsOpened(Project project) {
    ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TerminalToolWindowFactory.TOOL_WINDOW_ID);
    if (toolWindow != null) {
      ApplicationManager.getApplication().invokeAndWait(() -> toolWindow.show(null));
    }
  }

  public static void linkProcessToTerminal(Process p, Project project, String title,  boolean waitForProcessExit) throws IOException {
      try {
        ensureTerminalWindowsIsOpened(project);
        boolean isPost2018_3 = ApplicationInfo.getInstance().getBuild().getBaselineVersion() >= 183;
        final RedirectedProcess process = new RedirectedProcess(p, true);
        AbstractTerminalRunner runner = createTerminalRunner(project, process, title);

        TerminalOptionsProvider terminalOptions = ServiceManager.getService(TerminalOptionsProvider.class);
        terminalOptions.setCloseSessionOnLogout(false);
        final TerminalView view = TerminalView.getInstance(project);
        final Method[] method = new Method[1];
        final Object[][] parameters = new Object[1][];
        try {
          method[0] = TerminalView.class.getMethod("createNewSession", new Class[] {Project.class, AbstractTerminalRunner.class});
          parameters[0] = new Object[] {project, runner};
        } catch (NoSuchMethodException e) {
          try {
            method[0] = TerminalView.class.getMethod("createNewSession", new Class[] {AbstractTerminalRunner.class});
            parameters[0] = new Object[] { runner};
          } catch (NoSuchMethodException e1) {
            throw new IOException(e1);
          }
        }
        ApplicationManager.getApplication().invokeLater(() -> {
          try {
            method[0].invoke(view, parameters[0]);
          } catch (IllegalAccessException|InvocationTargetException e) {}
        });
        if (waitForProcessExit && p.waitFor() != 0) {
          throw new IOException("Process returned exit code: " + p.exitValue(), null);
        }
    } catch (IOException e) {
        throw e;
      }
      catch (InterruptedException e) {
        throw new IOException(e);
      }
  }

  public static void executeWithTerminal(Project project, String title, File workingDirectory,
                                         boolean waitForProcessToExit, Map<String, String> envs, String... command) throws IOException {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      execute(command[0], workingDirectory, envs, Arrays.stream(command)
              .skip(1)
              .toArray(String[]::new));
    } else {
      executeWithTerminalInternal(project, title, workingDirectory, waitForProcessToExit, envs, command);
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
    executeWithTerminal(project, title, new File(HOME_FOLDER), waitForProcessToExit, Collections.emptyMap(), command);
  }

  public static void executeWithTerminal(Project project, String title, Map<String, String> envs, String... command) throws IOException {
    executeWithTerminal(project, title, new File(HOME_FOLDER), true, envs, command);
  }

  public static void executeWithTerminal(Project project, String title, String... command) throws IOException {
    executeWithTerminal(project, title, new File(HOME_FOLDER), true, Collections.emptyMap(), command);
  }

  public static void executeWithTerminalWidget(Project project, String title, String... command) throws IOException {
    executeWithTerminalWidgetInternal(project, HOME_FOLDER, title, Collections.emptyMap(), command);
  }

  public static void executeWithTerminalWidget(Project project, String title, Map<String, String> envs, String... command) throws IOException {
    executeWithTerminalWidgetInternal(project, HOME_FOLDER, title, envs, command);
  }

  public static void executeWithTerminalWidgetInternal(Project project, String workingDirectory, String title, Map<String, String> envs, String... command) throws IOException {
    ensureTerminalWindowsIsOpened(project);

    ApplicationManager.getApplication().invokeLater(() -> {
      try {
        ShellTerminalWidget terminal = createTerminal(project, title, workingDirectory);
        if (terminal == null) {
          return;
        }
        TerminalProjectOptionsProvider.getInstance(project).setEnvData(EnvironmentVariablesData.create(envs, true));
        terminal.executeCommand(String.join(" ", command));
      } catch (IOException e) {
        Logger.getInstance(ExecHelper.class).warn("Could execute " + command + " in local shell terminal widget", e);
      }
    });
  }

  private static ShellTerminalWidget createTerminal(Project project, String title, String workingDirectory) throws IOException {
    Method[] method = new Method[1];
    Object[][] parameters = new Object[1][];
    try {
      method[0] = TerminalView.class.getMethod("createLocalShellWidget", String.class, String.class);
      parameters[0] = new Object[]{workingDirectory, title};
    } catch (NoSuchMethodException e) {
      try {
        method[0] = TerminalView.class.getMethod("createLocalShellWidget", String.class);
        parameters[0] = new Object[] { workingDirectory };
      } catch (NoSuchMethodException e1) {
        Logger.getInstance(ExecHelper.class).warn("Could not create shell terminal widget", e);
        return null;
      }
    }

    try {
      return (ShellTerminalWidget) method[0].invoke(TerminalView.getInstance(project), parameters[0]);
    } catch (IllegalAccessException | InvocationTargetException e) {
      Logger.getInstance(ExecHelper.class).warn("Could not create shell terminal widget", e);
      return null;
    }
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
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8));
        while ((line = reader.readLine()) != null) {
          sb.append(line).append("\n");

          UIHelper.executeInUI(() -> runnable.accept(sb.toString()));
        }
      }catch(IOException e) {}
    });
  }
}
