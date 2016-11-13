package com.example.tienbi.arcgismap;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.tienbi.arcgismap.account.AccountManager;
import com.example.tienbi.arcgismap.activity.MainActivity;
import com.example.tienbi.arcgismap.dialogs.ProgressDialogFragment;
import com.example.tienbi.arcgismap.util.TaskExecutor;
import com.esri.core.portal.Portal;
import com.esri.core.portal.PortalItem;
import com.esri.core.portal.PortalItemType;
import com.esri.core.portal.PortalUser;
import com.esri.core.portal.PortalUserContent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class ContentBrowserFragment extends Fragment implements OnClickListener {

    public final static String TAG = ContentBrowserFragment.class.getSimpleName();

    private GridView mMapGrid;

    private View mNoMapsInfo;

    private List<PortalItem> mMaps;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.content_browser_fragment_layout, null);

        mMapGrid = (GridView) view.findViewById(R.id.content_browser_fragment_gridview);
        mMapGrid.setVisibility(View.GONE);

        mNoMapsInfo = view.findViewById(R.id.content_browser_fragment_no_maps_layout);
        mNoMapsInfo.setVisibility(View.GONE);

        View refreshButton = view.findViewById(R.id.content_browser_fragment_refresh_button);
        refreshButton.setOnClickListener(this);

        if (mMaps == null || mMaps.isEmpty()) {
            // fetch the user's maps
            fetchMyMaps();
        } else {
            refreshView();
        }

        return view;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.map_item_linearlayout:
                // a map item has been clicked - open it
                ViewHolder viewHolder = (ViewHolder) view.getTag();
                ((MainActivity) getActivity()).showMap(viewHolder.portalItem.getItemId(), null);
                break;
            case R.id.content_browser_fragment_refresh_button:
                // re-fetch maps
                fetchMyMaps();
                break;
        }
    }

    private void fetchMyMaps() {
        new FetchMapsTask().execute();
    }

    private void refreshView() {
        if (mMaps == null || mMaps.isEmpty()) {
            mMapGrid.setVisibility(View.GONE);
            mNoMapsInfo.setVisibility(View.VISIBLE);
        } else {
            mMapGrid.setVisibility(View.VISIBLE);
            mNoMapsInfo.setVisibility(View.GONE);

            BaseAdapter mapGridAdapter = (BaseAdapter) mMapGrid.getAdapter();
            if (mapGridAdapter == null) {
                mapGridAdapter = new MapGridAdapter();
                mMapGrid.setAdapter(mapGridAdapter);
            } else {
                mapGridAdapter.notifyDataSetChanged();
            }
        }
    }

    private class FetchMapsTask extends AsyncTask<Void, Void, List<PortalItem>> {

        private static final String TAG_FETCH_MAPS_PROGRESS_DIALOG = "TAG_FETCH_MAPS_PROGRESS_DIALOG";

        private ProgressDialogFragment mProgressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            mProgressDialog = ProgressDialogFragment.newInstance(getActivity().getString(R.string.fetching_maps));
            mProgressDialog.show(getActivity().getFragmentManager(), TAG_FETCH_MAPS_PROGRESS_DIALOG);
        }

        @Override
        protected List<PortalItem> doInBackground(Void... params) {

            final List<PortalItem> webMapItems = new ArrayList<>();
            try {
                // fetch the user's maps from the portal
                Portal portal = AccountManager.getInstance().getPortal();
                if (portal != null) {
                    PortalUser portalUser = portal.fetchUser();
                    PortalUserContent content = portalUser != null ? portalUser.fetchContent() : null;
                    List<PortalItem> rootItems = content != null ? content.getItems() : null;
                    if (rootItems != null) {
                        // only select items of type WEBMAP
                        for (PortalItem item : rootItems) {
                            if (item.getType() == PortalItemType.WEBMAP) {
                                webMapItems.add(item);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // fetching content failed
            }

            return webMapItems;
        }

        @Override
        protected void onPostExecute(List<PortalItem> items) {
            super.onPostExecute(items);

            mMaps = items;
            refreshView();

            mProgressDialog.dismiss();
        }
    }

    private class MapGridAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mMaps.size();
        }

        @Override
        public Object getItem(int position) {
            return mMaps.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            ViewHolder viewHolder;
            if (view == null) {
                viewHolder = new ViewHolder();
                view = getActivity().getLayoutInflater().inflate(R.layout.map_item_layout, null);
                view.setOnClickListener(ContentBrowserFragment.this);
                view.setTag(viewHolder);

                viewHolder.title = (TextView) view.findViewById(R.id.map_item_title_textView);
                viewHolder.thumbnailImageView = (ImageView) view.findViewById(R.id.map_item_thumbnail_imageView);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            PortalItem portalItem = mMaps.get(position);

            viewHolder.title.setText(portalItem.getTitle());
            viewHolder.thumbnailImageView.setImageResource(R.drawable.ic_map_thumbnail); // use default thumbnail temporarily
            viewHolder.portalItem = portalItem;
            viewHolder.fetchTumbnail();

            return view;
        }
    }

    private class ViewHolder {
        TextView title;

        ImageView thumbnailImageView;

        PortalItem portalItem;

        Future<Void> thumbnailFetchTask;

        void fetchTumbnail() {
            if (thumbnailFetchTask != null) {
                thumbnailFetchTask.cancel(true);
            }

            thumbnailFetchTask = TaskExecutor.getInstance().getThreadPool().submit(new FetchPortalItemThumbnailTask(this));
        }
    }

    private class FetchPortalItemThumbnailTask implements Callable<Void> {

        private final ViewHolder mViewHolder;

        public FetchPortalItemThumbnailTask(ViewHolder viewHolder) {
            mViewHolder = viewHolder;
        }

        @Override
        public Void call() throws Exception {
            byte[] thumbnailBytes = null;

            // check if task has been cancelled
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }

            if (mViewHolder != null) {
                thumbnailBytes = mViewHolder.portalItem.fetchThumbnail();
            }

            // check if task has been cancelled
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }

            if (thumbnailBytes != null && thumbnailBytes.length > 0) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPurgeable = true;
                final Bitmap bmp = BitmapFactory.decodeByteArray(thumbnailBytes, 0, thumbnailBytes.length, options);

                getActivity().runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        mViewHolder.thumbnailImageView.setImageBitmap(bmp);
                    }
                });
            }

            return null;
        }
    }
}
