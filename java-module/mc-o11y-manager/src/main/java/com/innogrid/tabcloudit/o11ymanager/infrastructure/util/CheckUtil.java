package com.innogrid.tabcloudit.o11ymanager.infrastructure.util;

import java.util.Collections;
import java.util.List;

public class CheckUtil {

  public static <T> List<T> emptyIfNull(final List<T> list) {
    return list == null ? Collections.emptyList() : list;
  }


}
