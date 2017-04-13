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

import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.layers.FeatureLayer;

import java.util.ArrayList;
import java.util.List;

/**
 * A POJO defining data in a Feature
 */
public class FeatureContent {
  private final String mLayerName;
  private final FeatureLayer mFeatureLayer;
  private List<Entry> entries = null;
  private Feature mFeature = null;

  public FeatureContent(final FeatureLayer featureLayer){
    mFeatureLayer = featureLayer;
    mLayerName = mFeatureLayer.getName();
    setEntries(new ArrayList<Entry>());
  }

  public String getLayerName() {
    return mLayerName;
  }

  public void setFeature(final Feature feature){
    mFeature = feature;
  }
  public Feature getFeature(){
    return mFeature;
  }
  public FeatureLayer getFeatureLayer(){
    return mFeatureLayer;
  }
  public List<Entry> getEntries() {
    return entries;
  }


  public void setEntries(final List<Entry> entries) {
    this.entries = entries;
  }
}
