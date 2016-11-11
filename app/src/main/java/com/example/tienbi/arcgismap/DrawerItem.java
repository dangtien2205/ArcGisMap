package com.example.tienbi.arcgismap;

import android.view.View;
import android.widget.LinearLayout;

public class DrawerItem {
  public interface OnClickListener {
    void onClick();
  }

  private final OnClickListener mListener;

  private final LinearLayout mView;

  public DrawerItem(LinearLayout view, OnClickListener listener) {
    mView = view;
    mListener = listener;
  }

  public void onClicked() {
    if (mListener != null) {
      mListener.onClick();
    }
  }

  public View getView() {
    return mView;
  }
}
