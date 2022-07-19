/*******************************************************************************
 * Copyright (c) 2021 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc.
 ******************************************************************************/
package com.redhat.devtools.intellij.common.utils;

import com.intellij.execution.process.KillableColoredProcessHandler;
import com.intellij.util.io.BaseDataReader;
import com.intellij.util.io.BaseOutputReader;
import org.jetbrains.annotations.NotNull;

import java.io.Reader;
import java.nio.charset.Charset;

public class ExecProcessHandler extends KillableColoredProcessHandler {

    /**
     *
     * @param process process
     * @param commandLine must not be empty (for correct thread attribution in the stacktrace)
     * @param charset charset
     */
    public ExecProcessHandler(@NotNull Process process, /*@NotNull*/ String commandLine, @NotNull Charset charset) {
        super(process, commandLine, charset);
    }

    @Override
    protected BaseOutputReader.Options readerOptions() {
        return new BaseOutputReader.Options() {
            @Override
            public BaseDataReader.SleepingPolicy policy() {
                return BaseDataReader.SleepingPolicy.BLOCKING;
            }

            @Override
            public boolean splitToLines() {
                return false;
            }

            @Override
            public boolean sendIncompleteLines() {
                return true;
            }

            @Override
            public boolean withSeparators() {
                return true;
            }
        };
    }

    @NotNull
    @Override
    protected Reader createProcessOutReader() {
        return new ExecReader(myProcess.getInputStream());
    }

    @NotNull
    @Override
    protected Reader createProcessErrReader() {
        return new ExecReader(myProcess.getErrorStream());
    }
}
