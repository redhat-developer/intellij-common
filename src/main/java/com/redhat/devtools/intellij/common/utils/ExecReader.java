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

import com.intellij.util.io.BaseInputStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import org.jetbrains.annotations.NotNull;

public class ExecReader extends BaseInputStreamReader {

    private char[] readBuffer = new char[1024];
    private char[] buffer = new char[readBuffer.length * 2];
    private int bufferPosition = 0;
    private int bufferSize = 0;
    private boolean justReadCR = false;

    public ExecReader(@NotNull InputStream in) {
        super(in);
    }

    public ExecReader(@NotNull InputStream in, @NotNull Charset cs) {
        super(in, cs);
    }

    private boolean refillBufferIfNeeded(boolean onlyIfReady) throws IOException {
        if (bufferPosition < bufferSize || (onlyIfReady && !super.ready())) {
            return false;
        }
        bufferPosition = 0;
        bufferSize = 0;

        char[] readBuffer = this.readBuffer;
        int read = super.read(readBuffer, 0, readBuffer.length); // To not get delegated back to us
        if (read < 0) {
            return true; // EOF
        }

        boolean justReadCR = this.justReadCR;
        char[] buffer = this.buffer;
        int outI = 0;
        for (int i =0; i<read; i++) {
            char c = readBuffer[i];
            // If we encounter \n without preceding \r, add it.
            if (c == '\n' && !justReadCR) {
                buffer[outI++] = '\r';
            }
            buffer[outI++] = c;
            justReadCR = c == '\r';
        }
        bufferSize = outI;
        this.justReadCR = justReadCR;
        return false;
    }

    @Override
    public int read() throws IOException {
        if (refillBufferIfNeeded(false)) {
            return -1; // EOF
        }
        return buffer[bufferPosition++];
    }

    @Override
    public int read(@NotNull char[] cbuf, int offset, int length) throws IOException {
        if (refillBufferIfNeeded(false)) {// Blocking
            return -1; // EOF
        }

        int read = 0;
        while (bufferPosition < bufferSize && read < length) {
            int available = bufferSize - bufferPosition;
            int required = length - read;
            int copied = Math.min(available, required);
            System.arraycopy(buffer, bufferPosition, cbuf, offset + read, copied);
            read += copied;
            bufferPosition += copied;

            if (refillBufferIfNeeded(true)) {
                break;
            }
        }
        return read;
    }

    @Override
    public boolean ready() throws IOException {
        return bufferPosition < bufferSize || super.ready();
    }

    @Override
    public int read(@NotNull CharBuffer target) throws IOException {
        if (refillBufferIfNeeded(false)) {// Blocking
            return -1; // EOF
        }

        int read = 0;
        while (bufferPosition < bufferSize && target.remaining() > 0) {
            int available = bufferSize - bufferPosition;
            int required = target.remaining();
            int copied = Math.min(available, required);
            target.put(buffer, bufferPosition, copied);
            read += copied;
            bufferPosition += copied;

            if (refillBufferIfNeeded(true)) {
                break;
            }
        }
        return read;
    }

    @Override
    public int read(@NotNull char[] cbuf) throws IOException {
        return read(cbuf, 0, cbuf.length);
    }

    @Override
    public long skip(long n) throws IOException {
        if (refillBufferIfNeeded(false)) {// Blocking
            return -1; // EOF
        }

        long skipped = 0L;
        while (bufferPosition < bufferSize && skipped < n) {
            long available = (bufferSize - bufferPosition);
            long required = n - skipped;
            long copied = Math.min(available, required);
            skipped += copied;
            bufferPosition += copied;

            if (refillBufferIfNeeded(true)) {
                break;
            }
        }
        return skipped;
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public void mark(int readAheadLimit) throws IOException {
        super.mark(readAheadLimit);
    }

    @Override
    public void reset() throws IOException {
        super.reset();
    }
}

