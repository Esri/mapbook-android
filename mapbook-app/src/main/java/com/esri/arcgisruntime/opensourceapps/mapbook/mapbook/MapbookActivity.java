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

package com.esri.arcgisruntime.opensourceapps.mapbook.mapbook;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.View;
import com.esri.arcgisruntime.opensourceapps.mapbook.ApplicationModule;
import com.esri.arcgisruntime.opensourceapps.mapbook.MapBookApplication;
import com.esri.arcgisruntime.opensourceapps.mapbook.R;
import com.esri.arcgisruntime.opensourceapps.mapbook.data.FileManager;
import com.esri.arcgisruntime.opensourceapps.mapbook.download.PortalItemUpdateService;
import com.esri.arcgisruntime.opensourceapps.mapbook.util.ActivityUtils;

import javax.inject.Inject;

/**
 * The MapbookActivity checks for READ EXTERNAL storage permissions, starts the background service
 * used to check if new versions of the mapbook exist, injects dependencies, and
 * adds the MapbookFragment.
 */
public class MapbookActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

  private static final int PERMISSION_TO_READ_EXTERNAL_STORAGE = 5;
  private View mLayout = null;
  private final String TAG = MapbookActivity.class.getSimpleName();
  private boolean permissionsChecked = false;
  private boolean permissionsGranted = false;
  private boolean initialized = false;

  @Inject  FileManager mFilemanager;
  @Inject MapbookPresenter mMapbookPresenter;

  /**
   * On creation of the activity, set up the action bar title
   * and check that the user has granted permissions for reading
   * external storage.
   * @param savedInstanceState - Bundle
   */
  @Override
  final protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_main);
    mLayout = findViewById(R.id.coordinator_layout);

    final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    if (toolbar != null){
      final ActionBar actionBar = (this).getSupportActionBar();
      if (actionBar != null){

        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME);
        actionBar.setTitle(R.string.title);
        actionBar.setCustomView(R.layout.logged_in_user);
      }
      toolbar.setNavigationIcon(null);
    }

  }

  @Override
  final public void onPostResume(){
    super.onPostResume();
    Log.i(TAG,"onPostResume");
    if (!permissionsChecked){
      // Can we read external storage?
      checkForReadStoragePermissions();
    }
    if (permissionsGranted && !initialized){
      initialize();
    }
  }
  /**
   * Create the mapbook fragment and load the presenter
   */
  private void initialize(){
    MapbookFragment mapbookFragment = (MapbookFragment) getSupportFragmentManager().findFragmentById(R.id.mapbookViewFragment);
    if (mapbookFragment == null){
      mapbookFragment = MapbookFragment.newInstance();
      ActivityUtils.addFragmentToActivity(getSupportFragmentManager(), mapbookFragment, R.id.mapbookViewFragment);
    }

    // Load presenter
    DaggerMapbookComponent.builder().applicationComponent(((MapBookApplication) getApplication())
        .getComponent()).applicationModule(new ApplicationModule(getApplicationContext())).mapbookModule(new MapbookModule(mapbookFragment)).build().inject(this);

    checkForUpdatedPortalItem();
    initialized = true;
  }

  /**
   * Once the app has prompted for permission to read external storage, the response
   * from the user is handled here.
   *
   * @param requestCode
   *            int: The request code passed into requestPermissions
   * @param permissions
   *            String: The requested permission(s).
   * @param grantResults
   *            int: The grant results for the permission(s). This will be
   *            either PERMISSION_GRANTED or PERMISSION_DENIED
   */
  @Override
  final public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
    if (requestCode == PERMISSION_TO_READ_EXTERNAL_STORAGE) {

      // Request for reading external storage
      if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        // Permission has been granted
        permissionsGranted = true;

      } else {
        // Permission request was denied.
        Snackbar.make(mLayout, "Permission to read external storage was denied.", Snackbar.LENGTH_SHORT).show();
      }
    }
  }

  /**
   * Determine if we're able to read external storage
   */
  private void checkForReadStoragePermissions(){
    // Explicitly check for file system privs
    final int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
    if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
      Log.i(TAG, "This application has proper permissions for reading external storage...");
      permissionsGranted = true;

    } else {
      Log.i(TAG, "This application DOES NOT have appropriate permissions for reading external storage");
      ActivityCompat.requestPermissions(this,
          new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
          PERMISSION_TO_READ_EXTERNAL_STORAGE);
    }
    // We've checked permissions and shouldn't re-check in the future
    permissionsChecked = true;
  }
  /**
   * Start an IntentService to check for any
   * updated versions of the mobile map package
   */
  private void checkForUpdatedPortalItem() {
    final Intent portalItemUpdateServiceIntent = new Intent(this, PortalItemUpdateService.class);
    this.startService(portalItemUpdateServiceIntent);
  }
}
