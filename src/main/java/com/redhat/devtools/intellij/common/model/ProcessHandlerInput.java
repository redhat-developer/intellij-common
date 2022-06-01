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
package com.redhat.devtools.intellij.common.model;

import java.nio.charset.Charset;

public class ProcessHandlerInput {

    private Process process;
    private String commandLine;
    private Charset charset;

    public ProcessHandlerInput(Process process, String commandLine) {
        this(process, commandLine, Charset.defaultCharset());
    }

    public ProcessHandlerInput(Process process, String commandLine, Charset charset) {
        this.process = process;
        this.commandLine = commandLine;
        this.charset = charset;
    }

    public Process getProcess() {
        return process;
    }

    public String getCommandLine() {
        return commandLine;
    }

    public Charset getCharset() {
        return charset;
    }
}
