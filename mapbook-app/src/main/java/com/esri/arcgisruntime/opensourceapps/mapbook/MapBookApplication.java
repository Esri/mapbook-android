/* Copyright 2017 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * For additional information, contact:
 * Environmental Systems Research Institute, Inc.
 * Attn: Contracts Dept
 * 380 New York Street
 * Redlands, California, USA 92373
 *
 * email: contracts@esri.com
 *
 */

package com.esri.arcgisruntime.opensourceapps.mapbook;

import android.app.Application;

/**
 *
 * Even though Dagger2 allows annotating a {@link dagger.Component} as a singleton, the code itself
 * must ensure only one instance of the class is created. Therefore, we create a custom
 * {@link MapBookApplication} class to store a singleton reference to the {@link ApplicationComponent}.
 *
 * <P>
 * The application is made of 3 other Dagger components, as follows:<BR />
 * {@link com.esri.arcgisruntime.opensourceapps.mapbook.mapbook.MapbookComponent}: shows the mapbook metadata<BR />
 * {@link com.esri.arcgisruntime.opensourceapps.mapbook.download.DownloadComponent}: manages downloading mapbook from the Portal<BR />
 * {@link com.esri.arcgisruntime.opensourceapps.mapbook.map.MapComponent}: shows a specific map in the mapbook<BR />
 */

public class MapBookApplication extends Application {

  private ApplicationComponent component; // application works as a container for component.  Component is used for entire app.

  @Override
  public void onCreate(){
    super.onCreate();
    buildComponentAndInject();
    component = DaggerApplicationComponent.builder().applicationModule(new ApplicationModule(getApplicationContext())).build();

  }
  private void buildComponentAndInject(){
    component = DaggerComponentInitializer.init(this);
  }


  public ApplicationComponent getComponent(){
    return component;
  }

  private final static class DaggerComponentInitializer {
    public static ApplicationComponent init (final MapBookApplication app){
      return DaggerApplicationComponent.builder()
          .applicationModule(new ApplicationModule(app.getApplicationContext()))
          .build();
    }
  }
}
