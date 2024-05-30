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

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class ToolsConfigTest {
  private static ToolsConfig config;

  @BeforeClass
  public static void init() throws IOException {
    config = ConfigHelper.loadToolsConfig(ToolsConfig.class.getResource("/tkn-test.json"));
  }

  @Test
  public void verifyThatConfigCanLoad() {
    assertNotNull(config);
  }

  @Test
  public void verifyThatConfigReturnsTools() {
    assertNotNull(config);
    ToolsConfig.Tool tool = config.getTools().get("tkn");
    assertNotNull(tool);
  }

  @Test
  public void verifyThatConfigReturnsVersion() {
    assertNotNull(config);
    ToolsConfig.Tool tool = config.getTools().get("tkn");
    assertNotNull(tool);
    assertEquals("0.5.0", tool.getVersion());
  }

  @Test
  public void verifyThatConfigReturnsVersionCmd() {
    assertNotNull(config);
    ToolsConfig.Tool tool = config.getTools().get("tkn");
    assertNotNull(tool);
    assertEquals("version", tool.getVersionCmd());
  }

  @Test
  public void verifyThatConfigReturnsVersionExtractRegExp() {
    assertNotNull(config);
    ToolsConfig.Tool tool = config.getTools().get("tkn");
    assertNotNull(tool);
    assertEquals("Client version: (\\d+[\\.\\d+]*)\\s.*", tool.getVersionExtractRegExp());
  }

  @Test
  public void verifyThatConfigReturnsVersionMatchRegExp() {
    assertNotNull(config);
    ToolsConfig.Tool tool = config.getTools().get("tkn");
    assertNotNull(tool);
    assertEquals("0\\..*", tool.getVersionMatchRegExpr());
  }

  @Test
  public void verifyThatConfigReturnsBaseDir() {
    assertNotNull(config);
    ToolsConfig.Tool tool = config.getTools().get("tkn");
    assertNotNull(tool);
    assertEquals("$HOME/.tekton", tool.getBaseDir());
  }

  @Test
  public void verifyThatConfigReturnsPlatforms() {
    assertNotNull(config);
    ToolsConfig.Tool tool = config.getTools().get("tkn");
    assertNotNull(tool);
    assertNotNull(tool.getPlatforms());
    assertFalse(tool.getPlatforms().isEmpty());
  }

  @Test
  public void verifyThatConfigReturnsWindowsPlatform() {
    assertNotNull(config);
    ToolsConfig.Tool tool = config.getTools().get("tkn");
    assertNotNull(tool);
    assertNotNull(tool.getPlatforms());
    assertFalse(tool.getPlatforms().isEmpty());
    assertNotNull(tool.getPlatforms().get("win"));
  }

  @Test
  public void verifyThatConfigReturnsOSXPlatform() {
    assertNotNull(config);
    ToolsConfig.Tool tool = config.getTools().get("tkn");
    assertNotNull(tool);
    assertNotNull(tool.getPlatforms());
    assertFalse(tool.getPlatforms().isEmpty());
    assertNotNull(tool.getPlatforms().get("osx"));
  }

  @Test
  public void verifyThatConfigReturnsLinuxPlatform() {
    assertNotNull(config);
    ToolsConfig.Tool tool = config.getTools().get("tkn");
    assertNotNull(tool);
    assertNotNull(tool.getPlatforms());
    assertFalse(tool.getPlatforms().isEmpty());
    assertNotNull(tool.getPlatforms().get("lnx"));
  }

  @Test
  public void verifyThatConfigReturnsCorrectChecksums() {
    assertNotNull(config);
    ToolsConfig.Tool tool = config.getTools().get("tkn");
    assertNotNull(tool);
    assertNotNull(tool.getPlatforms());
    assertFalse(tool.getPlatforms().isEmpty());
    assertNotNull(tool.getPlatforms().get("win"));
    assertEquals("50dfa941ccdbe63c112cb28af521f74f3d972cf06ba0092844a20197ddf31de5", tool.getPlatforms().get("win").getSha256());
  }
}
