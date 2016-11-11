package com.example.tienbi.arcgismap.util;

import android.content.res.Resources;

public class UiUtils {

  public static int dipsToPixels(int dips) {
    if (dips == 0) {
      return 0;
    }

    final float scale = Resources.getSystem().getDisplayMetrics().density;
    return (int) (dips * scale + 0.5f);
  }

}
