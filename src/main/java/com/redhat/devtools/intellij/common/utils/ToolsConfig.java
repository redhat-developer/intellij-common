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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ToolsConfig {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Tool {
    private Map<String, Platform> platforms = new HashMap<>();

    private String version;
    private String versionCmd;
    private String versionExtractRegExp;
    private String versionMatchRegExpr;
    private String baseDir;
    private boolean silentMode;

    public Map<String, Platform> getPlatforms() {
      return platforms;
    }

    public String getVersion() {
      return version;
    }

    public String getVersionCmd() {
      return versionCmd;
    }

    public String getVersionExtractRegExp() {
      return versionExtractRegExp;
    }

    public String getVersionMatchRegExpr() {
      return versionMatchRegExpr;
    }

    public String getBaseDir() {
      return baseDir;
    }

    public void setBaseDir(String baseDir) {
      this.baseDir = baseDir;
    }

    public boolean isSilentMode() {
      return silentMode;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Platform {
    private URL url;
    private String cmdFileName;
    private String dlFileName;
    private String sha256;

    public URL getUrl() {
      return url;
    }

    public void setUrl(URL url) {
      this.url = url;
    }

    public String getCmdFileName() {
      return cmdFileName;
    }

    public String getDlFileName() {
      return dlFileName;
    }

    public String getSha256() {
      return sha256;
    }

  }

  private Map<String, Tool> tools = new HashMap<>();

  public Map<String, Tool> getTools() {
    return tools;
  }

}
