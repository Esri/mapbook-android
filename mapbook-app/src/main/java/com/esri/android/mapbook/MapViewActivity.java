/* Copyright 2017 Esri
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
package com.esri.android.mapbook;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.transition.TransitionManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
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
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.layers.Layer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.LayerList;
import com.esri.arcgisruntime.mapping.MobileMapPackage;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.*;
import com.esri.arcgisruntime.symbology.PictureMarkerSymbol;
import com.esri.arcgisruntime.tasks.geocode.GeocodeParameters;
import com.esri.arcgisruntime.tasks.geocode.GeocodeResult;
import com.esri.arcgisruntime.tasks.geocode.LocatorTask;
import com.esri.arcgisruntime.tasks.geocode.ReverseGeocodeParameters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;


public class MapViewActivity extends AppCompatActivity {
  RecyclerView mRecycleMapContentView = null;
  MapContentAdapter mContentAdapter = null;
  ArcGISMap mMap =null;
  MapView mMapView = null;
  private List<Layer> mRemovedLayers = new ArrayList<>();
  private List<String> mLayerNameList  = null;
  private LocatorTask mLocatorTask = null;
  private ReverseGeocodeParameters mReverseGeocodeParameters = null;
  private Callout mCallout;
  private SearchView mSearchview;
  private String mGraphicPointAddress;
  private Point mGraphicPoint;
  private GeocodeResult mGeocodedLocation;
  Spinner mSpinner;
  private boolean isPinSelected;
  private TextView mCalloutContent;
  private GraphicsOverlay graphicsOverlay;
  private GeocodeParameters mGeocodeParameters;
  private PictureMarkerSymbol mPinSourceSymbol;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.map_view);


    final Toolbar toolbar = (Toolbar) findViewById(R.id.mapToolbar);
    mRecycleMapContentView = (RecyclerView) findViewById(R.id.mapContentRecyclerView);
    mRecycleMapContentView.setLayoutManager(new LinearLayoutManager(this));
    mContentAdapter = new MapContentAdapter();
    mRecycleMapContentView.setAdapter(mContentAdapter);

    if (toolbar != null) {
      setSupportActionBar(toolbar);
      final ActionBar actionBar = (this).getSupportActionBar();
      if (actionBar != null) {
        actionBar.setTitle(R.string.title);
      }
      toolbar.setNavigationIcon(null);
      toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
        @Override public boolean onMenuItemClick(MenuItem item) {
          if (item.getTitle().toString().equalsIgnoreCase( "Layers" )) {
            toggleLayerList();
          }
          if (item.getTitle().toString().equalsIgnoreCase("Main Menu")){
            finish();
          }
          return false;
        }
      });

    }

    Intent intent = getIntent();
    String mmpkPath = intent.getStringExtra(MainActivity.FILE_PATH);
    int index = intent.getIntExtra("INDEX",0);
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
  //  mPinSourceSymbol.setLeaderOffsetY(45);


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
        return false;
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
                  displaySearchResult(mGeocodedLocation.getDisplayLocation(), mGeocodedLocation.getLabel());

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

  private void displaySearchResult(Point resultPoint, String address) {


    if (mMapView.getCallout().isShowing()) {
      mMapView.getCallout().dismiss();
    }
    //remove any previous graphics/search results
    graphicsOverlay.getGraphics().clear();
    // create graphic object for resulting location
    Graphic resultLocGraphic = new Graphic(resultPoint, mPinSourceSymbol);
    // add graphic to location layer
    graphicsOverlay.getGraphics().add(resultLocGraphic);

    // Zoom map to geocode result location
    mMapView.setViewpointAsync(new Viewpoint(resultPoint, 8000), 3);

    mGraphicPoint = resultPoint;
    mGraphicPointAddress = address;
  }
  /**
   * Toggle visibility of map layers
   * @param  layerName - String name to toggle
   * @return int - 0 for removal of feature layer, 1 for addition of feature layer
   */
  private int manageLayerSelection(String layerName){
    int result = -1;
    LayerList layers = mMap.getOperationalLayers();
    Layer foundLayer = null;
    Iterator<Layer> iterator = layers.iterator();
    while (iterator.hasNext()){
      Layer l = iterator.next();
      if (l.getName().equalsIgnoreCase(layerName)){
        foundLayer = l; // Layer is currently in the map and visible
        Log.i("MapViewActivity", "Found layer in map...");
        break;
      }
    }
    if (foundLayer != null){
      mRemovedLayers.add(foundLayer);
      mMap.getOperationalLayers().remove(foundLayer);
      result = 0;
    }else{ // Layer has been previously removed
      for (Layer layer : mRemovedLayers){
        if (layer.getName().equalsIgnoreCase(layerName)){
          foundLayer = layer;
          break;
        }
      }
      if (foundLayer != null){
        Log.i("MapViewActivity", "Found layer in removed layers...");
        mRemovedLayers.remove(foundLayer);
        mMap.getOperationalLayers().add(foundLayer);
        result = 1;
      }else{
        Log.e("MapViewActivity", "Programming error!");
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

          LayerList layers = mMap.getOperationalLayers();
          mLayerNameList = new ArrayList<String>(layers.size());
          Iterator<Layer> iter = layers.iterator();
          while (iter.hasNext()){
            Layer layer = iter.next();
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
      if (holder != null && holder.mapContentName!= null){

        holder.mapContentName.setText(mLayers.get(position));
        holder.mapContentName.setOnClickListener(new View.OnClickListener() {
          @Override public void onClick(View v) {
            int visibility = manageLayerSelection(mLayers.get(position));
            if (visibility  == 0){
              holder.button.setBackgroundResource(R.drawable.ic_visibility_black_36px);
            } else if (visibility == 1){
              holder.button.setBackgroundResource(R.drawable.ic_visibility_off_black_36px);
            }
          }
        });
        holder.button.setOnClickListener(new View.OnClickListener() {
          @Override public void onClick(View v) {
            int visibility = manageLayerSelection(mLayers.get(position));
            if (visibility  == 0){
              holder.button.setBackgroundResource(R.drawable.ic_visibility_black_36px);
            } else if (visibility == 1){
              holder.button.setBackgroundResource(R.drawable.ic_visibility_off_black_36px);
            }
          }
        });
      }

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

    public final LinearLayout linearLayout;
    public final TextView mapContentName;
    public final Button button;

    public RecycleViewContentHolder(final View view){
      super(view);

      mapContentName = (TextView) view.findViewById(R.id.txtMapContentName);
      button = (Button) view.findViewById(R.id.btnHide);
      linearLayout = (LinearLayout) findViewById(R.id.mapContentLinearLayout);

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

              if (!isPinSelected) {
                isPinSelected = true;
                graphic.get(0).setSelected(true);
                Toast.makeText(getApplicationContext(),
                    getString(R.string.reverse_geocode_message),
                    Toast.LENGTH_SHORT).show();

              }

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
