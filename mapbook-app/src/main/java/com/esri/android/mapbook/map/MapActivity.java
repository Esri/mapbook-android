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

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import com.esri.android.mapbook.ApplicationModule;
import com.esri.android.mapbook.MapBookApplication;
import com.esri.android.mapbook.R;
import com.esri.android.mapbook.data.DataManager;
import com.esri.android.mapbook.mapbook.MapbookFragment;
import com.esri.android.mapbook.util.ActivityUtils;

import javax.inject.Inject;


public class MapActivity extends AppCompatActivity {

  private final static String TAG = MapActivity.class.getSimpleName();

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


//  /**
//   * Updates marker and callout when new results are loaded.
//   */
//  private class ResultsLoadedListener implements Runnable {
//
//    private final ListenableFuture<List<GeocodeResult>> results;
//
//    /**
//     * Constructs a runnable listener for the geocode results.
//     *
//     * @param results results from a {@link LocatorTask#geocodeAsync} task
//     */
//    ResultsLoadedListener(ListenableFuture<List<GeocodeResult>> results) {
//      this.results = results;
//    }
//
//
//    @Override
//    public void run() {
//
//      try {
//        List<GeocodeResult> geocodes = results.get();
//        if (geocodes.size() > 0) {
//          // get the top result
//          GeocodeResult geocode = geocodes.get(0);
//
//          // set the viewpoint to the marker
//          Point location = geocode.getDisplayLocation();
//          // get attributes from the result for the callout
//          String title;
//          String detail;
//          Object matchAddr = geocode.getAttributes().get("Match_addr");
//          if (matchAddr != null) {
//            // attributes from a query-based search
//            title = matchAddr.toString().split(",")[0];
//            detail = matchAddr.toString().substring(matchAddr.toString().indexOf(",") + 1);
//          } else {
//            // attributes from a click-based search
//            String street = geocode.getAttributes().get("Street").toString();
//            String city = geocode.getAttributes().get("City").toString();
//            String state = geocode.getAttributes().get("State").toString();
//            String zip = geocode.getAttributes().get("ZIP").toString();
//            title = street;
//            detail = city + ", " + state + " " + zip;
//          }
//
//          // get attributes from the result for the callout
//          HashMap<String, Object> attributes = new HashMap<>();
//          attributes.put("title", title);
//          attributes.put("detail", detail);
//
//
//          // create the marker
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
//        }
//
//      } catch (Exception e) {
//        e.printStackTrace();
//      }
//    }
//
//  }
}