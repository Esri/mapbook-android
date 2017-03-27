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

import com.esri.android.mapbook.util.ActivityUtils;
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

  ArcGISMap mMap =null;

  private List<Layer> mLayerNameList  = null;
  private LocatorTask mLocatorTask = null;

  private Callout mCallout;

  private GeocodeResult mGeocodedLocation;

  private boolean isPinSelected;

  private LayerList mLayerList = null;

  private List<SuggestResult> mSuggestionList = null;


  @Inject DataManager mDataManager;
  @Inject MapPresenter mMapPresenter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);


    setContentView(R.layout.map_view);

    initialize();
  }

  private void initialize(){

    final Intent intent = getIntent();
    final String mmpkPath = intent.getStringExtra(MapbookFragment.FILE_PATH);
    int index = intent.getIntExtra("INDEX",0);


    MapFragment fragment = (MapFragment) getSupportFragmentManager().findFragmentById(R.id.mapLinearLayout);
    if (fragment == null){
      fragment = MapFragment.newInstance();
      Bundle args = fragment.getArguments();
      args.putString(MapbookFragment.FILE_PATH, mmpkPath);
      args.putInt("INDEX", index);
      ActivityUtils.addFragmentToActivity(getSupportFragmentManager(), fragment, R.id.mapLinearLayout);
    }
    // Ask the component to inject this activity
    DaggerMapComponent.builder().applicationComponent(((MapBookApplication) getApplication())
        .getComponent()).applicationModule(new ApplicationModule(getApplicationContext())).mapModule(new MapModule(fragment)).build().inject(this);

  }







//  private void loadMapInView(String mmpkPath, final int index){
//    final MobileMapPackage mmp = new MobileMapPackage(mmpkPath);
//    mmp.addDoneLoadingListener(new Runnable() {
//      @Override public void run() {
//        if (mmp.getLoadStatus() == LoadStatus.LOADED) {
//          List<ArcGISMap> maps = mmp.getMaps();
//          mMap = maps.get(index);
//          mMapView= (MapView) findViewById(R.id.mapView);
//          mMapView.setMap(mMap);
//
//          mMapView.setOnTouchListener(new MapTouchListener(getApplicationContext(), mMapView));
//
//          setUpOfflineMapGeocoding();
//
//          mLocatorTask = mmp.getLocatorTask();
//
//          mLayerList = mMap.getOperationalLayers();
//          mLayerNameList = new ArrayList<Layer>(mLayerList.size());
//          Iterator<Layer> iter = mLayerList.iterator();
//          while (iter.hasNext()){
//            Layer layer = iter.next();
//            if (layer instanceof FeatureLayer){
//              ((FeatureLayer) layer).setSelectionWidth(3.0d);
//              final ListenableFuture<List<LegendInfo>> legendInfoFuture = layer.fetchLegendInfosAsync();
//              legendInfoFuture.addDoneListener(new Runnable() {
//                @Override public void run() {
//                  try {
//                    List<LegendInfo> legendList = legendInfoFuture.get();
//                    Log.i("MapViewActivity", "Legend list size " + legendList.size());
//                  } catch (InterruptedException e) {
//                    e.printStackTrace();
//                  } catch (ExecutionException e) {
//                    e.printStackTrace();
//                  }
//                }
//              });
//
//
//            }
//
//            mLayerNameList.add(layer);
//          }
//          mContentAdapter.setLayerList(mLayerNameList);
//
//        }else{
//          Log.e("MapViewActivity", "There was a problem loading the map " + mmp.getLoadStatus().name());
//          Throwable err = mmp.getLoadError();
//          err.printStackTrace();
//          Log.i("MapViewActivity", err.getMessage());
//        }
//      }
//    });
//    mmp.loadAsync();
//  }





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

//    mLocatorTask.addDoneLoadingListener(new Runnable() {
//      @Override public void run() {
//        // Does this locator support suggestions?
//        if (mLocatorTask.getLoadStatus().name() != LoadStatus.LOADED.name()){
//        } else if (!mLocatorTask.getLocatorInfo().isSupportsSuggestions()){
//          return;
//        }
//
//        SuggestParameters suggestParameters = new SuggestParameters();
//        suggestParameters.setMaxResults(5);
//        suggestParameters.setSearchArea(mMapView.getVisibleArea());
//        final ListenableFuture<List<SuggestResult>> suggestionsFuture = mLocatorTask.suggestAsync(query, suggestParameters);
//        // Attach a done listener that executes upon completion of the async call
//        suggestionsFuture.addDoneListener(new Runnable() {
//          @Override
//          public void run() {
//            try {
//              // Get the suggestions returned from the locator task.
//              // Store retrieved suggestions for future use (e.g. if the user
//              // selects a retrieved suggestion, it can easily be
//              // geocoded).
//              List<SuggestResult> suggestResults = suggestionsFuture.get();
//              Log.i("MapViewActivity", "Suggestions returned " + suggestResults.size());
//              for (SuggestResult result : suggestResults){
//                Log.i("MapViewActivity", result.getLabel());
//              }
//
//              showSuggestedPlaceNames(suggestResults);
//
//            } catch (Exception e) {
//              Log.e("MapViewActivity", "Error on getting suggestions " + e.getMessage());
//            }
//          }
//        });
//      }
//    });
    // Initiate the asynchronous call
    mLocatorTask.loadAsync();
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

//      ListenableFuture<List<GeocodeResult>> results = mLocatorTask.reverseGeocodeAsync(longPressPoint,
//          mReverseGeocodeParameters);
//      results.addDoneListener(new ResultsLoadedListener(results));

    }


    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {


      if (mMapView.getCallout().isShowing()) {
        mMapView.getCallout().dismiss();
      }
//      if (graphicsOverlay.getGraphics().size() > 0) {
//        if (graphicsOverlay.getGraphics().get(0).isSelected()) {
//          isPinSelected = false;
//          graphicsOverlay.getGraphics().get(0).setSelected(false);
//        }
//      }
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
            //  displaySearchResult(center, facility,false);
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
    //  final ListenableFuture<IdentifyGraphicsOverlayResult> identifyGraphic = mMapView.identifyGraphicsOverlayAsync(graphicsOverlay, screenPoint, 1.0, false, 1);

//      identifyGraphic.addDoneListener(new Runnable() {
//        @Override
//        public void run() {
//          try {
//            IdentifyGraphicsOverlayResult grOverlayResult = identifyGraphic.get();
//            // get the list of graphics returned by identify
//            List<Graphic> graphic = grOverlayResult.getGraphics();
//            // if identified graphic is not empty, start DragTouchListener
//            if (!graphic.isEmpty()) {
////              mCalloutContent.setText(mGraphicPointAddress);
////              // get callout, set content and show
////              mCallout = mMapView.getCallout();
////              mCallout.setContent(mCalloutContent);
////              mCallout.setShowOptions(new Callout.ShowOptions(true,false,false));
////              mCallout.getStyle().setLeaderPosition(Callout.Style.LeaderPosition.UPPER_MIDDLE);
////              mCallout.setLocation(mGraphicPoint);
////              mCallout.show();
//            }
//          } catch (InterruptedException | ExecutionException ie) {
//            ie.printStackTrace();
//          }
//
//        }
//      });

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
//          Graphic marker = new Graphic(geocode.getDisplayLocation(), attributes, mPinSourceSymbol);
//          graphicsOverlay.getGraphics().clear();
//
//          // add the markers to the graphics overlay
//          graphicsOverlay.getGraphics().add(marker);
//
//          if (isPinSelected) {
//            marker.setSelected(true);
//          }
//          String calloutText = title + ", " + detail;
//          mCalloutContent.setText(calloutText);
//          // get callout, set content and show
//          mCallout = mMapView.getCallout();
//          mCallout.setLocation(geocode.getDisplayLocation());
//          mCallout.setContent(mCalloutContent);
//          mCallout.setShowOptions(new Callout.ShowOptions(true,false,false));
//          mCallout.getStyle().setLeaderPosition(Callout.Style.LeaderPosition.UPPER_MIDDLE);
//          mCallout.show();
//
//          mGraphicPoint = location;
//          mGraphicPointAddress = title + ", " + detail;
        }

      } catch (Exception e) {
        e.printStackTrace();
      }
    }

  }
}
