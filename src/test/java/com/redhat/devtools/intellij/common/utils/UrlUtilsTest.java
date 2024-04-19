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

import org.junit.Test;

public class UrlUtilsTest {

  @Test
  public void should_return_host_for_valid_url() {
    // given
    String url = "https://www.redhat.com";
    // when
    String host = UrlUtils.getHost(url);
    // then
    org.assertj.core.api.Assertions.assertThat(host).isEqualTo("www.redhat.com");
  }

  @Test
  public void should_return_null_for_invalid_url() {
    // given
    String url = "red-carpet";
    // when
    String host = UrlUtils.getHost(url);
    // then
    org.assertj.core.api.Assertions.assertThat(host).isNull();
  }

  @Test
  public void should_return_host_for_url_with_path() {
    // given
    String url = "https://www.redhat.com/en/about/open-source";
    // when
    String host = UrlUtils.getHost(url);
    // then
    org.assertj.core.api.Assertions.assertThat(host).isEqualTo("www.redhat.com");
  }
}
