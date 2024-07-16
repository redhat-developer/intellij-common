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
package com.redhat.devtools.intellij.common.utils;

import com.intellij.openapi.ui.TestDialog;
import com.intellij.testFramework.LightPlatformTestCase;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

public class DownloadHelperTest extends LightPlatformTestCase {
    private TestDialog previous;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        previous = MessagesHelper.setTestDialog(TestDialog.OK);
    }

    @Override
    protected void tearDown() throws Exception {
        MessagesHelper.setTestDialog(previous);
        super.tearDown();
    }

    public void testThatGZIsDownloaded() throws IOException {
        DownloadHelper.ToolInstance toolInstance = DownloadHelper.getInstance().downloadIfRequired("tkn", DownloadHelperTest.class.getResource("/tkn-test-gz.json"));
        assertNotNull(toolInstance);
        assertNotNull(toolInstance.getCommand());
        assertEquals("." + File.separatorChar + "cache" + File.separatorChar + "0.5.0" + File.separatorChar + "tkn", toolInstance.getCommand());
        FileUtils.deleteDirectory(Paths.get(toolInstance.getCommand()).toFile().getParentFile());
    }

    public void testThatTarGZIsDownloaded() throws IOException {
        DownloadHelper.ToolInstance toolInstance = DownloadHelper.getInstance().downloadIfRequired("tkn", DownloadHelperTest.class.getResource("/tkn-test-tar.gz.json"));
        assertNotNull(toolInstance);
        assertNotNull(toolInstance.getCommand());
        assertEquals("." + File.separatorChar + "cache" + File.separatorChar + "0.5.0" + File.separatorChar + "tkn", toolInstance.getCommand());
        FileUtils.deleteDirectory(Paths.get(toolInstance.getCommand()).toFile().getParentFile());
    }

    public void testThatPlainFileDownloaded() throws IOException {
        DownloadHelper.ToolInstance toolInstance = DownloadHelper.getInstance().downloadIfRequired("kn", DownloadHelperTest.class.getResource("/knative-test.json"));
        assertNotNull(toolInstance);
        assertNotNull(toolInstance.getCommand());
        assertEquals("." + File.separatorChar + "cache" + File.separatorChar + "0.5.0" + File.separatorChar + "tkn", toolInstance.getCommand());
        FileUtils.deleteDirectory(Paths.get(toolInstance.getCommand()).toFile().getParentFile());
    }

    public void testThatChecksumIsValidForDownloadedTool() throws IOException {
        DownloadHelper.ToolInstance toolInstance = DownloadHelper.getInstance().downloadIfRequired("tkn", DownloadHelperTest.class.getResource("/tkn-test.json"));
        assertNotNull(toolInstance);
        assertNotNull(toolInstance.getCommand());
        FileUtils.deleteDirectory(Paths.get(toolInstance.getCommand()).toFile().getParentFile());
    }

    public void testThatChecksumIsInValidForDownloadedTool() {
      try {
        DownloadHelper.getInstance().downloadIfRequired("tkn", DownloadHelperTest.class.getResource("/tkn-test-invalid-checksum.json"));
        fail("should raise exception");
      } catch (IOException e) {
        assertTrue(e.getMessage().contains("Error while setting tool"));
      }
    }

    public void testThatPlainFileDownloadedInUserSpecificFolder() throws IOException {
        DownloadHelper.ToolInstance toolInstance = DownloadHelper.getInstance().downloadIfRequired("tkn", DownloadHelperTest.class.getResource("/tkn-override-basedir-test.json"));
        assertNotNull(toolInstance);
        assertNotNull(toolInstance.getCommand());
        assertEquals(System.getProperty("tools.dl.path") + File.separatorChar + ".tkn-test" + File.separatorChar + "cache" + File.separatorChar + "0.5.0" + File.separatorChar + "tkn", toolInstance.getCommand());
        FileUtils.deleteDirectory(Paths.get(toolInstance.getCommand()).toFile().getParentFile().getParentFile().getParentFile());
    }
}
