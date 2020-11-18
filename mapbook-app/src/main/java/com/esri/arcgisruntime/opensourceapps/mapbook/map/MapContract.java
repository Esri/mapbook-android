/*
 *  Copyright 2017 Esri
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * https://www.apache.org/licenses/LICENSE-2.0
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

package com.esri.arcgisruntime.opensourceapps.mapbook.map;

import com.esri.arcgisruntime.opensourceapps.mapbook.BasePresenter;
import com.esri.arcgisruntime.opensourceapps.mapbook.BaseView;
import com.esri.arcgisruntime.opensourceapps.mapbook.data.FeatureContent;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.view.IdentifyLayerResult;
import com.esri.arcgisruntime.tasks.geocode.SuggestResult;

import java.util.List;

/**
 * This is the contract between the Presenter and View components of the MVP pattern.
 * It defines methods and logic used when showing a map in the mapbook.
 */
public interface MapContract {

  interface Presenter extends BasePresenter {
    /**
     * Geocode given string address
     * @param address - String representing address
     */
    void geoCodeAddress(String address);

    /**
     * Get suggestions for area defined by geometry and given query
     * @param geometry - Geometry
     * @param query - String
     */
    void getSuggestions(Geometry geometry, String query);

    /**
     * Returns true if mobile map package has task, false if does not.
     * @return boolean
     */
    boolean hasLocatorTask();

    /**
     * Load a specific map from a mobile map package stored
     * at the given path.
     * @param path - String representing location on device where mobile map package exists.
     * @param mapIndex - int representing index of the map to be loaded.
     */
    void loadMap(String path, int mapIndex);

    /**
     * Returns list of featured content given the results of an identify operation.

     * @param results - List of IdentifyLayerResults
     * @return List of FeatureContent
     */
    List<FeatureContent> identifyFeatures(List<IdentifyLayerResult> results);

  }
  interface View extends BaseView<Presenter>{

    /**
     * Display given map in the map view
     * @param map - ArcGIS map
     */
    void showMap(ArcGISMap map);

    /**
     * Display content for given location.
     * @param resultpoint - Point
     * @param calloutContent - View
     * @param zoomOut - boolean, true to zoom out
     */
    void displaySearchResult(Point resultpoint, android.view.View calloutContent, boolean zoomOut);

    /**
     * Initialize map-related objects
     */
    void initializeMapItems();

    /**
     * Show suggestions
     * @param suggestResultList List<SuggestResults></SuggestResults>
     */
    void showSuggestedPlaceNames(List<SuggestResult> suggestResultList);

    /**
     * Show a message
     * @param message - String
     */
    void showMessage(String message);

    /**
     * Get suggestions for the given area and query
     * @param geometry - Geometry
     * @param query - String
     */
    void getSuggestions(Geometry geometry, String query);
  }
}
