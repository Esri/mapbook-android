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

package com.esri.android.mapbook.data;

import com.esri.arcgisruntime.geometry.Geometry;

/**
 * Methods implemented by objects responsible for interacting with
 * mobile map packages and their locators.
 */

public interface DataManagerContract {
  /**
   * Load mobile map given path and callback
   * @param mobileMapPackagePath - String for  absolute path to file
   * @param callback - MapbookCallback that executes on mobile map package load (or error)
   */
  void loadMobileMapPackage(String mobileMapPackagePath, DataManagerCallbacks.MapbookCallback callback);

  /**
   * Geocode given address and execute callback upon completion
   * @param address - String representing address
   * @param geocodingCallback - GeoCodingCallback execute on completion of geocoding.
   */
  void geocodeAddress(String address, DataManagerCallbacks.GeoCodingCallback geocodingCallback);

  /**
   * Return a boolean indicating whether the mapbook has a locator task
   * @return -boolean, true if mapbook has locator task. False otherwise.
   */
  boolean hasLocatorTask();

  /**
   * Retrieve suggestions from locator given search area and query.  Result are processed
   * using the provided SuggestionCallback.
   * @param searchArea - Geometry representing area to search
   * @param query - String representing query string
   * @param callback - The SuggestionCallback executed upon completion of locator suggestion lookup.
   */
  void getSuggestions(Geometry searchArea, String query, DataManagerCallbacks.SuggestionCallback callback);

}
