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

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.*;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.layers.Layer;
import com.esri.arcgisruntime.mapping.LayerList;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DataManager {

  public DataManager(){

  }
  public void queryForFeatures(Geometry geometry, LayerList layers, DataManagerCallbacks.FeatureCallback callback){
    Iterator<Layer> iterator = layers.iterator();
    while (iterator.hasNext()){
      Layer layer = iterator.next();
      if (layer instanceof FeatureLayer){
        FeatureTable featureTable = ((FeatureLayer) layer).getFeatureTable();
        if (featureTable != null){
          final QueryParameters queryParameters = new QueryParameters();
          queryParameters.setGeometry(geometry);
          FeatureLayer featureLayer = (FeatureLayer) layer;
          final ListenableFuture<FeatureQueryResult> futureResult = featureTable.queryFeaturesAsync(queryParameters);
          processQueryForFeatures(futureResult,featureLayer,  callback);
        }
      }
    }
  }

  private void processQueryForFeatures(final ListenableFuture<FeatureQueryResult> futureResult, final FeatureLayer featureLayer, final DataManagerCallbacks.FeatureCallback callback ){
    futureResult.addDoneListener(new Runnable() {
      @Override public void run() {
        // call get on the future to get the result
        try {
          FeatureQueryResult result = futureResult.get();

          Iterator<Feature> iterator = result.iterator();
          List<Feature> featureList = new ArrayList<>();
          while (iterator.hasNext()){
            Feature feature = iterator.next();
            featureList.add(feature);
          }
          callback.onFeaturesFound(featureList, featureLayer);

        } catch (Exception e) {
          e.printStackTrace();
          callback.onNoFeaturesFound();
        }
      }
    });
  }
}

