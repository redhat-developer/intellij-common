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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.util.text.Strings;
import com.intellij.util.io.HttpRequests;
import com.redhat.devtools.intellij.common.CommonConstants;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DownloadHelper {

    private static final int BUFFER_SIZE = 4096;

    private static final UnaryOperator<InputStream> UNCOMPRESSOR = (input -> {
        try {
            return new CompressorStreamFactory().createCompressorInputStream(input);
        } catch (CompressorException e) {
            throw new RuntimeException(e);
        }
    });

    private static final UnaryOperator<InputStream> UNTAR = (TarArchiveInputStream::new);

    private static final UnaryOperator<InputStream> UNZIP = (ZipArchiveInputStream::new);

    private static final Map<String, UnaryOperator<InputStream>> MAPPERS = new HashMap<>();

    static {
        MAPPERS.put("gz", UNCOMPRESSOR);
        MAPPERS.put("zip", UNZIP);
        MAPPERS.put("tar", UNTAR);
    }

    private DownloadHelper() {
    }

    private static DownloadHelper INSTANCE;

    public static DownloadHelper getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DownloadHelper();
        }
        return INSTANCE;
    }

        /**
         * Download tool if required. First look at PATH then use the configuration file provided by the url to download the
         * tool.
         * The format of the file is the following:
         * <pre>
         * {
         *   "tools": {
         *     "tool": {
         *       "version": "1.0.0",
         *       "versionCmd": "version", //the argument(s) to add to cmdFileName to get the version
         *       "versionExtractRegExp": "", //the regular expression to extract the version string from the version command
         *       "versionMatchRegExpr": "", //the regular expression use to match the extracted version to decide if download if required
         *       "baseDir": "" //the basedir to install to, a sub folder named after version will be created, can use $HOME
         *       "silentMode": true, //if the download needs to be started automatically without user input
         *       "platforms": {
         *         "win": {
         *           "url": "https://tool.com/tool/v1.0.0/tool-windows-amd64.exe.tar.gz",
         *           "cmdFileName": "tool.exe",
         *           "dlFileName": "tool-windows-amd64.exe.gz"
         *           "sha256": "123456789"
         *         },
         *         "osx": {
         *           "url": "https://tool.com/tool/v1.0.0/tool-darwin-amd64.tar.gz",
         *           "cmdFileName": "tool",
         *           "dlFileName": "tool-darwin-amd64.gz"
         *           "sha256": "123456789"
         *         },
         *         "lnx": {
         *           "url": "https://tool.com/tool/v1.0.0/tool-linux-amd64.tar.gz",
         *           "cmdFileName": "tool",
         *           "dlFileName": "tool-linux-amd64.gz"
         *           "sha256": "123456789"
         *         }
         *       }
         *     }
         *   }
         * }
         * </pre>
         *
         * @param toolName the name of the tool to download
         * @param url the URL to the tool description file
         * @return the command path
         * @throws IOException if the tool was not found in the config file
         */
    private CompletableFuture<ToolInstance> downloadIfRequiredAsyncInner(String toolName, URL url) throws IOException {
        CompletableFuture<ToolInstance> result = new CompletableFuture<>();
        ToolsConfig config = ToolsConfigHelper.loadToolsConfig(url);
        ToolsConfig.Tool tool = config.getTools().get(toolName);
        if (tool == null) {
            throw new IOException("Tool " + toolName + " not found in config file " + url);
        }
        ToolsConfig.Platform platform = getPlatformBasedOnOs(tool);
        if (platform == null) {
            throw new IOException("Tool " + toolName + " not found in config file " + url);
        }
        String command = platform.getCmdFileName();
        String version = getVersionFromPath(tool, platform);
        if (!areCompatible(version, tool.getVersionMatchRegExpr())) {
            String replacement = Strings.isEmpty(CommonConstants.TOOLS_DOWNLOAD_PATH) ? CommonConstants.HOME_FOLDER : CommonConstants.TOOLS_DOWNLOAD_PATH;
            Path path = Paths.get(tool.getBaseDir().replace("$HOME", replacement), "cache", tool.getVersion(), command);
            final String cmd = path.toString();
            if (!Files.exists(path)) {
                result = downloadInBackground(toolName, platform, path, cmd, tool, version, platform.getSha256());
            } else {
                result.complete(new ToolInstance(cmd, false));
            }
        } else {
            result.complete(new ToolInstance(command, false));
        }
        return result;
    }

    private ToolsConfig.Platform getPlatformBasedOnOs(ToolsConfig.Tool tool) {
        String osArch = SystemUtils.OS_ARCH;
        String osId = getPlatformOS();
        if (tool.getPlatforms().containsKey(osId + "-" + osArch)) {
                return tool.getPlatforms().get(osId + "-" + osArch);
        }
        return tool.getPlatforms().get(osId);
    }

    private String getPlatformOS() {
        if (SystemUtils.IS_OS_WINDOWS) {
            return "win";
        } else if (SystemUtils.IS_OS_MAC_OSX) {
            return "osx";
        } else if (SystemUtils.IS_OS_LINUX) {
            return "lnx";
        } else {
            throw new IllegalArgumentException("Unsupported operating system " + SystemUtils.OS_NAME);
        }
    }

    private CompletableFuture<ToolInstance> downloadInBackground(String toolName, ToolsConfig.Platform platform, Path path, String cmd, ToolsConfig.Tool tool, String version, String checksum) {
        CompletableFuture<ToolInstance> result = new CompletableFuture<>();
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            downloadInBackgroundManager(toolName, platform, path, cmd, checksum, result);
        } else {
            ApplicationManager.getApplication().invokeLater(() -> {
                if (tool.isSilentMode() || isDownloadAllowed(toolName, version, tool.getVersion())) {
                    downloadInBackgroundManager(toolName, platform, path, cmd, checksum, result);
                } else {
                    result.complete(new ToolInstance(platform.getCmdFileName(), false));
                }
            });
        }
        return result;
    }

    private void downloadInBackgroundManager(String toolName, ToolsConfig.Platform platform, Path path, String cmd, String checksum, CompletableFuture<ToolInstance> result) {
        final Path dlFilePath = path.resolveSibling(platform.getDlFileName());
        ProgressManager.getInstance().run(new Task.Backgroundable(null, "Downloading " + toolName, false) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                try {
                    HttpRequests.request(platform.getUrl().toString()).useProxy(true).connect(request -> {
                        downloadFile(request.getInputStream(), dlFilePath, progressIndicator, request.getConnection().getContentLength());
                        uncompress(dlFilePath, path);
                        if (checksum != null && !verify(dlFilePath, checksum)){
                            throw new IOException("Failed to verify checksum for " + platform.getDlFileName());
                        }
                        return cmd;
                    });
                } catch (IOException e) {
                    result.completeExceptionally(new IOException("Error while setting tool " + toolName + ".", e));
                }
            }

            @Override
            public void onFinished() {
                if (!result.isCompletedExceptionally()) {
                    result.complete(new ToolInstance(cmd, true));
                }
            }
        });
    }

    public ToolInstance downloadIfRequired(String toolName, URL url) throws IOException {
        CompletableFuture<ToolInstance> future = downloadIfRequiredAsyncInner(toolName, url);
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        } catch (ExecutionException e) {
            throw new IOException(e);
        }
    }

    public CompletableFuture<ToolInstance> downloadIfRequiredAsync(String toolName, URL url) {
        try {
            return downloadIfRequiredAsyncInner(toolName, url);
        } catch (IOException e) {
            CompletableFuture<ToolInstance> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    private boolean isDownloadAllowed(String tool, String currentVersion, String requiredVersion) {
        return UIHelper.executeInUI(() ->
          Messages.showYesNoCancelDialog(StringUtil.isEmpty(currentVersion) ? tool + " not found , do you want to download " + tool + " " + requiredVersion + " ?" : tool + " " + currentVersion + " found, required version is " + requiredVersion + ", do you want to download " + tool + " ?", tool + " tool required", Messages.getQuestionIcon()) == Messages.YES);
    }

    private boolean areCompatible(String version, String versionMatchRegExpr) {
        boolean compatible = true;
        if (!StringUtil.isEmptyOrSpaces(versionMatchRegExpr)) {
            Pattern pattern = Pattern.compile(versionMatchRegExpr);
            compatible = pattern.matcher(version).matches();
        } else if (StringUtil.isEmptyOrSpaces(version)) {
            compatible = false;
        }
        return compatible;
    }

    private String getVersionFromPath(ToolsConfig.Tool tool, ToolsConfig.Platform platform) {
        String version = "";
        try {
            Pattern pattern = Pattern.compile(tool.getVersionExtractRegExp());
            String[] arguments = tool.getVersionCmd().split(" ");
            String output = ExecHelper.execute(platform.getCmdFileName(), false, arguments);
            try (BufferedReader reader = new BufferedReader(new StringReader(output))) {
                version = reader.lines().
                        map(pattern::matcher).
                        filter(Matcher::matches).
                        map(matcher -> matcher.group(1)).
                        findFirst().orElse("");
            }
        } catch (IOException e) {
          // swallow
        }
        return version;

    }

    private static void downloadFile(InputStream input, Path dlFileName, ProgressIndicator progressIndicator, long size) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        Files.createDirectories(dlFileName.getParent());
        try (OutputStream output = Files.newOutputStream(dlFileName)) {
            int lg;
            long accumulated = 0;
            while (((lg = input.read(buffer)) > 0) && !progressIndicator.isCanceled()) {
                output.write(buffer, 0, lg);
                accumulated += lg;
                progressIndicator.setFraction((double) accumulated / size);
            }
        }
    }

    private InputStream mapStream(String filename, InputStream input) {
        String extension;
        while (((extension = FilenameUtils.getExtension(filename)) != null) && MAPPERS.containsKey(extension)) {
            filename = FilenameUtils.removeExtension(filename);
            input = MAPPERS.get(extension).apply(input);
        }
        return input;
    }

    private void uncompress(Path dlFilePath, Path cmd) throws IOException {
        try (InputStream input = new BufferedInputStream(Files.newInputStream(dlFilePath))) {
            InputStream subStream = mapStream(dlFilePath.toString(), input);
            if (subStream instanceof ArchiveInputStream) {
                ArchiveEntry entry;

                while ((entry = ((ArchiveInputStream)subStream).getNextEntry()) != null) {
                    save(subStream, cmd.resolveSibling(entry.getName()), entry.getSize());
                }
            } else {
                // we already has cmd downloaded, so just set executable bit
                if(cmd.equals(dlFilePath)) {
                    cmd.toFile().setExecutable(true);
                    return;
                }
                save(subStream, cmd, -1L);
            }
        } catch (RuntimeException e) {
            throw new IOException(e);
        }
    }

    private void save(InputStream source, Path destination, long length) throws IOException {
        try (OutputStream stream = Files.newOutputStream(destination)) {
            if (length == -1L) {
                IOUtils.copy(source, stream);
            } else {
                IOUtils.copyLarge(source, stream, 0L, length);
            }
        }
        destination.toFile().setExecutable(true);
    }

    private boolean verify(Path path, String checksum) throws IOException {
        try (InputStream stream = Files.newInputStream(path)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] buffer = new byte[BUFFER_SIZE];
            int read = stream.read(buffer, 0, BUFFER_SIZE);
            while (read > -1) {
                digest.update(buffer, 0, read);
                read = stream.read(buffer, 0, BUFFER_SIZE);
            }
            return MessageDigest.isEqual(digest.digest(), Hex.decodeHex(checksum));
        } catch (NoSuchAlgorithmException | DecoderException e) {
            throw new IOException("Could not verify checksum for file " + path, e);
        }
    }

    public static class ToolInstance {
        private final String command;
        private final boolean isDownloaded;
        public ToolInstance(String command, boolean isDownloaded) {
            this.command = command;
            this.isDownloaded = isDownloaded;
        }

        public String getCommand() {
            return command;
        }

        public boolean isDownloaded() {
            return isDownloaded;
        }
    }

}
