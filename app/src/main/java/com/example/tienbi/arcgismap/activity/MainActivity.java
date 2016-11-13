/* Copyright 2016 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * For additional information, contact:
 * Environmental Systems Research Institute, Inc.
 * Attn: Contracts Dept
 * 380 New York Street
 * Redlands, California, USA 92373
 *
 * email: contracts@esri.com
 *
 */

package com.example.tienbi.arcgismap.activity;

import android.Manifest;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.example.tienbi.arcgismap.ContentBrowserFragment;
import com.example.tienbi.arcgismap.DrawerItem;
import com.example.tienbi.arcgismap.MapFragment;
import com.example.tienbi.arcgismap.R;
import com.example.tienbi.arcgismap.account.AccountManager;
import com.example.tienbi.arcgismap.account.SignInActivity;
import com.example.tienbi.arcgismap.basemaps.BasemapsDialogFragment;
import com.example.tienbi.arcgismap.mode.Location;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Entry point into the Maps App.
 */
public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    public static DrawerLayout mDrawerLayout;

    @InjectView(R.id.maps_app_activity_left_drawer)
    ListView mDrawerList;

    private final List<DrawerItem> mDrawerItems = new ArrayList<>();

    private static final int PERMISSION_REQUEST_LOCATION = 0;
    private View mLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.maps_app_activity);

        mLayout = findViewById(R.id.maps_app_activity_content_frame);

        ButterKnife.inject(this);

        setupDrawer();

        setView();

        requestLocationPermission();
    }

    private void requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            Snackbar.make(mLayout, "Location access is required to display the map.",
                    Snackbar.LENGTH_INDEFINITE).setAction("OK", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Request the permission
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            PERMISSION_REQUEST_LOCATION);
                }
            }).show();

        } else {
            Snackbar.make(mLayout,
                    "Permission is not available. Requesting location permission.",
                    Snackbar.LENGTH_SHORT).show();
            // Request the permission. The result will be received in onRequestPermissionResult().
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {

        if (requestCode == PERMISSION_REQUEST_LOCATION) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Snackbar.make(mLayout, "Location permission was granted. Showing map...",
                        Snackbar.LENGTH_SHORT)
                        .show();
                setView();
            } else {
                Snackbar.make(mLayout, "Location permission request was denied.",
                        Snackbar.LENGTH_SHORT)
                        .show();
            }
        }

    }
    @Override
    protected void onResume() {
        super.onResume();
        updateDrawer();
    }

    private void setupDrawer() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.maps_app_activity_drawer_layout);

        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow,
                GravityCompat.START);

        updateDrawer();
    }

    private void setView() {
        if (AccountManager.getInstance().isSignedIn()) {
            showContentBrowser();
        } else {
            showMap(null, "b834a68d7a484c5fb473d4ba90d35e71");
        }
    }

    private void showContentBrowser() {
        FragmentManager fragmentManager = getFragmentManager();
        Fragment browseFragment = fragmentManager
                .findFragmentByTag(ContentBrowserFragment.TAG);
        if (browseFragment == null) {
            browseFragment = new ContentBrowserFragment();
        }

        if (!browseFragment.isVisible()) {
            FragmentTransaction transaction = fragmentManager
                    .beginTransaction();
            transaction.add(R.id.maps_app_activity_content_frame,
                    browseFragment, ContentBrowserFragment.TAG);
            transaction.addToBackStack(null);
            transaction.commit();

            invalidateOptionsMenu(); // reload the options menu
        }

        mDrawerLayout.closeDrawers();
    }

    public void showMap(String portalItemId, String basemapPortalItemId) {
        FragmentTransaction transaction;
        FragmentManager fragmentManager = getFragmentManager();
        Fragment currentMapFragment = fragmentManager
                .findFragmentByTag(MapFragment.TAG);
        if (currentMapFragment != null) {
            transaction = fragmentManager.beginTransaction();
            transaction.remove(currentMapFragment);
            transaction.commit();
        }
        MapFragment mapFragment = null;
        if (getIntent().getAction().equals("ONE_LOCATION")) {
            mapFragment = MapFragment.newInstance(portalItemId,
                    basemapPortalItemId, (Location) getIntent().getSerializableExtra("LOCATION"));
        } else {
            mapFragment = MapFragment.newInstance1(portalItemId,
                    basemapPortalItemId);
        }

        transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.maps_app_activity_content_frame, mapFragment,
                MapFragment.TAG);
        transaction.addToBackStack(null);
        transaction.commit();

        invalidateOptionsMenu(); // reload the options menu
    }

    private void updateDrawer() {
        mDrawerItems.clear();


        DrawerItem item;

        LinearLayout view_basemap = (LinearLayout) getLayoutInflater().inflate(R.layout.drawer_item_layout, null);
        TextView text_drawer_basemap = (TextView) view_basemap.findViewById(R.id.drawer_item_textview);
        ImageView icon_drawer_basemap = (ImageView) view_basemap.findViewById(R.id.drawer_item_icon);
        text_drawer_basemap.setText(getString(R.string.menu_basemaps));
        icon_drawer_basemap.setImageResource(R.drawable.action_basemaps);
        item = new DrawerItem(view_basemap, new DrawerItem.OnClickListener() {

            @Override
            public void onClick() {
                BasemapsDialogFragment basemapsFrag = new BasemapsDialogFragment();
                basemapsFrag.setBasemapsDialogListener(new BasemapsDialogFragment.BasemapsDialogListener() {

                    @Override
                    public void onBasemapChanged(String itemId) {
                        showMap(null,itemId);
                    }
                });
                basemapsFrag.show(getFragmentManager(), null);
                mDrawerLayout.closeDrawers();
            }

        });

        mDrawerItems.add(item);

        BaseAdapter adapter = (BaseAdapter) mDrawerList.getAdapter();
        if (adapter == null) {
            adapter = new DrawerItemListAdapter();
            mDrawerList.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }
    }

    private class DrawerItemClickListener implements OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                                long id) {
            mDrawerItems.get(position).onClicked();
        }
    }

    private class DrawerItemListAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mDrawerItems.size();
        }

        @Override
        public Object getItem(int position) {
            return mDrawerItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            DrawerItem drawerItem = (DrawerItem) getItem(position);
            return drawerItem.getView();
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}
