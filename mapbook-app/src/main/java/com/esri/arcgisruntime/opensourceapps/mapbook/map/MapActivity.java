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

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.esri.arcgisruntime.opensourceapps.mapbook.ApplicationModule;
import com.esri.arcgisruntime.opensourceapps.mapbook.MapBookApplication;
import com.esri.arcgisruntime.opensourceapps.mapbook.R;
import com.esri.arcgisruntime.opensourceapps.mapbook.data.DataManager;
import com.esri.arcgisruntime.opensourceapps.mapbook.mapbook.MapbookFragment;
import com.esri.arcgisruntime.opensourceapps.mapbook.util.ActivityUtils;

import javax.inject.Inject;

/**
 *  The MapActivity processes the Intent, starts the MapFragment and injects dependencies.
 */
public class MapActivity extends AppCompatActivity {

  private final static String TAG = MapActivity.class.getSimpleName();

  @Inject DataManager mDataManager;
  @Inject MapPresenter mMapPresenter;

  /**
   * On creation of the activity, get the data
   * passed in the Intent to the activity and
   * set up the map fragment and inject map module dependencies.
   * @param savedInstanceState - Bundle
   */
  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.map_view);

    initialize();
  }

  private void initialize(){

    final Intent intent = getIntent();
    final String mmpkPath = intent.getStringExtra(MapbookFragment.FILE_PATH);
    final int index = intent.getIntExtra(getString(R.string.index),0);
    final String title = intent.getStringExtra(getString(R.string.map_title));

    MapFragment fragment = (MapFragment) getSupportFragmentManager().findFragmentById(R.id.mapLinearLayout);
    if (fragment == null){
      fragment = MapFragment.newInstance();
      final Bundle args = fragment.getArguments();
      args.putString(MapbookFragment.FILE_PATH, mmpkPath);
      args.putInt(getString(R.string.index), index);
      args.putString(getString(R.string.map_title), title);
      ActivityUtils.addFragmentToActivity(getSupportFragmentManager(), fragment, R.id.mapLinearLayout);
    }
    // Ask the component to inject this activity
    DaggerMapComponent.builder().applicationComponent(((MapBookApplication) getApplication())
        .getComponent()).applicationModule(new ApplicationModule(getApplicationContext())).mapModule(new MapModule(fragment)).build().inject(this);

  }
}
