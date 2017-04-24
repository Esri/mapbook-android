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

package com.esri.android.mapbook.download;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import com.esri.android.mapbook.ApplicationModule;
import com.esri.android.mapbook.MapBookApplication;
import com.esri.android.mapbook.mapbook.MapbookFragment;
import com.esri.android.mapbook.util.ActivityUtils;
import com.esri.arcgisruntime.security.AuthenticationManager;
import com.esri.arcgisruntime.security.DefaultAuthenticationChallengeHandler;
import com.esri.arcgisruntime.security.OAuthConfiguration;

import javax.inject.Inject;

/**
 * The DownloadActivity processes the fragment arguments, starts the DownloadFragment, injects dependencies,
 * and finishes setup for the AuthenticationManager.
 */
public class DownloadActivity extends AppCompatActivity {

  @Inject DefaultAuthenticationChallengeHandler defaultAuthenticationChallengeHandler;
  @Inject OAuthConfiguration oAuthConfiguration;
  @Inject DownloadPresenter mPresenter;


  public static final String ERROR_STRING = "error string";
  private final String TAG = DownloadActivity.class.getSimpleName();

  @Override
  final protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    initialize();
  }

  /**
   * Configure AuthenticationManager for portal access
   */
  final private void initialize(){

    DownloadFragment fragment = (DownloadFragment) getSupportFragmentManager().findFragmentByTag("downloadFragment");
    if (fragment == null){
      fragment = DownloadFragment.newInstance();
      final String fileName =  getIntent().getStringExtra(MapbookFragment.FILE_PATH);
      final Bundle args = fragment.getArguments();
      args.putString(MapbookFragment.FILE_PATH, fileName);
      ActivityUtils.addFragmentToActivity(getSupportFragmentManager(), fragment, "downloadFragment");
    }
    Log.i(TAG, "Initializing Dagger component...");
    DaggerDownloadComponent.builder().applicationComponent(((MapBookApplication) getApplication())
        .getComponent())
        .applicationModule(new ApplicationModule(getApplicationContext()))
        .downloadModule(new DownloadModule(fragment, this))
        .build()
        .inject(this);
    AuthenticationManager.setAuthenticationChallengeHandler(defaultAuthenticationChallengeHandler);
    AuthenticationManager.addOAuthConfiguration(oAuthConfiguration);
  }
}
