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

package com.esri.arcgisruntime.opensourceapps.mapbook.data;

import androidx.annotation.NonNull;
import android.util.Log;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.MobileMapPackage;
import com.esri.arcgisruntime.tasks.geocode.GeocodeParameters;
import com.esri.arcgisruntime.tasks.geocode.SuggestParameters;
import com.esri.arcgisruntime.tasks.geocode.LocatorTask;
import com.esri.arcgisruntime.tasks.geocode.GeocodeResult;
import com.esri.arcgisruntime.tasks.geocode.SuggestResult;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class DataManager implements DataManagerContract {

  private MobileMapPackage mMobileMapPackage = null;
  private GeocodeParameters mGeocodeParameters = null;

  private LocatorTask mLocatorTask = null;

  final private static String TAG = DataManager.class.getSimpleName();

  public DataManager(){
    mGeocodeParameters = new GeocodeParameters();
    mGeocodeParameters.getResultAttributeNames().add("*");
    mGeocodeParameters.setMaxResults(1);
  }


  @Override public void loadMobileMapPackage(@NonNull final String mobileMapPackagePath,
      final DataManagerCallbacks.MapbookCallback callback) {
    mMobileMapPackage = new MobileMapPackage(mobileMapPackagePath);
    mMobileMapPackage.addDoneLoadingListener(new Runnable() {
      @Override public void run() {
        if (mMobileMapPackage.getLoadStatus() == LoadStatus.LOADED){
          callback.onMapbookLoaded(mMobileMapPackage);
        }else{
          callback.onMapbookNotLoaded(mMobileMapPackage.getLoadError());
        }
      }
    });
    mMobileMapPackage.loadAsync();
  }

  /**
   *
   * Geocode an address typed in by user
   *
   * @param address - String
   * @param geocodingCallback GeoCodingCallback
   */
  @Override
  public void geocodeAddress(final String address,
      final DataManagerCallbacks.GeoCodingCallback geocodingCallback){
    if (hasLocatorTask()){
      if (mLocatorTask == null){
        mLocatorTask = mMobileMapPackage.getLocatorTask();
        // If locator task is still null, then the mobile map package doesn't have a locator
        if (mLocatorTask == null){
          geocodingCallback.onNoGeoCodingTask("No locator task found in mobile map package");
          return;
        }
      }
      mLocatorTask.addDoneLoadingListener(new Runnable() {
        @Override public void run() {
          if (mLocatorTask.getLoadStatus() == LoadStatus.LOADED){
            // Call geocodeAsync passing in an address
            final ListenableFuture<List<GeocodeResult>> geocodeFuture = mLocatorTask.geocodeAsync(address,
                mGeocodeParameters);
            geocodeFuture.addDoneListener(new Runnable() {
              @Override public void run() {

                try {
                  final List<GeocodeResult> geocodeResults = geocodeFuture.get();
                  geocodingCallback.onGeoCodingTaskCompleted(geocodeResults);

                } catch (InterruptedException | ExecutionException e) {

                  Log.e(TAG, e.getMessage());
                  geocodingCallback.onGeoCodingError(e.getCause());
                }
              }
            });
          }else{
            geocodingCallback.onGeoCodingTaskNotLoaded(mLocatorTask.getLoadError());
          }
        }
      });
      mLocatorTask.loadAsync();
    }else{
      geocodingCallback.onNoGeoCodingTask("No locator task available");
    }
  }


  @Override
  public boolean hasLocatorTask(){
    return mMobileMapPackage.getLocatorTask() != null;
  }



  @Override public void getSuggestions(final Geometry searchArea, final String query, final DataManagerCallbacks.SuggestionCallback callback) {

    if (mLocatorTask == null) {
      mLocatorTask = mMobileMapPackage.getLocatorTask();
      // If locator task is still null, then the mobile map package doesn't have a locator
      if (mLocatorTask == null){
        callback.noSuggestionSupport();
        return;
      }
    }
    mLocatorTask.addDoneLoadingListener(new Runnable() {
      @Override public void run() {
        if (mLocatorTask.getLoadStatus() == LoadStatus.LOADED) {
          if (mLocatorTask.getLocatorInfo().isSupportsSuggestions()) {
            final SuggestParameters suggestParameters = new SuggestParameters();
            suggestParameters.setMaxResults(5);
            suggestParameters.setSearchArea(searchArea);

            final ListenableFuture<List<SuggestResult>> suggestionsFuture = mLocatorTask
                .suggestAsync(query, suggestParameters);
            suggestionsFuture.addDoneListener(new Runnable() {
              @Override public void run() {
                try {
                  final List<SuggestResult> results = suggestionsFuture.get();
                  callback.onSuggestionsComplete(results);
                } catch (InterruptedException | ExecutionException e) {
                  Log.e(TAG, "InterruptedException " + e.getMessage());
                  callback.onSuggestionFailure(e.getCause());
                }
              }
            });
          } else {
            callback.noSuggestionSupport();

          }
        }
      }

    });
    mLocatorTask.loadAsync();
  }


}

