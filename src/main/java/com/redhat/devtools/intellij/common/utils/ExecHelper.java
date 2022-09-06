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

import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.process.NopProcessHandler;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.RunContentManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.terminal.TerminalExecutionConsole;
import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import com.redhat.devtools.intellij.common.CommonConstants;
import com.redhat.devtools.intellij.common.model.ProcessHandlerInput;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.output.WriterOutputStream;
import org.jetbrains.annotations.Nullable;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
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
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

  private static class ListeningWriterOutputStream extends WriterOutputStream {
    private final ProcessListener listener;
    private final Charset charset;
    private StringWriter writer;
    private WriterOutputStream stream;

    private ListeningWriterOutputStream(Writer writer, Charset charset, ProcessListener listener) {
      super(writer, charset);
      this.charset = charset;
      this.listener = listener;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      super.write(b, off, len);
      if (listener != null) {
        String str = new String(b, off, len, charset);
        listener.onTextAvailable(new ProcessEvent(new NopProcessHandler(), str), ProcessOutputTypes.STDOUT);
      }
    }
  }

  /**
   * This method combine <b>out</b> and <b>err</b> outputs in result string, if you need to have them separately
   *  use @link {@link #executeWithResult(String, boolean, File, Map, String...)}
   *
   * @param executable the executable
   * @param checkExitCode if exit code should be checked
   * @param workingDirectory the working directory for the process
   * @param envs the map for the environment variables
   * @param listener the process listener
   * @param arguments the arguments
   * @return the combined output and error stream as a String
   * @throws IOException if error during process execution
   */
  public static String execute(String executable, boolean checkExitCode, File workingDirectory, Map<String,String> envs,
                               ProcessListener listener, String... arguments) throws IOException {
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
    PumpStreamHandler handler = new PumpStreamHandler(new ListeningWriterOutputStream(writer, Charset.defaultCharset(),
            listener));
    executor.setStreamHandler(handler);
    executor.setWorkingDirectory(workingDirectory);
    CommandLine command = new CommandLine(executable).addArguments(arguments, false);
    Map<String, String> env = new HashMap<>(System.getenv());
    env.putAll(envs);
    try {
      if (checkExitCode) {
        executor.execute(command, env);
      } else {
        executor.execute(command, env, new DefaultExecuteResultHandler());
      }
      return writer.toString();
    } catch (IOException e) {
      throw new IOException(e.getLocalizedMessage() + " " + writer.toString(), e);
    }
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
    return execute(executable, checkExitCode, workingDirectory, envs, null, arguments);
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

  private static void executeWithTerminalInternal(Project project, String title, File workingDirectory,
                                                  boolean waitForProcessExit, Map<String, String> envs,
                                                  ConsoleView terminalToReuse,
                                                  Function<ProcessHandlerInput, ExecProcessHandler> processHandlerFunction,
                                                  ProcessListener processListener, String... command) throws IOException {
    try {
      PtyProcess p = createPtyProcess(workingDirectory, envs, command);
      if (terminalToReuse != null) {
        attachProcessToTerminal(terminalToReuse, p, waitForProcessExit, processHandlerFunction, processListener, command);
      } else {
        linkProcessToTerminal(p, project, title, waitForProcessExit, processHandlerFunction, processListener, command);
      }
    } catch (IOException e) {
      throw e;
    }
  }

  public static CommonTerminalExecutionConsole createTerminalTabForReuse(Project project, String title) {
    String tabTitle = getTabTitle(project, title);
    return new CommonTerminalExecutionConsole(project, null, tabTitle);
  }


  private static PtyProcess createPtyProcess(File workingDirectory, Map<String, String> envs, String[] command) throws IOException {
    PtyProcessBuilder builder = new PtyProcessBuilder(command);
    builder.setEnvironment(getEnvs(envs));
    builder.setDirectory(workingDirectory.getPath());
    builder.setRedirectErrorStream(true);
    return builder.start();
  }

  private static Map<String, String> getEnvs(Map<String, String> customEnvs) {
    Map<String, String> envs = new HashMap<>();
    envs.putAll(System.getenv());
    envs.putAll(customEnvs);
    return envs;
  }

  /**
   *
   * @param p ptyprocess
   * @param project project
   * @param title tab title
   * @param waitForProcessExit wait
   * @param command must not be empty (for correct thread attribution in the stacktrace)
   * @throws IOException if errors while processing
   */
  public static void linkProcessToTerminal(PtyProcess p, Project project, String title, boolean waitForProcessExit, String... command) throws IOException {
    linkProcessToTerminal(p, project, title, waitForProcessExit, null, null, command);
  }

  /**
   *
   * @param p ptyprocess
   * @param project project
   * @param title tab title
   * @param waitForProcessExit wait
   * @param processHandlerFunction the process handler function
   * @param processListener listener to attach to the process
   * @param command must not be empty (for correct thread attribution in the stacktrace)
   * @throws IOException if errors while processing
   */
  public static void linkProcessToTerminal(PtyProcess p, Project project, String title, boolean waitForProcessExit,
                                           Function<ProcessHandlerInput, ExecProcessHandler> processHandlerFunction,
                                           ProcessListener processListener, String... command) throws IOException {
    ExecProcessHandler processHandler = createProcessHandler(p, processHandlerFunction, command);
    if (processListener != null) {
      processHandler.addProcessListener(processListener);
    }

    TerminalExecutionConsole terminalExecutionConsole = new TerminalExecutionConsole(project, processHandler);

    JPanel panel = new JPanel(new BorderLayout());
    panel.add(terminalExecutionConsole.getComponent(), BorderLayout.CENTER);
    processHandler.startNotify();
    String tabTitle = getTabTitle(project, title);
    ApplicationManager.getApplication().invokeLater(() -> {
      ExecRunContentDescriptor contentDescriptor = new ExecRunContentDescriptor(terminalExecutionConsole, processHandler, panel, tabTitle);
      RunContentManager.getInstance(project).showRunContent(DefaultRunExecutor.getRunExecutorInstance(), contentDescriptor);
    });

    try {
      if (waitForProcessExit && p.waitFor() != 0) {
        throw new IOException("Process returned exit code: " + p.exitValue(), null);
      }
    } catch (InterruptedException e) {
      throw new IOException(e);
    }
  }

  public static void attachProcessToTerminal(ConsoleView terminalExecutionConsole, PtyProcess p, boolean waitForProcessExit,
                                             Function<ProcessHandlerInput, ExecProcessHandler> processHandlerFunction,
                                             ProcessListener processListener, String... command) throws IOException {
    ExecProcessHandler processHandler = createProcessHandler(p, processHandlerFunction, command);
    if (processListener != null) {
      processHandler.addProcessListener(processListener);
    }
    terminalExecutionConsole.attachToProcess(processHandler);
    processHandler.startNotify();

    try {
      if (waitForProcessExit && p.waitFor() != 0) {
        throw new IOException("Process returned exit code: " + p.exitValue(), null);
      }
    } catch (InterruptedException e) {
      throw new IOException(e);
    }
  }

  private static ExecProcessHandler createProcessHandler(PtyProcess p,
                                                  Function<ProcessHandlerInput, ExecProcessHandler> processHandlerFunction,
                                                  String... command) {
    if (processHandlerFunction != null) {
      return processHandlerFunction.apply(new ProcessHandlerInput(p, String.join(" ", command)));
    } else {
      return new ExecProcessHandler(p, String.join(" ", command), Charset.defaultCharset());
    }
  }

  private static String getTabTitle(Project project, String title) {
    Pattern pattern = Pattern.compile(title + "\\(([0-9]+)\\)");
    int max = RunContentManager.getInstance(project).getAllDescriptors()
            .stream()
            .mapToInt(run -> {
              Matcher m = pattern.matcher(run.getDisplayName());
              if (m.find()) {
                return Integer.parseInt(m.group(1));
              }
              return -1;
            }).max().orElse(0);
    return title + "(" + (++max) + ")";
  }

  public static void executeWithTerminal(Project project, String title, File workingDirectory,
                                         boolean waitForProcessToExit, Map<String, String> envs,
                                         ConsoleView terminalToReuse,
                                         Function<ProcessHandlerInput, ExecProcessHandler> processHandlerFunction,
                                         ProcessListener processListener, String... command) throws IOException {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      execute(command[0], waitForProcessToExit, workingDirectory, envs, processListener, Arrays.stream(command)
              .skip(1)
              .toArray(String[]::new));
    } else {
      executeWithTerminalInternal(project, title, workingDirectory, waitForProcessToExit, envs, terminalToReuse,
              processHandlerFunction, processListener, command);
    }
  }

  public static void executeWithTerminal(Project project, String title, File workingDirectory, String... command) throws IOException {
    executeWithTerminal(project, title, workingDirectory, true, Collections.emptyMap(), null, null, null, command);
  }

  public static void executeWithTerminal(Project project, String title, boolean waitForProcessToExit,
                                         Map<String, String> envs, String... command) throws IOException {
    executeWithTerminal(project, title, new File(HOME_FOLDER), waitForProcessToExit, envs, null, null, null, command);
  }

  public static void executeWithTerminal(Project project, String title, boolean waitForProcessToExit, String... command) throws IOException {
    executeWithTerminal(project, title, new File(HOME_FOLDER), waitForProcessToExit, Collections.emptyMap(), null, null, null, command);
  }

  public static void executeWithTerminal(Project project, String title, Map<String, String> envs, String... command) throws IOException {
    executeWithTerminal(project, title, new File(HOME_FOLDER), true, envs, null, null, null, command);
  }

  public static void executeWithTerminal(Project project, String title, String... command) throws IOException {
    executeWithTerminal(project, title, new File(HOME_FOLDER), true, Collections.emptyMap(), null, null, null, command);
  }

  public static void executeWithTerminal(Project project, String title, File workingDirectory, boolean waitForProcessToExit, Map<String, String> envs, String... command) throws IOException {
    executeWithTerminal(project, title, workingDirectory, waitForProcessToExit, envs, null, null, null, command);
  }

  public static void executeWithTerminal(Project project, String title, Map<String, String> envs, ConsoleView terminalToReuse, String... command) throws IOException {
    executeWithTerminal(project, title, new File(HOME_FOLDER), true, envs, terminalToReuse, null, null, command);
  }

  public static void executeWithTerminal(Project project, String title, Map<String, String> envs, ConsoleView terminalToReuse, ProcessListener processListener, String... command) throws IOException {
    executeWithTerminal(project, title, new File(HOME_FOLDER), true, envs, terminalToReuse, null, processListener, command);
  }

  public static void executeWithTerminal(Project project, String title, Map<String, String> envs, ConsoleView terminalToReuse,
                                         Function<ProcessHandlerInput, ExecProcessHandler> processHandlerFunction, ProcessListener processListener, String... command) throws IOException {
    executeWithTerminal(project, title, new File(HOME_FOLDER), true, envs, terminalToReuse, processHandlerFunction, processListener, command);
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
