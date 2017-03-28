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

import android.util.Log;
import android.widget.Toast;
import com.esri.android.mapbook.R;
import com.esri.android.mapbook.data.DataManager;
import com.esri.android.mapbook.data.DataManagerCallbacks;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.LayerList;
import com.esri.arcgisruntime.mapping.MobileMapPackage;
import com.esri.arcgisruntime.tasks.geocode.GeocodeResult;
import com.esri.arcgisruntime.tasks.geocode.SuggestResult;

import javax.inject.Inject;
import java.util.List;

public class MapPresenter implements MapContract.Presenter {

  private final DataManager mDataManager;

  private final MapContract.View mView;

  private final String TAG = MapPresenter.class.getSimpleName();

  @Inject
  MapPresenter (final DataManager manager, final MapContract.View view){
    mDataManager = manager;
    mView = view;
  }

  /**
   * Method injection is used here to safely reference {@code this} after the object is created.
   * For more information, see Java Concurrency in Practice.
   */
  @Inject
  final void setupListeners() {
    mView.setPresenter(this);
  }


  @Override public void start() {
    mView.setUpMap();
  }

  @Override public void geoCodeAddress(final String address) {
    mDataManager.geocodeAddress(address, new DataManagerCallbacks.GeocodingCallback() {
      @Override public void onGeocodingTaskLoaded(List<GeocodeResult> geocodeResults) {
        if (geocodeResults.size() > 0) {
          // Use the first result - for example
          // display on the map
          GeocodeResult result = geocodeResults.get(0);
          Point displayLocation = result.getDisplayLocation();

          mView.displaySearchResult(displayLocation, result.getLabel(), true);

        } else {
          mView.showMessage( "Location not found for " + address);

        }
      }

      @Override public void onGeocodingTaskNotLoaded(Throwable error) {
        mView.showMessage("An error was encountered when trying to search for the address " + error.getMessage());
      }

      @Override public void onNoGeocodingTask(String message) {
        mView.showMessage(message);
      }

      @Override public void onGeocodingError(Throwable error) {

      }
    });
  }

  @Override public void getSuggestions(Geometry geometry, String query) {
      mDataManager.getSuggestions(geometry, query, new DataManagerCallbacks.SuggestionCallback() {
        @Override public void onSuggestionsComplete(List<SuggestResult> suggestResults) {
          if (suggestResults.size() > 0){
            mView.showSuggestedPlaceNames(suggestResults);
          }else{
            Log.i(TAG, "No suggestions returned");
          }
        }

        @Override public void onSuggetionFailure(Throwable error) {
          Log.i(TAG, "Suggestion error " +  error.getMessage());
        }

        @Override public void noSuggestionSupport(){
          Log.i(TAG,"No suggestion support");
        }
      });
  }

  @Override public boolean hasLocatorTask() {
    return mDataManager.hasLocatorTask();
  }

  @Override public void loadMap(String path, final int mapIndex) {
    mDataManager.loadMobileMapPackage(path, new DataManagerCallbacks.MapbookCallback() {
      @Override public void onMapbookLoaded(MobileMapPackage mobileMapPackage) {
        List<ArcGISMap> maps = mobileMapPackage.getMaps();
        ArcGISMap map = maps.get(mapIndex);
        mView.showMap(map);
      }

      @Override public void onMapbookNotLoaded(Throwable error) {

      }
    });
  }

  @Override public void queryForFeatures(Geometry geometry, LayerList layers) {
    mDataManager.queryForFeatures(geometry, layers, new DataManagerCallbacks.FeatureCallback() {
      @Override public void onFeaturesFound(List<Feature> featureList, FeatureLayer layer) {

      }

      @Override public void onNoFeaturesFound() {
        mView.showMessage("No features found");
      }
    });
  }
}
