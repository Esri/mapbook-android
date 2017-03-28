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

import android.graphics.Point;
import android.support.annotation.NonNull;
import android.util.Log;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.*;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.layers.Layer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.LayerList;
import com.esri.arcgisruntime.mapping.MobileMapPackage;
import com.esri.arcgisruntime.tasks.geocode.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;

public final class DataManager implements DataManagerContract {

  private MobileMapPackage mMobileMapPackage = null;
  private GeocodeParameters mGeocodeParameters = null;
  private ReverseGeocodeParameters mReverseGeocodeParameters = null;
  private LocatorTask mLocatorTask;

  final private static String TAG = DataManager.class.getSimpleName();

  public DataManager(){
    mGeocodeParameters = new GeocodeParameters();
    mGeocodeParameters.getResultAttributeNames().add("*");
    mGeocodeParameters.setMaxResults(1);
  }



  @Override
  public void queryForFeatures(final Geometry geometry, final LayerList layers,
      final DataManagerCallbacks.FeatureCallback callback){

    final Iterator<Layer> iterator = layers.iterator();
    while (iterator.hasNext()){
      final Layer layer = iterator.next();
      if (layer instanceof FeatureLayer){
        final FeatureTable featureTable = ((FeatureLayer) layer).getFeatureTable();
        if (featureTable != null){
          final QueryParameters queryParameters = new QueryParameters();
          queryParameters.setGeometry(geometry);
          final FeatureLayer featureLayer = (FeatureLayer) layer;
          final ListenableFuture<FeatureQueryResult> futureResult = featureTable.queryFeaturesAsync(queryParameters);
          processQueryForFeatures(futureResult,featureLayer,  callback);
        }
      }
    }
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
   * @param futureResult
   * @param featureLayer
   * @param callback
   */
  final private static void processQueryForFeatures(final ListenableFuture<FeatureQueryResult> futureResult,
      final FeatureLayer featureLayer, final DataManagerCallbacks.FeatureCallback callback ){

    futureResult.addDoneListener(new Runnable() {
      @Override public void run() {
        // call get on the future to get the result
        try {
          final FeatureQueryResult result = futureResult.get();

          final Iterator<Feature> iterator = result.iterator();
          final List<Feature> featureList = new ArrayList<>();
          while (iterator.hasNext()){
            final Feature feature = iterator.next();
            featureList.add(feature);
          }
          callback.onFeaturesFound(featureList, featureLayer);

        } catch (Exception e) {
          Log.e(TAG,e.getMessage());
          callback.onNoFeaturesFound();
        }
      }
    });
  }

  /**
   *
   * Geocode an address typed in by user
   *
   * @param address
   * @param geocodingCallback
   */
  @Override
  public void geocodeAddress(final String address,
      final DataManagerCallbacks.GeocodingCallback geocodingCallback){
    if (hasLocatorTask()){
      if (mLocatorTask == null){
        mLocatorTask = mMobileMapPackage.getLocatorTask();
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
                  List<GeocodeResult> geocodeResults = geocodeFuture.get();
                } catch (InterruptedException e) {
                  Log.e(TAG, "InterruptedException " + e.getMessage());
                  geocodingCallback.onGeocodingError(e.getCause());
                } catch (ExecutionException e) {
                  Log.e(TAG, "ExecutionException " + e.getMessage() );
                  geocodingCallback.onGeocodingError(e.getCause());
                }
              }
            });
          }else{
            geocodingCallback.onGeocodingTaskNotLoaded(mLocatorTask.getLoadError());
          }
        }
      });
      mLocatorTask.loadAsync();
    }else{
      geocodingCallback.onNoGeocodingTask("No locator task available");
    }
  }

  @Override
  public boolean hasLocatorTask(){
    return mMobileMapPackage.getLocatorTask() != null;
  }



  @Override public void getSuggestions(final Geometry searchArea, final String query, final DataManagerCallbacks.SuggestionCallback callback) {

    if (mLocatorTask == null) {
      mLocatorTask = mMobileMapPackage.getLocatorTask();
    }
    mLocatorTask.addDoneLoadingListener(new Runnable() {
      @Override public void run() {
        if (mLocatorTask.getLoadStatus() == LoadStatus.LOADED) {
          if (mLocatorTask.getLocatorInfo().isSupportsSuggestions()) {
            SuggestParameters suggestParameters = new SuggestParameters();
            suggestParameters.setMaxResults(5);
            suggestParameters.setSearchArea(searchArea);

            final ListenableFuture<List<SuggestResult>> suggestionsFuture = mLocatorTask
                .suggestAsync(query, suggestParameters);
            suggestionsFuture.addDoneListener(new Runnable() {
              @Override public void run() {
                try {
                  List<SuggestResult> results = suggestionsFuture.get();
                  callback.onSuggestionsComplete(results);
                } catch (InterruptedException e) {
                  Log.e(TAG, "InterruptedException " + e.getMessage());
                  callback.onSuggetionFailure(e.getCause());
                } catch (ExecutionException e) {
                  Log.e(TAG, "ExecutionException " + e.getMessage());
                  callback.onSuggetionFailure(e.getCause());
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

