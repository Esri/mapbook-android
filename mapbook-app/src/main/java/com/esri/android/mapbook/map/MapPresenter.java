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
import com.esri.android.mapbook.data.DataManager;
import com.esri.android.mapbook.data.DataManagerCallbacks;
import com.esri.android.mapbook.data.Entry;
import com.esri.android.mapbook.data.FeatureContent;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.GeoElement;
import com.esri.arcgisruntime.mapping.MobileMapPackage;
import com.esri.arcgisruntime.mapping.popup.Popup;
import com.esri.arcgisruntime.mapping.view.IdentifyLayerResult;
import com.esri.arcgisruntime.tasks.geocode.GeocodeResult;
import com.esri.arcgisruntime.tasks.geocode.SuggestResult;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MapPresenter implements MapContract.Presenter {

  private final DataManager mDataManager;

  private final MapContract.View mView;

  @Inject
  ContentExtractor popupInteractor;

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

  /**
   * Entry point for this class starts by initializng map related items.
   */
  @Override public void start() {
    mView.initializeMapItems();
  }

  /**
   * Geocode given string address
   * @param address - String representing address
   */
  @Override public void geoCodeAddress(final String address) {
    mDataManager.geocodeAddress(address, new DataManagerCallbacks.GeoCodingCallback() {
      @Override public void onGeoCodingTaskCompleted(final List<GeocodeResult> geocodeResults) {
        if (!geocodeResults.isEmpty()) {
          // Use the first result - for example
          // display on the map
          final GeocodeResult result = geocodeResults.get(0);
          final Point displayLocation = result.getDisplayLocation();

          mView.displaySearchResult(displayLocation, null, true);

        } else {
          mView.showMessage( "Location not found for " + address);

        }
      }

      @Override public void onGeoCodingTaskNotLoaded(final Throwable error) {
        mView.showMessage("An error was encountered when trying to search for the address " + error.getMessage());
      }

      @Override public void onNoGeoCodingTask(final String message) {
        mView.showMessage(message);
      }

      @Override public void onGeoCodingError(final Throwable error) {

      }
    });
  }

  /**
   * Get suggestions for area defined by geometry and given query
   * @param geometry - Geometry
   * @param query - String
   */
  @Override public void getSuggestions(final Geometry geometry, final String query) {
      mDataManager.getSuggestions(geometry, query, new DataManagerCallbacks.SuggestionCallback() {
        @Override public void onSuggestionsComplete(final List<SuggestResult> suggestResults) {
          if (!suggestResults.isEmpty()){
            mView.showSuggestedPlaceNames(suggestResults);
          }else{
            Log.i(TAG, "No suggestions returned");
          }
        }

        @Override public void onSuggestionFailure(final Throwable error) {
          Log.i(TAG, "Suggestion error " +  error.getMessage());
        }

        @Override public void noSuggestionSupport(){
          Log.i(TAG,"No suggestion support");
        }
      });
  }

  /**
   * Returns true if mobile map package has task, false if does not.
   * @return boolean
   */
  @Override public boolean hasLocatorTask() {
    return mDataManager.hasLocatorTask();
  }

  /**
   * Load a specific map from a mobile map package stored
   * at the given path.
   * @param path - String representing location on device where mobile map package exists.
   * @param mapIndex - int representing index of the map to be loaded.
   */
  @Override public void loadMap(final String path, final int mapIndex) {
    mDataManager.loadMobileMapPackage(path, new DataManagerCallbacks.MapbookCallback() {
      @Override public void onMapbookLoaded(final MobileMapPackage mobileMapPackage) {
        final List<ArcGISMap> maps = mobileMapPackage.getMaps();
        final ArcGISMap map = maps.get(mapIndex);
        mView.showMap(map);
      }

      @Override public void onMapbookNotLoaded(final Throwable error) {
        mView.showMessage("There was a problem loading the map");
      }
    });
  }

  /**
   * Returns list of featured content given the results of an identify operation.

   * @param results - List of IdentifyLayerResults
   * @return List of FeatureContent
   */
  @Override public List<FeatureContent> identifyFeatures( final List<IdentifyLayerResult> results) {
    final List<FeatureContent> content = new ArrayList<>();

    for (final IdentifyLayerResult result : results){

      // a reference to the feature layer can be used, for example, to select identified features
      FeatureLayer featureLayer = null;

      // We only care about FeatureLayer results
      if (result.getLayerContent() instanceof FeatureLayer) {
        featureLayer = (FeatureLayer) result.getLayerContent();
        final FeatureContent featureContent = new FeatureContent(featureLayer);
        featureLayer.setSelectionWidth(3.0d);

        final List<Popup> popups = result.getPopups();

        if (popups.size() > 0) {
          for (final Popup popup : popups){

            final List<Entry> entries = popupInteractor.getPopupFields(popup);
            featureContent.setEntries(entries);
            // Select feature
            final GeoElement element = popup.getGeoElement();
            if (element instanceof Feature){
              final Feature ft = (Feature) element;
              if (featureLayer != null){
                featureContent.setFeature(ft); // Assuming 1 popup per IdentifyLayerResult!!!
              }
            }
          }
        }else{ // No popups available, so get content from first GeoElement
          if (result.getElements().size() > 0){
            GeoElement geoElement = result.getElements().get(0);
            if (geoElement instanceof Feature){
              final Feature feature = (Feature) geoElement;
              featureContent.setFeature(feature);
              List<Entry> entries = popupInteractor.getEntriesFromGeoElement(geoElement);
              featureContent.setEntries(entries);
            }
          }
        }

        content.add(featureContent);
      }
    }
    return content;
  }


}
