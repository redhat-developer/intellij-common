/*******************************************************************************
 * Copyright (c) 2025 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.common.utils;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.EnvironmentUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KubeconfigEnvValue {

  private static final String KUBECONFIG_KEY = "KUBECONFIG";

  private static final Logger LOGGER = LoggerFactory.getLogger(KubeconfigEnvValue.class);

  private KubeconfigEnvValue() {
    // inhibit instantiation
  }

  /**
   * Copies the "KUBECONFIG" env variable value to the system properties when running on MacOS.
   * This env variable is used to list multiple config files and is supported by `kubectl`
   * and the kubernetes-client.
   *
   * example:
   * ```
   * export KUBECONFIG=${HOME}/.kube/config:${HOME}/.kube/minikube.yaml
   * ```
   * On MacOS env variables present in the shell don't exist in IDEA
   * because applications that are launched from the dock don't get
   * env variables that are exported for the shell (export in `~/.zshrc`, `~/.bashrc`, `~/.zprofile`, etc.).
   * They are therefore not present in [System.getProperties].
   * This method copies the shell env KUBECONFIG variable to the System properties.
   *
   * See <a href="https://github.com/redhat-developer/intellij-kubernetes/issues/826">issue #826</a>
   */
  public static void copyToSystem() {
    if (!SystemInfo.isMac) {
      return;
    }
    String current = System.getProperty(KUBECONFIG_KEY);
    if (!StringUtil.isEmpty(current)) {
      LOGGER.info("Current KUBECONFIG value is " + current + ".");
      return;
    }
    String shellValue = EnvironmentUtil.getValue(KUBECONFIG_KEY);
    if (StringUtil.isEmpty(shellValue)) {
      return;
    }
    LOGGER.info("Copying KUBECONFIG value " + shellValue+ " from shell to System.");
    System.getProperties().put(KUBECONFIG_KEY, shellValue);
    System.getProperties().put(KUBECONFIG_KEY.toLowerCase(), shellValue);
  }


}
