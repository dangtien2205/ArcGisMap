package com.example.tienbi.arcgismap;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.database.MatrixCursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.DrawerLayout;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.LocationDisplayManager;
import com.esri.android.map.LocationDisplayManager.AutoPanMode;
import com.esri.android.map.MapOnTouchListener;
import com.esri.android.map.MapView;
import com.esri.android.map.event.OnPinchListener;
import com.esri.android.map.event.OnStatusChangedListener;
import com.example.tienbi.arcgismap.account.AccountManager;
import com.example.tienbi.arcgismap.basemaps.BasemapsDialogFragment;
import com.example.tienbi.arcgismap.dialogs.ProgressDialogFragment;
import com.example.tienbi.arcgismap.location.DirectionsDialogFragment;
import com.example.tienbi.arcgismap.location.RoutingDialogFragment;
import com.example.tienbi.arcgismap.tools.Compass;
import com.example.tienbi.arcgismap.util.TaskExecutor;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.LinearUnit;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.geometry.Unit;
import com.esri.core.map.CallbackListener;
import com.esri.core.map.Graphic;
import com.esri.core.portal.BaseMap;
import com.esri.core.portal.Portal;
import com.esri.core.portal.WebMap;
import com.esri.core.symbol.PictureMarkerSymbol;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.symbol.SimpleLineSymbol.STYLE;
import com.esri.core.tasks.geocode.Locator;
import com.esri.core.tasks.geocode.LocatorFindParameters;
import com.esri.core.tasks.geocode.LocatorGeocodeResult;
import com.esri.core.tasks.geocode.LocatorReverseGeocodeResult;
import com.esri.core.tasks.geocode.LocatorSuggestionParameters;
import com.esri.core.tasks.geocode.LocatorSuggestionResult;
import com.esri.core.tasks.na.NAFeaturesAsFeature;
import com.esri.core.tasks.na.Route;
import com.esri.core.tasks.na.RouteDirection;
import com.esri.core.tasks.na.RouteParameters;
import com.esri.core.tasks.na.RouteResult;
import com.esri.core.tasks.na.RouteTask;
import com.esri.core.tasks.na.StopGraphic;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;

public class MapFragment extends Fragment implements BasemapsDialogFragment.BasemapsDialogListener,
		RoutingDialogFragment.RoutingDialogListener, OnCancelListener {
	public static final String TAG = MapFragment.class.getSimpleName();

	private static final String KEY_PORTAL_ITEM_ID = "KEY_PORTAL_ITEM_ID";

	private static final String KEY_BASEMAP_ITEM_ID = "KEY_BASEMAP_ITEM_ID";

	private static final String KEY_IS_LOCATION_TRACKING = "IsLocationTracking";

	private static final String COLUMN_NAME_ADDRESS = "address";

	private static final String COLUMN_NAME_X = "x";

	private static final String COLUMN_NAME_Y = "y";

	private static final int REQUEST_CODE_PROGRESS_DIALOG = 1;

	private static final String SEARCH_HINT = "Search";

	private static final String FIND_PLACE = "Find";

	private static final String SUGGEST_PLACE = "Suggest";

	private static FrameLayout.LayoutParams mlayoutParams;

	// Margins parameters for search view
	private static int TOP_MARGIN_SEARCH = 55;

	// The circle area specified by search_radius and input lat/lon serves
	// searching purpose.
	// It is also used to construct the extent which map zooms to after the
	// first
	// GPS fix is retrieved.
	private final static double SEARCH_RADIUS = 10;

	private String mPortalItemId;
	private String mBasemapPortalItemId;
	private FrameLayout mMapContainer;
	public static MapView mMapView;
	private String mMapViewState;
	private SearchView mSearchview;

	// GPS location tracking
	private boolean mIsLocationTracking;
	private Point mLocation = null;

	// Graphics layer to show geocode and reverse geocode results
	private GraphicsLayer mLocationLayer;
	private Point mLocationLayerPoint;
	private String mLocationLayerPointString;

	// Graphics layer to show routes
	private GraphicsLayer mRouteLayer;
	private List<RouteDirection> mRoutingDirections;

	// Spatial references used for projecting points
	private final SpatialReference mWm = SpatialReference.create(102100);
	private final SpatialReference mEgs = SpatialReference.create(4326);
	private MatrixCursor mSuggestionCursor;

	Compass mCompass;
	LayoutParams compassFrameParams;
	private MotionEvent mLongPressEvent;

	@SuppressWarnings("rawtypes")
	// - using this only to cancel pending tasks in a generic way
	private AsyncTask mPendingTask;
	private View mSearchBox;
	private Locator mLocator = Locator.createOnlineLocator();
	private View mSearchResult;
	private LayoutInflater mInflater;
	private String mStartLocation, mEndLocation;
	private LocatorSuggestionParameters suggestParams;

	private final Map<String,Point> suggestMap = new TreeMap<>();
	private static ArrayList<LocatorSuggestionResult> suggestionsList;

	private SpatialReference mapSpatialReference;
	private boolean suggestionClickFlag = false;
	private Point resultEndPoint;
	int width, height;

	LayoutParams gpsFrameParams;

	ImageButton navButton;
	DrawerLayout mDrawerLayout;
	ListView mDrawerList;

	public static MapFragment newInstance(String portalItemId,
			String basemapPortalItemId) {
		MapFragment mapFragment = new MapFragment();

		Bundle args = new Bundle();
		args.putString(KEY_PORTAL_ITEM_ID, portalItemId);
		args.putString(KEY_BASEMAP_ITEM_ID, basemapPortalItemId);

		mapFragment.setArguments(args);
		return mapFragment;
	}

	public MapFragment() {
		// make MapFragment ctor private - use newInstance() instead
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);

		Bundle args = savedInstanceState != null ? savedInstanceState
				: getArguments();
		if (args != null) {
			mIsLocationTracking = args.getBoolean(KEY_IS_LOCATION_TRACKING);
			mPortalItemId = args.getString(KEY_PORTAL_ITEM_ID);
			mBasemapPortalItemId = args.getString(KEY_BASEMAP_ITEM_ID);
		}

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {

		mMapContainer= (FrameLayout)inflater.inflate(R.layout.map_fragment_layout, container, false);

		if (mPortalItemId != null) {
			// load the WebMap
			loadWebMapIntoMapView(mPortalItemId, mBasemapPortalItemId,
					AccountManager.getInstance().getPortal());

		} else {
			if (mBasemapPortalItemId != null) {
				// show a map with the basemap represented by
				// mBasemapPortalItemId
				loadWebMapIntoMapView(mBasemapPortalItemId, null,
						AccountManager.getInstance().getAGOLPortal());

			} else {
				String defaultBaseMapURL = getString(R.string.default_basemap_url);
				MapView mapView = new MapView(getActivity(), defaultBaseMapURL,
						"", "");

				// Finish wiring up map view
				setMapView(mapView);
				// Then zoom to location
				mapView.zoomin();
			}
		}

		return mMapContainer;
	}
	private void setClickListenerForFloatingActionButton(){
		final FloatingActionButton fab = (FloatingActionButton) mMapContainer.findViewById(R.id.fab);
		fab.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d("***", "onClick called with: " + "view = [" + v + "]");
				// Toggle location tracking on or off
				if (mIsLocationTracking) {
					fab.setImageResource(R.drawable.ic_action_compass_mode);
					mMapView.getLocationDisplayManager().setAutoPanMode(
							AutoPanMode.COMPASS);
					mCompass.start();
					mCompass.setVisibility(View.VISIBLE);
					mIsLocationTracking = false;
				} else {
					startLocationTracking();
					fab.setImageResource(android.R.drawable.ic_menu_mylocation);
					if (mMapView.getRotationAngle() != 0) {
						mCompass.setVisibility(View.VISIBLE);
						mCompass.setRotationAngle(mMapView.getRotationAngle());
					} else {
						mCompass.setVisibility(View.GONE);
					}
				}
			}
		});
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);

		// Inflate the menu items for use in the action bar
		inflater.inflate(R.menu.action, menu);

	}

	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public void showMap(String portalItemId, String basemapPortalItemId) {

		// remove existing MapFragment explicitly, simply replacing it can cause
		// the app to freeze when switching basemaps
		FragmentTransaction transaction;
		FragmentManager fragmentManager = getFragmentManager();
		Fragment currentMapFragment = fragmentManager
				.findFragmentByTag(MapFragment.TAG);
		if (currentMapFragment != null) {
			transaction = fragmentManager.beginTransaction();
			transaction.remove(currentMapFragment);
			transaction.commit();
		}

		MapFragment mapFragment = MapFragment.newInstance(portalItemId,
				basemapPortalItemId);

		transaction = fragmentManager.beginTransaction();
		transaction.replace(R.id.maps_app_activity_content_frame, mapFragment,
				MapFragment.TAG);
		transaction.addToBackStack(null);
		transaction.commit();

		getActivity().invalidateOptionsMenu(); // reload the options menu
	}

	@Override
	public void onPause() {
		super.onPause();

		// Pause the MapView and stop the LocationDisplayManager to save battery
		if (mMapView != null) {
			if (mIsLocationTracking) {
				mMapView.getLocationDisplayManager().stop();
				mCompass.stop();
			}
			mMapViewState = mMapView.retainState();

			mMapView.pause();
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		// Start the MapView and LocationDisplayManager running again
		if (mMapView != null) {
			// mCompass.start();
			mMapView.unpause();
			if (mMapViewState != null) {
				mMapView.restoreState(mMapViewState);
			}
			if (mIsLocationTracking) {
				mMapView.getLocationDisplayManager().start();
			}
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putBoolean(KEY_IS_LOCATION_TRACKING, mIsLocationTracking);
		outState.putString(KEY_PORTAL_ITEM_ID, mPortalItemId);
		outState.putString(KEY_BASEMAP_ITEM_ID, mBasemapPortalItemId);
	}

	private void loadWebMapIntoMapView(final String portalItemId,
									   final String basemapPortalItemId, final Portal portal) {

		TaskExecutor.getInstance().getThreadPool().submit(new Callable<Void>() {

			@Override
			public Void call() throws Exception {

				// load a WebMap instance from the portal item
				final WebMap webmap = WebMap.newInstance(portalItemId, portal);

				// load the WebMap that represents the basemap if one was
				// specified
				WebMap basemapWebMap = null;
				if (basemapPortalItemId != null
						&& !basemapPortalItemId.isEmpty()) {
					basemapWebMap = WebMap.newInstance(basemapPortalItemId,
							portal);
				}
				final BaseMap basemap = basemapWebMap != null ? basemapWebMap
						.getBaseMap() : null;

				if (webmap != null) {
					getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							MapView mapView = new MapView(getActivity(),
									webmap, basemap, null, null);

							// mapView.setAllowRotationByPinch(true);

							setMapView(mapView);
							setClickListenerForFloatingActionButton();
							mapView.zoomin();

						}
					});
				} else {
					throw new Exception("Failed to load web map.");
				}
				return null;
			}
		});
	}

	private void setMapView(final MapView mapView) {

		mMapView = mapView;
		mMapView.setEsriLogoVisible(true);
		mMapView.enableWrapAround(true);
		mapView.setAllowRotationByPinch(true);

		// Creating an inflater
		mInflater = (LayoutInflater) getActivity().getSystemService(
				Context.LAYOUT_INFLATER_SERVICE);

		// Setting up the layout params for the searchview and searchresult
		// layout
		mlayoutParams = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP);
		int LEFT_MARGIN_SEARCH = 15;
		int RIGHT_MARGIN_SEARCH = 15;
		int BOTTOM_MARGIN_SEARCH = 0;
		mlayoutParams.setMargins(LEFT_MARGIN_SEARCH, TOP_MARGIN_SEARCH,
				RIGHT_MARGIN_SEARCH, BOTTOM_MARGIN_SEARCH);

		// Add the map to the layout
		mMapContainer.addView(mMapView,0);

		// Set up floating action button
		setClickListenerForFloatingActionButton();


		// Displaying the searchbox layout
		showSearchBoxLayout();

		mMapView.setOnPinchListener(new OnPinchListener() {

			private static final long serialVersionUID = 1L;

			@Override
			public void postPointersDown(float x1, float y1, float x2,
										 float y2, double factor) {
			}

			@Override
			public void postPointersMove(float x1, float y1, float x2,
										 float y2, double factor) {
			}

			@Override
			public void postPointersUp(float x1, float y1, float x2, float y2,
									   double factor) {
			}

			@Override
			public void prePointersDown(float x1, float y1, float x2, float y2,
										double factor) {
			}

			@Override
			public void prePointersMove(float x1, float y1, float x2, float y2,
										double factor) {
				if (mMapView.getRotationAngle() > 5
						|| mMapView.getRotationAngle() < -5) {
					mCompass.setVisibility(View.VISIBLE);
					mCompass.sensorManager.unregisterListener(mCompass.sensorEventListener);
					mCompass.setRotationAngle(mMapView.getRotationAngle());
				}
			}

			@Override
			public void prePointersUp(float x1, float y1, float x2, float y2,
									  double factor) {
			}

		});

		// Setup listener for map initialized
		mMapView.setOnStatusChangedListener(new OnStatusChangedListener() {

			private static final long serialVersionUID = 1L;

			@Override
			public void onStatusChanged(Object source, STATUS status) {

				if (source == mMapView && status == STATUS.INITIALIZED) {

					mapSpatialReference = mMapView.getSpatialReference();

					if (mMapViewState == null) {
						// Starting location tracking will cause zoom to My
						// Location
						startLocationTracking();
					} else {
						mMapView.restoreState(mMapViewState);
					}
					// add search and routing layers
					addGraphicLayers();
				}
			}
		});

		// Setup use of magnifier on a long press on the map
		mMapView.setShowMagnifierOnLongPress(true);
		mLongPressEvent = null;

		// Setup OnTouchListener to detect and act on long-press
		mMapView.setOnTouchListener(new MapOnTouchListener(getActivity(),
				mMapView) {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
					// Start of a new gesture. Make sure mLongPressEvent is
					// cleared.
					mLongPressEvent = null;
				}
				return super.onTouch(v, event);
			}

			@Override
			public void onLongPress(MotionEvent point) {
				// Set mLongPressEvent to indicate we are processing a
				// long-press
				mLongPressEvent = point;
				super.onLongPress(point);

			}

			@Override
			public boolean onDragPointerUp(MotionEvent from,
										   final MotionEvent to) {
				if (mLongPressEvent != null) {
					// This is the end of a long-press that will have displayed
					// the
					// magnifier.
					// Perform reverse-geocoding of the point that was pressed
					Point mapPoint = mMapView.toMapPoint(to.getX(), to.getY());
					ReverseGeocodingAsyncTask reverseGeocodeTask = new ReverseGeocodingAsyncTask();
					reverseGeocodeTask.execute(mapPoint);
					mPendingTask = reverseGeocodeTask;

					mLongPressEvent = null;
					// Remove any previous graphics
					resetGraphicsLayers();
				}
				return super.onDragPointerUp(from, to);
			}

		});

	}

	private void addCompass(int height) {

		mMapContainer.removeView(mCompass);

		// Create the Compass custom view, and add it onto
		// the MapView.
		mCompass = new Compass(mMapView.getContext());
		mCompass.setAlpha(1f);
		mCompass.setRotationAngle(45);
		int HEIGHT = 240;
		int WIDTH = 240;
		compassFrameParams = new FrameLayout.LayoutParams(WIDTH, HEIGHT,
				Gravity.RIGHT);

		int TOP_MARGIN_COMPASS = TOP_MARGIN_SEARCH + height + 45;

		int LEFT_MARGIN_COMPASS = 0;
		int BOTTOM_MARGIN_COMPASS = 0;
		int RIGHT_MARGIN_COMPASS = 0;
		((MarginLayoutParams) compassFrameParams).setMargins(
				LEFT_MARGIN_COMPASS, TOP_MARGIN_COMPASS, RIGHT_MARGIN_COMPASS,
				BOTTOM_MARGIN_COMPASS);

		mCompass.setLayoutParams(compassFrameParams);

		mCompass.setVisibility(View.GONE);

		mCompass.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mCompass.setVisibility(View.GONE);
				mMapView.setRotationAngle(0);
			}
		});

		// Add the compass on the map
		mMapContainer.addView(mCompass);

	}

	private void showRoutingDialogFragment() {

		suggestionClickFlag = false;
		// Show RoutingDialogFragment to get routing start and end points.
		// This calls back to onGetRoute() to do the routing.
		RoutingDialogFragment routingFrag = new RoutingDialogFragment();
		routingFrag.setRoutingDialogListener(this);
		Bundle arguments = new Bundle();
		if (mLocationLayerPoint != null) {
			arguments.putString(RoutingDialogFragment.ARG_END_POINT_DEFAULT,
					mLocationLayerPointString);
		}
		routingFrag.setArguments(arguments);
		routingFrag.show(getFragmentManager(), null);

	}

	private void showDirectionsDialogFragment() {
		// Launch a DirectionsListFragment to display list of directions
		final DirectionsDialogFragment frag = new DirectionsDialogFragment();
		frag.setRoutingDirections(mRoutingDirections,
				new DirectionsDialogFragment.DirectionsDialogListener() {

					@Override
					public void onDirectionSelected(int position) {
						// User has selected a particular direction -
						// dismiss the dialog and
						// zoom to the selected direction
						frag.dismiss();
						RouteDirection direction = mRoutingDirections
								.get(position);
						mMapView.setExtent(direction.getGeometry());
					}

				});
		getFragmentManager().beginTransaction().add(frag, null).commit();

	}

	private void showSearchBoxLayout() {

		// Inflating the layout from the xml file
		mSearchBox = mInflater.inflate(R.layout.searchview, null);
		// Inflate navigation drawer button on SearchView
		navButton = (ImageButton) mSearchBox.findViewById(R.id.btn_nav_menu);
		// Get the navigation drawer from Activity
		mDrawerLayout = (DrawerLayout) getActivity().findViewById(R.id.maps_app_activity_drawer_layout);
		mDrawerList = (ListView) getActivity().findViewById(R.id.maps_app_activity_left_drawer);

		// Set click listener to open/close drawer
		navButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(mDrawerLayout.isDrawerOpen(mDrawerList)){
					mDrawerLayout.closeDrawer(mDrawerList);
				}else{
					mDrawerLayout.openDrawer(mDrawerList);
				}

			}
		});

		// Setting the layout parameters to the layout
		mSearchBox.setLayoutParams(mlayoutParams);

		// Initializing the searchview and the image view
		mSearchview = (SearchView) mSearchBox
				.findViewById(R.id.searchView1);

		ImageView iv_route = (ImageView) mSearchBox
				.findViewById(R.id.imageView1);

		mSearchview.setIconifiedByDefault(false);
		mSearchview.setQueryHint(SEARCH_HINT);

		applySuggestionCursor();

//		navButton = (Button)mSearchBox.findViewById(R.id.navbutton);

		// Adding the layout to the map conatiner
		mMapContainer.addView(mSearchBox);

		// Setup the listener for the route onclick
		iv_route.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				showRoutingDialogFragment();

			}
		});

		// Setup the listener when the search button is pressed on the keyboard
		mSearchview.setOnQueryTextListener(new OnQueryTextListener() {

			@Override
			public boolean onQueryTextSubmit(String query) {
				onSearchButtonClicked(query);
				mSearchview.clearFocus();
				return true;
			}

			@Override
			public boolean onQueryTextChange(String newText) {
				if(mLocator == null)
					return false;
				getSuggestions(newText);
				return true;
			}
		});

		// Add the compass after getting the height of the layout
		mSearchBox.getViewTreeObserver().addOnGlobalLayoutListener(
				new OnGlobalLayoutListener() {
					@Override
					public void onGlobalLayout() {
						addCompass(mSearchBox.getHeight());
						mSearchBox.getViewTreeObserver().removeGlobalOnLayoutListener(this);
					}

				});

		mSearchview.setOnSuggestionListener(new SearchView.OnSuggestionListener() {

			@Override
			public boolean onSuggestionSelect(int position) {
				return false;
			}

			@Override
			public boolean onSuggestionClick(int position) {
				// Obtain the content of the selected suggesting place via cursor
				MatrixCursor cursor = (MatrixCursor) mSearchview.getSuggestionsAdapter().getItem(position);
				int indexColumnSuggestion = cursor.getColumnIndex(COLUMN_NAME_ADDRESS);
				final String address = cursor.getString(indexColumnSuggestion);

				suggestionClickFlag = true;
				// Find the Location of the suggestion
				new FindLocationTask(address).execute(address);

				cursor.close();

				return true;
			}
		});

	}

	private void initSuggestionCursor() {
		String[] cols = new String[]{BaseColumns._ID, COLUMN_NAME_ADDRESS, COLUMN_NAME_X, COLUMN_NAME_Y};
		mSuggestionCursor = new MatrixCursor(cols);
	}

	private void applySuggestionCursor() {
		String[] cols = new String[]{COLUMN_NAME_ADDRESS};
		int[] to = new int[]{R.id.suggestion_item_address};

		SimpleCursorAdapter mSuggestionAdapter = new SimpleCursorAdapter(mMapView.getContext(), R.layout.search_suggestion_item, mSuggestionCursor, cols, to, 0);
		mSearchview.setSuggestionsAdapter(mSuggestionAdapter);
		mSuggestionAdapter.notifyDataSetChanged();
	}

	private void getSuggestions(String query) {
		final CallbackListener<List<LocatorSuggestionResult>> suggestCallback = new CallbackListener<List<LocatorSuggestionResult>>() {
			@Override
			public void onCallback(List<LocatorSuggestionResult> locatorSuggestionResults) {
				final List<LocatorSuggestionResult> locSuggestionResults = locatorSuggestionResults;
				if (locatorSuggestionResults == null)
					return;
				suggestionsList = new ArrayList<>();
				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						int key = 0;
						if (locSuggestionResults.size() > 0) {
							// Add suggestion list to a cursor
							initSuggestionCursor();
							for (final LocatorSuggestionResult result : locSuggestionResults) {

								// add the locatorSuggestionResult to the ArrayList
								suggestionsList.add(result);

								// Add the suggestion results to the cursor
								mSuggestionCursor.addRow(new Object[]{key++, result.getText(), "0", "0"});
							}

							applySuggestionCursor();
						}
					}

				});

			}

			@Override
			public void onError(Throwable throwable) {

				//Log the error
				Log.e(MapFragment.class.getSimpleName(), "No Results found!!");
				Log.e(MapFragment.class.getSimpleName(), throwable.getMessage());
			}
		};

		try {
			// Initialize the locatorSugestion parameters
			locatorParams(SUGGEST_PLACE,query);

			mLocator.suggest(suggestParams, suggestCallback);
		}
		catch (Exception e) {
			Log.e(MapFragment.class.getSimpleName(),"No Results found");
			Log.e(MapFragment.class.getSimpleName(),e.getMessage());
		}
	}

	private void locatorParams(String TYPE, String query) {
		if(TYPE.contentEquals(SUGGEST_PLACE)) {
			// Create suggestion parameters
			suggestParams = new LocatorSuggestionParameters(query);

			// Use the centre of the current map extent as the location
			suggestParams.setLocation(mMapView.getCenter(),
					mMapView.getSpatialReference());

			// Calculate distance for search operation
			Envelope mapExtent = new Envelope();
			mMapView.getExtent().queryEnvelope(mapExtent);

			// assume map is in meters, other units wont work, double
			// current envelope
			double distance = (mapExtent.getWidth() > 0) ? mapExtent
					.getWidth() * 2 : 10000;
			suggestParams.setDistance(distance);



		}

		if (TYPE.contentEquals(FIND_PLACE)) {
			// Create find parameters
			LocatorFindParameters findParams = new LocatorFindParameters(query);

			// Use the centre of the current map extent as the location
			findParams.setLocation(mMapView.getCenter(),
			mMapView.getSpatialReference());

			// Calculate distance for search operation
			Envelope mapExtent = new Envelope();
			mMapView.getExtent().queryEnvelope(mapExtent);

			// assume map is in meters, double current envelope
			double distance = (mapExtent.getWidth() > 0) ? mapExtent
					.getWidth() * 2 : 10000;
			findParams.setDistance(distance);

			findParams.setOutSR(mMapView.getSpatialReference());
		}
	}

	//Fetch the Location from the suggestMap and display it
	private class FindLocationTask extends AsyncTask<String,Void,Point> {
		private Point resultPoint = null;
		private String resultAddress;
		private static final String TAG_LOCATOR_PROGRESS_DIALOG = "TAG_LOCATOR_PROGRESS_DIALOG";

		private ProgressDialogFragment mProgressDialog;

		public FindLocationTask(String address) {
			resultAddress = address;
		}

		@Override
		protected Point doInBackground(String... params) {

			// get the Location for the suggestion from the ArrayList
			for(LocatorSuggestionResult result: suggestionsList) {
				if(resultAddress.matches(result.getText())) {
					try {
						resultPoint = ((mLocator.find(result, 2, null, mapSpatialReference)).get(0)).getLocation();
					} catch (Exception e) {
						Log.e(TAG,"Error in fetching the Location");
						Log.e(TAG,e.getMessage());
					}
				}
			}

			resultEndPoint = resultPoint;

			return resultPoint;
		}

		@Override
		protected void onPreExecute() {
			// Display progress dialog on UI thread
			mProgressDialog = ProgressDialogFragment.newInstance(getActivity()
					.getString(R.string.address_search));
			// set the target fragment to receive cancel notification
			mProgressDialog.setTargetFragment(MapFragment.this,
					REQUEST_CODE_PROGRESS_DIALOG);
			mProgressDialog.show(getActivity().getFragmentManager(),
					TAG_LOCATOR_PROGRESS_DIALOG);
		}

		@Override
		protected void onPostExecute(Point resultPoint) {
			// Dismiss progress dialog
			mProgressDialog.dismiss();
			if (resultPoint == null)
				return;

			// Display the result
			displaySearchResult(resultPoint,resultAddress);
			hideKeyboard();
		}

	}

	protected void hideKeyboard() {

		// Hide soft keyboard
		mSearchview.clearFocus();
		InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		inputManager.hideSoftInputFromWindow(mSearchview.getWindowToken(), 0);
	}

	private void displaySearchResult(Point resultPoint, String address) {

		// create marker symbol to represent location
		Drawable drawable = getActivity().getResources().getDrawable(
				R.drawable.pin_circle_red);
		PictureMarkerSymbol resultSymbol = new PictureMarkerSymbol(
				getActivity(), drawable);
		// create graphic object for resulting location
		Graphic resultLocGraphic = new Graphic(resultPoint,
				resultSymbol);
		// add graphic to location layer
		mLocationLayer.addGraphic(resultLocGraphic);

		mLocationLayerPoint = resultPoint;

		mLocationLayerPointString = address;

		// Zoom map to geocode result location
		mMapView.zoomToResolution(resultPoint, 2);
		showSearchResultLayout(address);
	}

	void resetGraphicsLayers() {
		mLocationLayer.removeAll();
		mRouteLayer.removeAll();
		mLocationLayerPoint = null;
		mLocationLayerPointString = null;
		mRoutingDirections = null;
	}

	void addGraphicLayers() {
		// Add location layer
		if (mLocationLayer == null) {
			mLocationLayer = new GraphicsLayer();
		}
		mMapView.addLayer(mLocationLayer);

		// Add the route graphic layer
		if (mRouteLayer == null) {
			mRouteLayer = new GraphicsLayer();
		}
		mMapView.addLayer(mRouteLayer);
	}

	void startLocationTracking() {
		LocationDisplayManager locDispMgr = mMapView
				.getLocationDisplayManager();
		mCompass.start();
		locDispMgr.setAutoPanMode(AutoPanMode.LOCATION);
		locDispMgr.setAllowNetworkLocation(true);

		locDispMgr.setLocationListener(new LocationListener() {

			boolean locationChanged = false;

			// Zooms to the current location when first GPS fix arrives
			@Override
			public void onLocationChanged(Location loc) {
				double locy = loc.getLatitude();
				double locx = loc.getLongitude();
				Point wgspoint = new Point(locx, locy);
				mLocation = (Point) GeometryEngine.project(wgspoint,
						SpatialReference.create(4326),
						mMapView.getSpatialReference());
				if (!locationChanged) {
					locationChanged = true;
					Unit mapUnit = mMapView.getSpatialReference().getUnit();
					double zoomWidth = Unit.convertUnits(SEARCH_RADIUS,
							Unit.create(LinearUnit.Code.MILE_US), mapUnit);
					Envelope zoomExtent = new Envelope(mLocation,
							zoomWidth / 10, zoomWidth / 10);
					mMapView.setExtent(zoomExtent);
				}
			}

			@Override
			public void onProviderDisabled(String arg0) {
			}

			@Override
			public void onProviderEnabled(String arg0) {
			}

			@Override
			public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
			}
		});
		locDispMgr.start();
		mIsLocationTracking = true;
	}

	@Override
	public void onBasemapChanged(String basemapPortalItemId) {
		((MainActivity) getActivity()).showMap(mPortalItemId,
				basemapPortalItemId);
	}

	public void onSearchButtonClicked(String address) {

		// Hide virtual keyboard
		hideKeyboard();

		// Remove any previous graphics and routes
		resetGraphicsLayers();

		executeLocatorTask(address);
	}

	private void executeLocatorTask(String address) {
		// Create Locator parameters from single line address string
		LocatorFindParameters findParams = new LocatorFindParameters(address);

		// Use the centre of the current map extent as the find location point
		findParams.setLocation(mMapView.getCenter(),
				mMapView.getSpatialReference());

		// Calculate distance for find operation
		Envelope mapExtent = new Envelope();
		mMapView.getExtent().queryEnvelope(mapExtent);
		// assume map is in metres, other units wont work, double current
		// envelope
		double distance = (mapExtent.getWidth() > 0) ? mapExtent
				.getWidth() * 2 : 10000;
		findParams.setDistance(distance);
		findParams.setMaxLocations(2);

		// Set address spatial reference to match map
		findParams.setOutSR(mMapView.getSpatialReference());

		// Execute async task to find the address
		LocatorAsyncTask locatorTask = new LocatorAsyncTask();
		locatorTask.execute(findParams);
		mPendingTask = locatorTask;

		mLocationLayerPointString = address;
	}

	@Override
	public boolean onGetRoute(String startPoint, String endPoint) {
		// Check if we need a location fix
		if (startPoint.equals(getString(R.string.my_location))
				&& mLocation == null) {
			Toast.makeText(getActivity(),
					getString(R.string.need_location_fix), Toast.LENGTH_LONG)
					.show();
			return false;
		}
		// Remove any previous graphics and routes
		resetGraphicsLayers();

		// Do the routing
		executeRoutingTask(startPoint, endPoint);
		return true;
	}

	@SuppressWarnings("unchecked")
	private void executeRoutingTask(String start, String end) {
		resetGraphicsLayers();
		// Create a list of start end point params
		LocatorFindParameters routeStartParams = new LocatorFindParameters(
				start);
		LocatorFindParameters routeEndParams = new LocatorFindParameters(end);
		List<LocatorFindParameters> routeParams = new ArrayList<>();

		// Add params to list
		routeParams.add(routeStartParams);
		routeParams.add(routeEndParams);

		// Execute async task to do the routing
		RouteAsyncTask routeTask = new RouteAsyncTask();
		routeTask.execute(routeParams);
		mPendingTask = routeTask;
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		// a pending task needs to be canceled
		if (mPendingTask != null) {
			mPendingTask.cancel(true);
		}
	}

	private void showSearchResultLayout(String address) {
		// Remove the layouts
		mMapContainer.removeView(mSearchBox);
		mMapContainer.removeView(mSearchResult);

		// Inflate the new layout from the xml file
		mSearchResult = mInflater.inflate(R.layout.search_result, null);

		// Set layout parameters
		mSearchResult.setLayoutParams(mlayoutParams);

		// Initialize the textview and set its text
		TextView tv = (TextView) mSearchResult.findViewById(R.id.textView1);
		tv.setTypeface(null, Typeface.BOLD);
		tv.setText(address);

		// Adding the search result layout to the map container
		mMapContainer.addView(mSearchResult);

		// Setup the listener for the "cancel" icon
		ImageView iv_cancel = (ImageView) mSearchResult
				.findViewById(R.id.imageView3);
		iv_cancel.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// Remove the search result view
				mMapContainer.removeView(mSearchResult);

				suggestionClickFlag = false;
				// Add the search box view
				showSearchBoxLayout();

				// Remove all graphics from the map
				resetGraphicsLayers();

			}
		});

		// Set up the listener for the "Get Directions" icon
		ImageView iv_route = (ImageView) mSearchResult
				.findViewById(R.id.imageView2);
		iv_route.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				onGetRoute(getString(R.string.my_location),
						mLocationLayerPointString);
			}
		});

		// Add the compass after getting the height of the layout
		mSearchResult.getViewTreeObserver().addOnGlobalLayoutListener(
				new OnGlobalLayoutListener() {
					@Override
					public void onGlobalLayout() {
						addCompass(mSearchResult.getHeight());
						mSearchResult.getViewTreeObserver().removeGlobalOnLayoutListener(this);
					}

				});

	}

	private void showRoutingResultLayout(double distance, double time) {

		// Remove the layours
		mMapContainer.removeView(mSearchResult);
		mMapContainer.removeView(mSearchBox);

		// Inflate the new layout from the xml file
		mSearchResult = mInflater.inflate(R.layout.routing_result, null);

		mSearchResult.setLayoutParams(mlayoutParams);

		// Shorten the start and end location by finding the first comma if
		// present
		int index_from = mStartLocation.indexOf(",");
		int index_to = mEndLocation.indexOf(",");
		if (index_from != -1)
			mStartLocation = mStartLocation.substring(0, index_from);
		if (index_to != -1)
			mEndLocation = mEndLocation.substring(0, index_to);

		// Initialize the textvieww and display the text
		TextView tv_from = (TextView) mSearchResult.findViewById(R.id.tv_from);
		tv_from.setTypeface(null, Typeface.BOLD);
		tv_from.setText(" " + mStartLocation);

		TextView tv_to = (TextView) mSearchResult.findViewById(R.id.tv_to);
		tv_to.setTypeface(null, Typeface.BOLD);
		tv_to.setText(" " + mEndLocation);

		// Rounding off the values
		distance = Math.round(distance * 10.0) / 10.0;
		time = Math.round(time * 10.0) / 10.0;

		TextView tv_time = (TextView) mSearchResult.findViewById(R.id.tv_time);
		tv_time.setTypeface(null, Typeface.BOLD);
		tv_time.setText(time + " mins");

		TextView tv_dist = (TextView) mSearchResult.findViewById(R.id.tv_dist);
		tv_dist.setTypeface(null, Typeface.BOLD);
		tv_dist.setText(" (" + distance + " miles)");

		// Adding the layout
		mMapContainer.addView(mSearchResult);

		// Setup the listener for the "Cancel" icon
		ImageView iv_cancel = (ImageView) mSearchResult
				.findViewById(R.id.imageView3);
		iv_cancel.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// Remove the search result view
				mMapContainer.removeView(mSearchResult);
				// Add the default search box view
				showSearchBoxLayout();
				// Remove all graphics from the map
				resetGraphicsLayers();

			}
		});

		// Set up the listener for the "Show Directions" icon
		ImageView iv_directions = (ImageView) mSearchResult
				.findViewById(R.id.imageView2);
		iv_directions.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				showDirectionsDialogFragment();
			}
		});

		// Add the compass after getting the height of the layout
		mSearchResult.getViewTreeObserver().addOnGlobalLayoutListener(
				new OnGlobalLayoutListener() {
					@Override
					public void onGlobalLayout() {
						addCompass(mSearchResult.getHeight());
						mSearchResult.getViewTreeObserver().removeGlobalOnLayoutListener(this);
					}

				});

	}

	/*
	 * This class provides an AsyncTask that performs a geolocation request on a
	 * background thread and displays the first result on the map on the UI
	 * thread.
	 */
	private class LocatorAsyncTask extends
			AsyncTask<LocatorFindParameters, Void, List<LocatorGeocodeResult>> {
		private static final String TAG_LOCATOR_PROGRESS_DIALOG = "TAG_LOCATOR_PROGRESS_DIALOG";

		private Exception mException;

		private ProgressDialogFragment mProgressDialog;

		public LocatorAsyncTask() {
		}

		@Override
		protected void onPreExecute() {
			mProgressDialog = ProgressDialogFragment.newInstance(getActivity()
					.getString(R.string.address_search));
			// set the target fragment to receive cancel notification
			mProgressDialog.setTargetFragment(MapFragment.this,
					REQUEST_CODE_PROGRESS_DIALOG);
			mProgressDialog.show(getActivity().getFragmentManager(),
					TAG_LOCATOR_PROGRESS_DIALOG);
		}

		@Override
		protected List<LocatorGeocodeResult> doInBackground(
				LocatorFindParameters... params) {
			// Perform routing request on background thread
			mException = null;
			List<LocatorGeocodeResult> results = null;

			// Create locator using default online geocoding service and tell it
			// to
			// find the given address
			Locator locator = Locator.createOnlineLocator();
			try {
				results = locator.find(params[0]);
			} catch (Exception e) {
				mException = e;
			}
			return results;
		}

		@Override
		protected void onPostExecute(List<LocatorGeocodeResult> result) {
			// Display results on UI thread
			mProgressDialog.dismiss();
			if (mException != null) {
				Log.w(TAG, "LocatorSyncTask failed with:");
				mException.printStackTrace();
				Toast.makeText(getActivity(),
						getString(R.string.addressSearchFailed),
						Toast.LENGTH_LONG).show();
				return;
			}

			if (result.size() == 0) {
				Toast.makeText(getActivity(),
						getString(R.string.noResultsFound), Toast.LENGTH_LONG)
						.show();
			} else {
				// Use first result in the list
				LocatorGeocodeResult geocodeResult = result.get(0);

				// get return geometry from geocode result
				Point resultPoint = geocodeResult.getLocation();

				// Get the address
				String address = geocodeResult.getAddress();

				// Display the result on the map
				displaySearchResult(resultPoint,address);

			}
		}

	}

	private class RouteAsyncTask extends
			AsyncTask<List<LocatorFindParameters>, Void, RouteResult> {
		private static final String TAG_ROUTE_SEARCH_PROGRESS_DIALOG = "TAG_ROUTE_SEARCH_PROGRESS_DIALOG";

		private Exception mException;

		private ProgressDialogFragment mProgressDialog;

		public RouteAsyncTask() {
		}

		@Override
		protected void onPreExecute() {
			mProgressDialog = ProgressDialogFragment.newInstance(getActivity()
					.getString(R.string.route_search));
			// set the target fragment to receive cancel notification
			mProgressDialog.setTargetFragment(MapFragment.this,
					REQUEST_CODE_PROGRESS_DIALOG);
			mProgressDialog.show(getActivity().getFragmentManager(),
					TAG_ROUTE_SEARCH_PROGRESS_DIALOG);
		}

		@Override
		protected RouteResult doInBackground(
				List<LocatorFindParameters>... params) {
			// Perform routing request on background thread
			mException = null;

			// Define route objects
			List<LocatorGeocodeResult> geocodeStartResult;
			List<LocatorGeocodeResult> geocodeEndResult;
			Point startPoint;
			Point endPoint;

			// Create a new locator to geocode start/end points;
			// by default uses ArcGIS online world geocoding service
			Locator locator = Locator.createOnlineLocator();

			try {
				// Geocode start position, or use My Location (from GPS)
				LocatorFindParameters startParam = params[0].get(0);
				if (startParam.getText()
						.equals(getString(R.string.my_location))) {
					mStartLocation = getString(R.string.my_location);
					startPoint = (Point) GeometryEngine.project(mLocation, mWm,
							mEgs);
				} else {
					geocodeStartResult = locator.find(startParam);
					startPoint = geocodeStartResult.get(0).getLocation();
					mStartLocation = geocodeStartResult.get(0).getAddress();

					if (isCancelled()) {
						return null;
					}
				}

				// Geocode the destination
				LocatorFindParameters endParam = params[0].get(1);
				if (endParam.getText().equals(getString(R.string.my_location))) {
					mEndLocation = getString(R.string.my_location);
					endPoint = (Point) GeometryEngine.project(mLocation, mWm,
							mEgs);
				} else {
					geocodeEndResult = locator.find(endParam);
					if (suggestionClickFlag) {

						endPoint = (Point) GeometryEngine.project(resultEndPoint, mWm, mEgs);
						mEndLocation = endParam.getText();
					} else {
						endPoint = geocodeEndResult.get(0).getLocation();
						mEndLocation = geocodeEndResult.get(0).getAddress();
					}
				}

			} catch (Exception e) {
				mException = e;
				return null;
			}
			if (isCancelled()) {
				return null;
			}

			// Create a new routing task pointing to an ArcGIS Network Analysis
			// Service
			RouteTask routeTask;
			RouteParameters routeParams;
			try {
				routeTask = RouteTask.createOnlineRouteTask(
						getString(R.string.routingservice_url), null);
				// Retrieve default routing parameters
				routeParams = routeTask.retrieveDefaultRouteTaskParameters();
			} catch (Exception e) {
				mException = e;
				return null;
			}
			if (isCancelled()) {
				return null;
			}

			// Customize the route parameters
			NAFeaturesAsFeature routeFAF = new NAFeaturesAsFeature();
			StopGraphic sgStart = new StopGraphic(startPoint);
			StopGraphic sgEnd = new StopGraphic(endPoint);
			routeFAF.setFeatures(new Graphic[]{sgStart, sgEnd});
			routeFAF.setCompressedRequest(true);
			routeParams.setStops(routeFAF);
			routeParams.setOutSpatialReference(mMapView.getSpatialReference());

			// Solve the route
			RouteResult routeResult;
			try {
				routeResult = routeTask.solve(routeParams);
			} catch (Exception e) {
				mException = e;
				return null;
			}
			if (isCancelled()) {
				return null;
			}
			return routeResult;
		}

		@Override
		protected void onPostExecute(RouteResult result) {
			// Display results on UI thread
			mProgressDialog.dismiss();
			if (mException != null) {
				Log.w(TAG, "RouteSyncTask failed with:");
				mException.printStackTrace();
				Toast.makeText(getActivity(),
						getString(R.string.routingFailed), Toast.LENGTH_LONG)
						.show();
				return;
			}

			// Get first item in list of routes provided by server
			Route route;
			try {
				route = result.getRoutes().get(0);
				if( route.getTotalMiles() == 0.0 || route.getTotalKilometers() == 0.0 ) {
					throw new Exception("Can not find the Route");
				}
			} catch (Exception e) {
				Toast.makeText(getActivity(), "We are sorry, we couldn't find the route. Please make " +
								"sure the Source and Destination are different or are connected by road",
						Toast.LENGTH_LONG).show();
				Log.e(TAG,e.getMessage());
				return;
			}


			// Create polyline graphic of the full route
			SimpleLineSymbol lineSymbol = new SimpleLineSymbol(Color.RED, 2,
					STYLE.SOLID);
			Graphic routeGraphic = new Graphic(route.getRouteGraphic()
					.getGeometry(), lineSymbol);

			// Create point graphic to mark start of route
			Point startPoint = ((Polyline) routeGraphic.getGeometry())
					.getPoint(0);
			Graphic startGraphic = createMarkerGraphic(startPoint, false);

			// Create point graphic to mark end of route
			int endPointIndex = ((Polyline) routeGraphic.getGeometry())
					.getPointCount() - 1;
			Point endPoint = ((Polyline) routeGraphic.getGeometry())
					.getPoint(endPointIndex);
			Graphic endGraphic = createMarkerGraphic(endPoint, true);

			// Add these graphics to route layer
			mRouteLayer.addGraphics(new Graphic[]{routeGraphic, startGraphic,
					endGraphic});

			// Zoom to the extent of the entire route with a padding
			mMapView.setExtent(route.getEnvelope(), 100);

			// Save routing directions so user can display them later
			mRoutingDirections = route.getRoutingDirections();

			// Show Routing Result Layout
			showRoutingResultLayout(route.getTotalMiles(),
					route.getTotalMinutes());

		}

		Graphic createMarkerGraphic(Point point, boolean endPoint) {
			Drawable marker = getResources().getDrawable(
					endPoint ? R.drawable.pin_circle_blue
							: R.drawable.pin_circle_red);
			PictureMarkerSymbol destinationSymbol = new PictureMarkerSymbol(
					mMapView.getContext(), marker);
			// NOTE: marker's bounds not set till marker is used to create
			// destinationSymbol
			float offsetY = convertPixelsToDp(getActivity(),
					marker != null ? marker.getBounds().bottom : 0);
			destinationSymbol.setOffsetY(offsetY);
			return new Graphic(point, destinationSymbol);
		}
	}

	public class ReverseGeocodingAsyncTask extends
			AsyncTask<Point, Void, LocatorReverseGeocodeResult> {
		private static final String TAG_REVERSE_GEOCODING_PROGRESS_DIALOG = "TAG_REVERSE_GEOCODING_PROGRESS_DIALOG";

		private Exception mException;

		private ProgressDialogFragment mProgressDialog;

		private Point mPoint;

		@Override
		protected void onPreExecute() {
			mProgressDialog = ProgressDialogFragment.newInstance(getActivity()
					.getString(R.string.reverse_geocoding));
			// set the target fragment to receive cancel notification
			mProgressDialog.setTargetFragment(MapFragment.this,
					REQUEST_CODE_PROGRESS_DIALOG);
			mProgressDialog.show(getActivity().getFragmentManager(),
					TAG_REVERSE_GEOCODING_PROGRESS_DIALOG);
		}

		@Override
		protected LocatorReverseGeocodeResult doInBackground(Point... params) {
			// Perform reverse geocoding request on background thread
			mException = null;
			LocatorReverseGeocodeResult result = null;
			mPoint = params[0];

			// Create locator using default online geocoding service and tell it
			// to
			// find the given point
			Locator locator = Locator.createOnlineLocator();
			try {
				// Our input and output spatial reference will be the same as
				// the map
				SpatialReference mapRef = mMapView.getSpatialReference();
				result = locator.reverseGeocode(mPoint, 100.0, mapRef, mapRef);
				mLocationLayerPoint = mPoint;
			} catch (Exception e) {
				mException = e;
			}
			// return the resulting point(s)
			return result;
		}

		@Override
		protected void onPostExecute(LocatorReverseGeocodeResult result) {
			// Display results on UI thread
			mProgressDialog.dismiss();
			if (mException != null) {
				Log.w(TAG, "LocatorSyncTask failed with:");
				mException.printStackTrace();
				Toast.makeText(getActivity(),
						getString(R.string.addressSearchFailed),
						Toast.LENGTH_LONG).show();
				return;
			}

			// Construct a nicely formatted address from the results
			StringBuilder address = new StringBuilder();
			if (result != null && result.getAddressFields() != null) {
				Map<String, String> addressFields = result.getAddressFields();
				address.append(String.format("%s\n%s, %s %s",
						addressFields.get("Address"),
						addressFields.get("City"), addressFields.get("Region"),
						addressFields.get("Postal")));

				// Draw marker on map.
				// create marker symbol to represent locationhh
				Drawable drawable = getActivity().getResources().getDrawable(
						R.drawable.pin_circle_red);
				PictureMarkerSymbol symbol = new PictureMarkerSymbol(
						getActivity(), drawable);
				mLocationLayer.addGraphic(new Graphic(mPoint, symbol));

				// Address string is saved for use in routing
				mLocationLayerPointString = address.toString();
				// center the map to result location
				mMapView.centerAt(mPoint, true);

				// Show the result on the search result layout
				showSearchResultLayout(address.toString());
			}
		}
	}

	private float convertPixelsToDp(Context context, float px) {
		DisplayMetrics metrics = context.getResources().getDisplayMetrics();
		return px / (metrics.densityDpi / 160f);
	}
}
