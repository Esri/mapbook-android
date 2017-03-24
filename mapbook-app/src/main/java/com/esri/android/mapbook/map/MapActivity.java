/*
 *  Copyright 2017 Esri
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *
 *  * For additional information, contact:
 *  * Environmental Systems Research Institute, Inc.
 *  * Attn: Contracts Dept
 *  * 380 New York Street
 *  * Redlands, California, USA 92373
 *  *
 *  * email: contracts@esri.com
 *  *
 *
 */

package com.esri.android.mapbook.map;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.MatrixCursor;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.transition.TransitionManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.esri.android.mapbook.*;
import com.esri.android.mapbook.data.DataManagerCallbacks;
import com.esri.android.mapbook.data.DataManager;
import com.esri.android.mapbook.mapbook.MapbookFragment;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.layers.Layer;
import com.esri.arcgisruntime.layers.LegendInfo;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.LayerList;
import com.esri.arcgisruntime.mapping.MobileMapPackage;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.*;
import com.esri.arcgisruntime.symbology.PictureMarkerSymbol;
import com.esri.arcgisruntime.tasks.geocode.*;

import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.ExecutionException;


public class MapActivity extends AppCompatActivity {
  RecyclerView mRecycleMapContentView = null;
  MapContentAdapter mContentAdapter = null;
  ArcGISMap mMap =null;
  MapView mMapView = null;
  private List<String> mLayerNameList  = null;
  private LocatorTask mLocatorTask = null;
  private ReverseGeocodeParameters mReverseGeocodeParameters = null;
  private Callout mCallout;
  private SearchView mSearchview;
  private String mGraphicPointAddress;
  private Point mGraphicPoint;
  private GeocodeResult mGeocodedLocation;
  private boolean isPinSelected;
  private TextView mCalloutContent;
  private GraphicsOverlay graphicsOverlay;
  private GeocodeParameters mGeocodeParameters;
  private PictureMarkerSymbol mPinSourceSymbol;

  private LayerList mLayerList = null;
  private static final String COLUMN_NAME_ADDRESS = "address";
  private static final String COLUMN_NAME_X = "x";
  private static final String COLUMN_NAME_Y = "y";
  private MatrixCursor mSuggestionCursor;
  private List<SuggestResult> mSuggestionList = null;


  @Inject DataManager mDataManager;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Ask the component to inject this activity
    //DaggerMapbookComponent.builder().mapbookModule(new MapbookModule())

    setContentView(R.layout.map_view);


    final Toolbar toolbar = (Toolbar) findViewById(R.id.mapToolbar);
    mRecycleMapContentView = (RecyclerView) findViewById(R.id.mapContentRecyclerView);
    mRecycleMapContentView.setLayoutManager(new LinearLayoutManager(this));
    mContentAdapter = new MapContentAdapter();
    mRecycleMapContentView.setAdapter(mContentAdapter);

    final Intent intent = getIntent();
    final String mmpkPath = intent.getStringExtra(MapbookFragment.FILE_PATH);
    int index = intent.getIntExtra("INDEX",0);
    final String title = intent.getStringExtra("TITLE");

    if (toolbar != null) {
      final ActionBar actionBar = (this).getSupportActionBar();
      if (actionBar != null) {
        actionBar.setTitle(title );
      }
      toolbar.setNavigationIcon(null);
      toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
        @Override public boolean onMenuItemClick(MenuItem item) {
          if (item.getTitle().toString().equalsIgnoreCase( getString(R.string.layer_action) )) {
            toggleLayerList();
          }
          return false;
        }
      });

    }


    loadMapInView(mmpkPath, index);

  }
  private void setUpOfflineMapGeocoding() {

    // add a graphics overlay
    graphicsOverlay = new GraphicsOverlay();
    graphicsOverlay.setSelectionColor(0xFF00FFFF);
    mMapView.getGraphicsOverlays().add(graphicsOverlay);

    mGeocodeParameters = new GeocodeParameters();
    mGeocodeParameters.getResultAttributeNames().add("*");
    mGeocodeParameters.setMaxResults(1);

    BitmapDrawable startDrawable = (BitmapDrawable) ContextCompat.getDrawable(this, R.drawable.pin);
    mPinSourceSymbol = new PictureMarkerSymbol(startDrawable);
    mPinSourceSymbol.setHeight(90);
    mPinSourceSymbol.setWidth(20);
    mPinSourceSymbol.loadAsync();

    mReverseGeocodeParameters = new ReverseGeocodeParameters();
    mReverseGeocodeParameters.getResultAttributeNames().add("*");
    mReverseGeocodeParameters.setOutputSpatialReference(mMap.getSpatialReference());
    mReverseGeocodeParameters.setMaxResults(1);

    mCalloutContent = new TextView(getApplicationContext());
    mCalloutContent.setTextColor(Color.BLACK);
    mCalloutContent.setTextIsSelectable(true);
  }
  /**
   * Show/hide the list of map layers
   * by adjusting the layout
   */
  private void toggleLayerList() {

    LinearLayout transitionsContainer = (LinearLayout) findViewById(R.id.mapLinearLayout) ;
    TransitionManager.beginDelayedTransition(transitionsContainer);
    if (mRecycleMapContentView.getVisibility() == View.GONE){
      ViewGroup.LayoutParams params = mRecycleMapContentView.getLayoutParams();
      mRecycleMapContentView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT, 4));
      mRecycleMapContentView.requestLayout();
      mRecycleMapContentView.setVisibility(View.VISIBLE);
    }else{
      mRecycleMapContentView.setVisibility(View.GONE);
      mRecycleMapContentView.setLayoutParams(new LinearLayout.LayoutParams(0,
          ViewGroup.LayoutParams.MATCH_PARENT, 4));
      mRecycleMapContentView.requestLayout();
    }
  }

  /**
   *
   * @param menu Menu
   * @return boolean
   */
  @Override
  public boolean onCreateOptionsMenu(final Menu menu) {
    super.onCreateOptionsMenu(menu);
    getMenuInflater().inflate(R.menu.menu, menu);

    // Retrieve the SearchView and plug it into SearchManager
    mSearchview= (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.action_search));
    mSearchview.setQueryHint(getString(R.string.query_hint));
    final SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
    mSearchview.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
    mSearchview.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
      @Override public boolean onQueryTextSubmit(final String query) {

        hideKeyboard();
        geoCodeTypedAddress(query);
        mSearchview.clearFocus();
        return true;
      }

      @Override public boolean onQueryTextChange(final String newText) {

        if (mLocatorTask == null)
          return false;
        getSuggestions(newText);
        return true;
      }
    });

    applySuggestionCursor();

    mSearchview.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
      @Override public boolean onSuggestionSelect(int position) {
        return false;
      }

      @Override public boolean onSuggestionClick(int position) {
        // Obtain the content of the selected suggesting place via
        // cursor
        MatrixCursor cursor = (MatrixCursor) mSearchview.getSuggestionsAdapter().getItem(position);
        int indexColumnSuggestion = cursor.getColumnIndex(COLUMN_NAME_ADDRESS);
        final String address = cursor.getString(indexColumnSuggestion);

        // Find the Location of the suggestion
        geoCodeTypedAddress(address);

        cursor.close();
        mSearchview.setQuery(address,false);
        hideKeyboard();
        return true;
      }
    });
    return true;
  }
  /**
   * Hides soft keyboard
   */
  private void hideKeyboard() {
    mSearchview.clearFocus();
    InputMethodManager inputManager = (InputMethodManager) getApplicationContext()
        .getSystemService(Context.INPUT_METHOD_SERVICE);
    inputManager.hideSoftInputFromWindow(mSearchview.getWindowToken(), 0);
  }


  /**
   * Geocode an address typed in by user
   *
   * @param address
   */
  private void geoCodeTypedAddress(final String address) {
    // Null out any previously located result
    mGeocodedLocation = null;

    // Execute async task to find the address
    mLocatorTask.addDoneLoadingListener(new Runnable() {
      @Override
      public void run() {
        if (mLocatorTask.getLoadStatus() == LoadStatus.LOADED) {
          // Call geocodeAsync passing in an address
          final ListenableFuture<List<GeocodeResult>> geocodeFuture = mLocatorTask.geocodeAsync(address,
              mGeocodeParameters);
          geocodeFuture.addDoneListener(new Runnable() {
            @Override
            public void run() {
              try {
                // Get the results of the async operation
                List<GeocodeResult> geocodeResults = geocodeFuture.get();

                if (geocodeResults.size() > 0) {
                  // Use the first result - for example
                  // display on the map
                  mGeocodedLocation = geocodeResults.get(0);
                  displaySearchResult(mGeocodedLocation.getDisplayLocation(), mGeocodedLocation.getLabel(), true);

                } else {
                  Toast.makeText(getApplicationContext(),
                      getString(R.string.location_not_foud) + address,
                      Toast.LENGTH_LONG).show();
                }

              } catch (InterruptedException | ExecutionException e) {
                // Deal with exception...
                e.printStackTrace();
                Toast.makeText(getApplicationContext(),
                    getString(R.string.geo_locate_error),
                    Toast.LENGTH_LONG).show();

              }
              // Done processing and can remove this listener.
              geocodeFuture.removeDoneListener(this);
            }
          });

        } else {
          Log.i("MapViewActivity", "Trying to reload locator task");
          mLocatorTask.retryLoadAsync();
        }
      }
    });
    mLocatorTask.loadAsync();
  }

  private void displaySearchResult(Point resultPoint, String address, boolean zoomOut) {


    if (mMapView.getCallout().isShowing()) {
      mMapView.getCallout().dismiss();
    }
    //remove any previous graphics/search results
    graphicsOverlay.getGraphics().clear();
    // create graphic object for resulting location
    Graphic resultLocGraphic = new Graphic(resultPoint, mPinSourceSymbol);
    // add graphic to location layer
    graphicsOverlay.getGraphics().add(resultLocGraphic);

    if (zoomOut){
      // Zoom map to geocode result location
      mMapView.setViewpointAsync(new Viewpoint(resultPoint, 8000), 3);
    }


    mGraphicPoint = resultPoint;
    mGraphicPointAddress = address;
  }
  /**
   * Toggle visibility of map layers
   * @param  layerName - String name to toggle
   * @return int - 0 for removal of feature layer, 1 for addition of feature layer
   */
  private int manageLayerVisibility(String layerName){
    int result = -1;
    LayerList layers = mMap.getOperationalLayers();

    Iterator<Layer> iterator = layers.iterator();
    while (iterator.hasNext()){
      Layer l = iterator.next();
      if (l.getName().equalsIgnoreCase(layerName)){
        if (l.isVisible()){
          l.setVisible(false);
          result = 0;
          break;
        }else{
          l.setVisible(true);
          result = 1;
          break;
        }
      }
    }
    return result;
  }

  private void loadMapInView(String mmpkPath, final int index){
    final MobileMapPackage mmp = new MobileMapPackage(mmpkPath);
    mmp.addDoneLoadingListener(new Runnable() {
      @Override public void run() {
        if (mmp.getLoadStatus() == LoadStatus.LOADED) {
          List<ArcGISMap> maps = mmp.getMaps();
          mMap = maps.get(index);
          mMapView= (MapView) findViewById(R.id.mapView);
          mMapView.setMap(mMap);

          mMapView.setOnTouchListener(new MapTouchListener(getApplicationContext(), mMapView));

          setUpOfflineMapGeocoding();

          mLocatorTask = mmp.getLocatorTask();

          mLayerList = mMap.getOperationalLayers();
          mLayerNameList = new ArrayList<String>(mLayerList.size());
          Iterator<Layer> iter = mLayerList.iterator();
          while (iter.hasNext()){
            Layer layer = iter.next();
            if (layer instanceof FeatureLayer){
              ((FeatureLayer) layer).setSelectionWidth(3.0d);
              final ListenableFuture<List<LegendInfo>> legendInfoFuture = layer.fetchLegendInfosAsync();
              legendInfoFuture.addDoneListener(new Runnable() {
                @Override public void run() {
                  try {
                    List<LegendInfo> legendList = legendInfoFuture.get();
                    Log.i("MapViewActivity", "Legend list size " + legendList.size());
                  } catch (InterruptedException e) {
                    e.printStackTrace();
                  } catch (ExecutionException e) {
                    e.printStackTrace();
                  }
                }
              });


            }

            mLayerNameList.add(layer.getName());
          }
          mContentAdapter.setLayerList(mLayerNameList);

        }else{
          Log.e("MapViewActivity", "There was a problem loading the map " + mmp.getLoadStatus().name());
          Throwable err = mmp.getLoadError();
          err.printStackTrace();
          Log.i("MapViewActivity", err.getMessage());
        }
      }
    });
    mmp.loadAsync();
  }

  /**
   * Resume map view
   */
  @Override
  public final void onResume(){
    super.onResume();
    if (mMapView != null){
      mMapView.resume();
    }
  }

  /**
   * Pause map view
   */
  @Override
  public final void onPause() {
    super.onPause();
    if (mMapView != null){
      mMapView.pause();
    }
  }

  /**
   * Initialize Suggestion Cursor
   */
  private void initSuggestionCursor() {
    String[] cols = { BaseColumns._ID, COLUMN_NAME_ADDRESS, COLUMN_NAME_X, COLUMN_NAME_Y};
    mSuggestionCursor = new MatrixCursor(cols);
  }

  /**
   * Set the suggestion cursor to an Adapter then set it to the search view
   */
  private void applySuggestionCursor() {
    String[] cols = { COLUMN_NAME_ADDRESS};
    int[] to = {R.id.suggestion_item_address};
    SimpleCursorAdapter mSuggestionAdapter = new SimpleCursorAdapter(getApplicationContext(),
        R.layout.search_suggestion_item, mSuggestionCursor, cols, to, 0);
    mSearchview.setSuggestionsAdapter(mSuggestionAdapter);
    mSuggestionAdapter.notifyDataSetChanged();
  }

  /**
   * Provide a character by character suggestions for the search string
   *
   * @param query
   *            String typed so far by the user to fetch the suggestions
   */
  private void getSuggestions(final String query) {
    if (query == null || query.isEmpty()) {
      return;
    }
    // Attach a listener to the locator task since
    // the LocatorTask may or may not be loaded the
    // the very first time a user types text into the search box.
    // If the Locator is already loaded, the following listener
    // is invoked immediately.

    mLocatorTask.addDoneLoadingListener(new Runnable() {
      @Override public void run() {
        // Does this locator support suggestions?
        if (mLocatorTask.getLoadStatus().name() != LoadStatus.LOADED.name()){
        } else if (!mLocatorTask.getLocatorInfo().isSupportsSuggestions()){
          return;
        }

        SuggestParameters suggestParameters = new SuggestParameters();
        suggestParameters.setMaxResults(5);
        suggestParameters.setSearchArea(mMapView.getVisibleArea());
        final ListenableFuture<List<SuggestResult>> suggestionsFuture = mLocatorTask.suggestAsync(query, suggestParameters);
        // Attach a done listener that executes upon completion of the async call
        suggestionsFuture.addDoneListener(new Runnable() {
          @Override
          public void run() {
            try {
              // Get the suggestions returned from the locator task.
              // Store retrieved suggestions for future use (e.g. if the user
              // selects a retrieved suggestion, it can easily be
              // geocoded).
              List<SuggestResult> suggestResults = suggestionsFuture.get();
              Log.i("MapViewActivity", "Suggestions returned " + suggestResults.size());
              for (SuggestResult result : suggestResults){
                Log.i("MapViewActivity", result.getLabel());
              }

              showSuggestedPlaceNames(suggestResults);

            } catch (Exception e) {
              Log.e("MapViewActivity", "Error on getting suggestions " + e.getMessage());
            }
          }
        });
      }
    });
    // Initiate the asynchronous call
    mLocatorTask.loadAsync();
  }
  private void showSuggestedPlaceNames(List<SuggestResult> suggestions){
    if (suggestions == null || suggestions.isEmpty()){
      return;
    }
    mSuggestionList = suggestions;
    initSuggestionCursor();
    int key = 0;
    for (SuggestResult result : suggestions) {
      // Add the suggestion results to the cursor
      mSuggestionCursor.addRow(new Object[]{key++, result.getLabel(), "0", "0"});
    }
    applySuggestionCursor();
  }

  public class MapContentAdapter extends RecyclerView.Adapter<RecycleViewContentHolder>{

    private List<String> mLayers ;

    public MapContentAdapter(){}

    public void setLayerList(List layers){

      mLayers = layers;

    }

    @Override public RecycleViewContentHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
      View itemView = LayoutInflater.
          from(viewGroup.getContext()).
          inflate(R.layout.map_content, viewGroup, false);

      return new RecycleViewContentHolder(itemView);
    }

    @Override public void onBindViewHolder(final RecycleViewContentHolder holder, final int position) {
      final String layerName = mLayers.get(position);
      holder.mapContentName.setText(layerName);
      boolean layerVisible = (mLayerList.get(position).isVisible());
      holder.checkBox.setChecked(layerVisible);
      holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
        @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
          manageLayerVisibility(layerName);
        }
      });
    }

    @Override public int getItemCount() {
      if (mLayers == null){
        return 0;
      }else{
        return mLayers.size();
      }

    }
  }
  public class RecycleViewContentHolder extends RecyclerView.ViewHolder{

    public final TextView mapContentName;
  //  public final Button button;
    public final CheckBox checkBox;

    public RecycleViewContentHolder(final View view){
      super(view);
      checkBox = (CheckBox) view.findViewById(R.id.cbLayer) ;
      mapContentName = (TextView) view.findViewById(R.id.txtMapContentName);
    }
  }


  private class MapTouchListener extends DefaultMapViewOnTouchListener {

    public MapTouchListener(Context context, MapView mapView) {
      super(context, mapView);
    }

    @Override
    public void onLongPress(MotionEvent e) {
      android.graphics.Point screenPoint = new android.graphics.Point(Math.round(e.getX()),
          Math.round(e.getY()));

      Point longPressPoint = mMapView.screenToLocation(screenPoint);

      ListenableFuture<List<GeocodeResult>> results = mLocatorTask.reverseGeocodeAsync(longPressPoint,
          mReverseGeocodeParameters);
      results.addDoneListener(new ResultsLoadedListener(results));

    }


    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {


      if (mMapView.getCallout().isShowing()) {
        mMapView.getCallout().dismiss();
      }
      if (graphicsOverlay.getGraphics().size() > 0) {
        if (graphicsOverlay.getGraphics().get(0).isSelected()) {
          isPinSelected = false;
          graphicsOverlay.getGraphics().get(0).setSelected(false);
        }
      }
      // get the screen point where user tapped
      final android.graphics.Point screenPoint = new android.graphics.Point((int) e.getX(), (int) e.getY());

      final Point geometry = mMapView.screenToLocation(screenPoint);

      mDataManager.queryForFeatures(geometry, mMap.getOperationalLayers(), new DataManagerCallbacks.FeatureCallback() {
        @Override public void onFeaturesFound(List<Feature> featureList, FeatureLayer featureLayer) {
          Log.i("MapViewActivity", "Features returned " + featureList.size());

          featureLayer.clearSelection();
          for (Feature f : featureList){
            featureLayer.selectFeature(f);
            Map<String,Object> attributes = f.getAttributes();
            Log.i("MapViewActivity", "Feature layer = " + featureLayer.getName()+  " , keys = " + attributes.toString());

            if(attributes.containsKey("FACILITYID")){
              String facility = attributes.get("FACILITYID").toString();
              Point center = f.getGeometry().getExtent().getCenter();
              displaySearchResult(center, facility,false);
              break;
            }
          }
        }

        @Override public void onNoFeaturesFound() {
          Toast.makeText(getApplicationContext(),
              getString(R.string.feature_not_found),
              Toast.LENGTH_SHORT).show();
          Log.i("MapViewActivity", "No features found");
        }
      });
      // identify graphics on the graphics overlay
      final ListenableFuture<IdentifyGraphicsOverlayResult> identifyGraphic = mMapView.identifyGraphicsOverlayAsync(graphicsOverlay, screenPoint, 1.0, false, 1);

      identifyGraphic.addDoneListener(new Runnable() {
        @Override
        public void run() {
          try {
            IdentifyGraphicsOverlayResult grOverlayResult = identifyGraphic.get();
            // get the list of graphics returned by identify
            List<Graphic> graphic = grOverlayResult.getGraphics();
            // if identified graphic is not empty, start DragTouchListener
            if (!graphic.isEmpty()) {
              mCalloutContent.setText(mGraphicPointAddress);
              // get callout, set content and show
              mCallout = mMapView.getCallout();
              mCallout.setContent(mCalloutContent);
              mCallout.setShowOptions(new Callout.ShowOptions(true,false,false));
              mCallout.getStyle().setLeaderPosition(Callout.Style.LeaderPosition.UPPER_MIDDLE);
              mCallout.setLocation(mGraphicPoint);
              mCallout.show();
            }
          } catch (InterruptedException | ExecutionException ie) {
            ie.printStackTrace();
          }

        }
      });

      return super.onSingleTapConfirmed(e);
    }
  }

  /**
   * Updates marker and callout when new results are loaded.
   */
  private class ResultsLoadedListener implements Runnable {

    private final ListenableFuture<List<GeocodeResult>> results;

    /**
     * Constructs a runnable listener for the geocode results.
     *
     * @param results results from a {@link LocatorTask#geocodeAsync} task
     */
    ResultsLoadedListener(ListenableFuture<List<GeocodeResult>> results) {
      this.results = results;
    }


    @Override
    public void run() {

      try {
        List<GeocodeResult> geocodes = results.get();
        if (geocodes.size() > 0) {
          // get the top result
          GeocodeResult geocode = geocodes.get(0);

          // set the viewpoint to the marker
          Point location = geocode.getDisplayLocation();
          // get attributes from the result for the callout
          String title;
          String detail;
          Object matchAddr = geocode.getAttributes().get("Match_addr");
          if (matchAddr != null) {
            // attributes from a query-based search
            title = matchAddr.toString().split(",")[0];
            detail = matchAddr.toString().substring(matchAddr.toString().indexOf(",") + 1);
          } else {
            // attributes from a click-based search
            String street = geocode.getAttributes().get("Street").toString();
            String city = geocode.getAttributes().get("City").toString();
            String state = geocode.getAttributes().get("State").toString();
            String zip = geocode.getAttributes().get("ZIP").toString();
            title = street;
            detail = city + ", " + state + " " + zip;
          }

          // get attributes from the result for the callout
          HashMap<String, Object> attributes = new HashMap<>();
          attributes.put("title", title);
          attributes.put("detail", detail);


          // create the marker
          Graphic marker = new Graphic(geocode.getDisplayLocation(), attributes, mPinSourceSymbol);
          graphicsOverlay.getGraphics().clear();

          // add the markers to the graphics overlay
          graphicsOverlay.getGraphics().add(marker);

          if (isPinSelected) {
            marker.setSelected(true);
          }
          String calloutText = title + ", " + detail;
          mCalloutContent.setText(calloutText);
          // get callout, set content and show
          mCallout = mMapView.getCallout();
          mCallout.setLocation(geocode.getDisplayLocation());
          mCallout.setContent(mCalloutContent);
          mCallout.setShowOptions(new Callout.ShowOptions(true,false,false));
          mCallout.getStyle().setLeaderPosition(Callout.Style.LeaderPosition.UPPER_MIDDLE);
          mCallout.show();

          mGraphicPoint = location;
          mGraphicPointAddress = title + ", " + detail;
        }

      } catch (Exception e) {
        e.printStackTrace();
      }
    }

  }
}
