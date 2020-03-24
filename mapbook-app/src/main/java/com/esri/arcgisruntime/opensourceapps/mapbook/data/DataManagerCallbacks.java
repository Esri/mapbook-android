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

package com.esri.arcgisruntime.opensourceapps.mapbook.data;

import com.esri.arcgisruntime.mapping.MobileMapPackage;
import com.esri.arcgisruntime.tasks.geocode.GeocodeResult;
import com.esri.arcgisruntime.tasks.geocode.SuggestResult;

import java.util.List;

/**
 * Represents the different types of callback functions executed
 * upon asynchronous completion of tasks.
 */
public interface DataManagerCallbacks {

  /**
   * Handles async calls for loading map book
   */
  interface MapbookCallback{
    void onMapbookLoaded(MobileMapPackage mobileMapPackage);
    void onMapbookNotLoaded(Throwable error);
  }

  /**
   * Handle async calls for geo coding
   */
  interface GeoCodingCallback {
    void onGeoCodingTaskCompleted(List<GeocodeResult> results);
    void onGeoCodingTaskNotLoaded(Throwable error);
    void onNoGeoCodingTask(String message);
    void onGeoCodingError(Throwable error);
  }

  /**
   * Callbacks for async suggestion methods
   */
  interface SuggestionCallback{
    void onSuggestionsComplete(List<SuggestResult> suggestResults);
    void onSuggestionFailure(Throwable error);
    void noSuggestionSupport();
  }


}
