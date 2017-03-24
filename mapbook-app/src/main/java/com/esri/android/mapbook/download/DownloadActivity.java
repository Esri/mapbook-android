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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import com.esri.android.mapbook.ApplicationModule;
import com.esri.android.mapbook.MapBookApplication;
import com.esri.android.mapbook.mapbook.MapbookFragment;
import com.esri.android.mapbook.util.ActivityUtils;
import com.esri.arcgisruntime.portal.Portal;
import com.esri.arcgisruntime.security.AuthenticationManager;
import com.esri.arcgisruntime.security.DefaultAuthenticationChallengeHandler;
import com.esri.arcgisruntime.security.OAuthConfiguration;

import javax.inject.Inject;

public class DownloadActivity extends AppCompatActivity {

  @Inject DefaultAuthenticationChallengeHandler defaultAuthenticationChallengeHandler;
  @Inject OAuthConfiguration oAuthConfiguration;
  @Inject DownloadPresenter mPresenter;

  Portal mPortal = null;
  long mPortalItemSize;
  final Activity activity = this;
  public static final String ERROR_STRING = "error string";
  private final String TAG = DownloadActivity.class.getSimpleName();
 // private  NetworkReceiver receiver = new NetworkReceiver();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    initialize();

    // Registers BroadcastReceiver to track network connection changes.
    //registerReceiver();
  }

  /**
   * Registers BroadcastReceiver to track network connection changes
   */
  private void registerReceiver(){
//    IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
//    receiver = new NetworkReceiver();
//    registerReceiver(receiver, filter);
  }
  /**
   * Configure AuthenticationManager for portal access
   */
  private void initialize(){

    DownloadFragment fragment = (DownloadFragment) getSupportFragmentManager().findFragmentByTag("downloadFragment");
    if (fragment == null){
      fragment = DownloadFragment.newInstance();
      String fileName =  getIntent().getStringExtra(MapbookFragment.FILE_PATH);
      Bundle args = fragment.getArguments();
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

  @Override
  public void onDestroy() {
    super.onDestroy();
    // Unregisters BroadcastReceiver when activity is destroyed.
//    if (receiver != null) {
//      this.unregisterReceiver(receiver);
//    }
  }

//  public class NetworkReceiver extends BroadcastReceiver {
//
//    @Override public void onReceive(Context context, Intent intent) {
//      ConnectivityManager conn =  (ConnectivityManager)
//          context.getSystemService(Context.CONNECTIVITY_SERVICE);
//      NetworkInfo networkInfo = conn.getActiveNetworkInfo();
//
//      // Just log behavior for now (do we really need a BroadcastReceiver?
//      Log.i(TAG, "Network info received...");
//    }
//  }
}
