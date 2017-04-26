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

package com.esri.android.mapbook.mapbook;

import android.util.Log;
import com.esri.android.mapbook.data.DataManagerCallbacks;
import com.esri.android.mapbook.data.FileManager;
import com.esri.android.mapbook.download.CredentialCryptographer;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Item;
import com.esri.arcgisruntime.mapping.MobileMapPackage;
import com.esri.arcgisruntime.security.AuthenticationManager;
import com.esri.arcgisruntime.security.Credential;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;
import java.util.List;

/**
 * This is the concrete implementation of the Presenter defined in the MapbookContract.
 * It encapsulates business logic and drives the behavior of the View.
 */

public class MapbookPresenter implements MapbookContract.Presenter {

  private final FileManager mFileManager;

  private final MapbookContract.View mView;

  private String mPath = null;

  private final String TAG = MapbookPresenter.class.getSimpleName();

  @Inject CredentialCryptographer mCredentialCryptopgrapher;

  @Inject
  MapbookPresenter (final FileManager manager, final MapbookContract.View view) {
    mFileManager = manager;
    mView = view;
  }

  /**
   * Method injection is used here to safely reference {@code this} after the object is created.
   * For more information, see Java Concurrency in Practice.
   */
  @Inject
  final void setupListeners() {
    mView.setPresenter(this);
  }

  @Override final public void start() {
    checkForMapbook();
    checkForUserName();
  }

  /**
   * If mapbook exists on the device, populate the UI
   * otherwise try to download it from the portal.
   */
  @Override final public void checkForMapbook() {

    mPath = mFileManager.fileExists();
    if (mPath != null){

      loadMapbook(new DataManagerCallbacks.MapbookCallback() {
        /**
         * If successfully loaded, populate view
         * @param mobileMapPackage - MobileMapPackage
         */
        @Override final public void onMapbookLoaded(final MobileMapPackage mobileMapPackage) {
          final List<ArcGISMap> maps = mobileMapPackage.getMaps();
          mView.setMaps(maps);
          final Item item = mobileMapPackage.getItem();
          mView.populateMapbookLayout(item);

          // Get the file size and date
          final long mapbookSize = mFileManager.getSize();
          final long mapbookModified = mFileManager.getModifiedDate();

          mView.setMapbookMetatdata(mapbookSize, mapbookModified, maps.size());

          final byte[] thumbnailData = item.getThumbnailData();
          if (thumbnailData != null && thumbnailData.length > 0) {
            mView.setThumbnailBitmap(thumbnailData);
          }else{
            final ListenableFuture<byte[]> futureThumbnail = item.fetchThumbnailAsync();
            futureThumbnail.addDoneListener(new Runnable() {
              @Override public void run() {
                try {
                  final byte[] itemThumbnailData = futureThumbnail.get();
                  mView.setThumbnailBitmap(itemThumbnailData);
                } catch (final Exception e) {
                  Log.e(TAG,e.getMessage());
                  mView.showMessage("There were problems obtaining thumbnail images for maps in mapbook.");
                }
              }
            });
          }
        }

        /**
         * If the mapbook fails to load, show a message
         * @param error - Throwable
         */
        @Override final public void onMapbookNotLoaded(final Throwable error) {
          Log.e(TAG, "Problem loading map book " + error.getMessage());
          mView.showMapbookNotFound();
          mView.showMessage("There was a problem loading the mapbook");
        }
      });


    }else{
      // Mapbook file isn't found, try downloading it...
      mView.downloadMapbook(mFileManager.createMobileMapPackageFilePath());
    }
  }

  /**
   * Load the mobile map package
   * @param callback- MapbookCallback to handle async response.
   */
  @Override final public void loadMapbook(final DataManagerCallbacks.MapbookCallback callback) {
    final String mmpkPath = mFileManager.createMobileMapPackageFilePath();

    final MobileMapPackage mmp = new MobileMapPackage(mmpkPath);

    mmp.addDoneLoadingListener(new Runnable() {
      @Override final public void run() {
        if (mmp.getLoadStatus() == LoadStatus.LOADED) {
          callback.onMapbookLoaded(mmp);

        }else{
          callback.onMapbookNotLoaded(mmp.getLoadError());
        }
      }
    });
    mmp.loadAsync();
  }

  @Override public String getMapbookPath() {
    return mPath;
  }

  /**
   * Process PortalItemUpdateService Broadcast.  If
   * there's a newer version of the mobile map package
   * on the server, then enable the download button.
   * @param modifiedMillis long - The milliseconds representing modified date of PortalItem
   */
  @Override public void processBroadcast(long modifiedMillis) {
    long mapbookModMillis = mFileManager.getModifiedDate();
    Log.i(TAG, "Mapbook modified milliseconds "+ mapbookModMillis);

    if (modifiedMillis > mapbookModMillis){
      mView.toggleDownloadVisibility(true);
    }else{
      mView.toggleDownloadVisibility(false);
    }
  }

  /**
   * Logout user by deleting cred_file and mobile map package
   */
  @Override public void logout() {
    // Clear credential cache
    AuthenticationManager.CredentialCache.clear();

    // Delete cred file
    boolean deletedCred =  mCredentialCryptopgrapher.deleteCredentialFile();
    Log.i(TAG, "Credentials deleted "+ deletedCred);
    // Delete mmpk
    boolean deletedMmpk = mCredentialCryptopgrapher.deleteMobileMapPackage(mPath);
    Log.i(TAG, "MMPK deleted "+ deletedMmpk);
    // Unset user name in action bar
    mView.setUserName("");

    // Exit app
    mView.exit();
  }

  /**
   * Check for user credentials on the device using the
   * stored credential file.
   */
  @Override public String getUserName() {
    return mCredentialCryptopgrapher.getUserName();
  }
  /**
   * Get the user name if known
   */
  private void checkForUserName(){
    if (getUserName()== null){
      mCredentialCryptopgrapher.setUserNameFromCredentials(null);
    }
    mView.setUserName(getUserName());

  }
}
