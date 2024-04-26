package com.redhat.devtools.intellij.common.utils;

import com.intellij.openapi.ui.TestDialog;
import com.intellij.testFramework.LightPlatformTestCase;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class DownloadHelperTest extends LightPlatformTestCase {
    private TestDialog previous;
    @Override
    public void setUp() throws Exception {
        super.setUp();
        previous = MessagesHelper.setTestDialog(TestDialog.OK);
        FileUtils.deleteDirectory(new File("cache"));
    }

    @Override
    protected void tearDown() throws Exception {
        MessagesHelper.setTestDialog(previous);
        super.tearDown();
        FileUtils.deleteDirectory(new File("cache"));
    }

    public void testThatGZIsDownloaded() throws IOException {
        DownloadHelper.ToolInstance toolInstance = DownloadHelper.getInstance().downloadIfRequired("tkn", DownloadHelperTest.class.getResource("/tkn-test-gz.json"));
        assertNotNull(toolInstance);
        assertNotNull(toolInstance.getCommand());
        assertEquals("." + File.separatorChar + "cache" + File.separatorChar + "0.5.0" + File.separatorChar + "tkn", toolInstance.getCommand());
        assertEquals(17, new File(toolInstance.getCommand()).length());
    }

    public void testThatTarGZIsDownloaded() throws IOException {
        DownloadHelper.ToolInstance toolInstance = DownloadHelper.getInstance().downloadIfRequired("tkn", DownloadHelperTest.class.getResource("/tkn-test-tar.gz.json"));
        assertNotNull(toolInstance);
        assertNotNull(toolInstance.getCommand());
        assertEquals("." + File.separatorChar + "cache" + File.separatorChar + "0.5.0" + File.separatorChar + "tkn", toolInstance.getCommand());
        assertEquals(17, new File(toolInstance.getCommand()).length());
    }

    public void testThatPlainFileDownloaded() throws IOException{
        DownloadHelper.ToolInstance toolInstance = DownloadHelper.getInstance().downloadIfRequired("kn", DownloadHelperTest.class.getResource("/knative-test.json"));
        assertNotNull(toolInstance);
        assertNotNull(toolInstance.getCommand());
        assertEquals("." + File.separatorChar + "cache" + File.separatorChar + "0.5.0" + File.separatorChar + "tkn", toolInstance.getCommand());
        assertEquals(17, new File(toolInstance.getCommand()).length());
    }
}
