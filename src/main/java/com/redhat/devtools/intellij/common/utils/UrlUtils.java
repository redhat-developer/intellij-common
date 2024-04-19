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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlUtils {

  private static final Pattern HTTP_URL_REGEX = Pattern.compile("https?://((www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6})\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*)");
  private UrlUtils() {}

  /**
   * Returns the host portion of the given url. Returns {@code null} otherwise.
   * @param url the url to get the host portion from
   * @return the host portion of the given url
   */
  public static String getHost(String url) {
    if (url == null) {
      return null;
    }
    Matcher matcher = HTTP_URL_REGEX.matcher(url);
    if (!matcher.matches()) {
      return null;
    }
    return matcher.group(1);
  }

}
