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

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.util.Log;
import com.esri.android.mapbook.Constants;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.portal.Portal;
import com.esri.arcgisruntime.portal.PortalItem;
import com.esri.arcgisruntime.security.AuthenticationManager;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;

import static android.app.Activity.RESULT_CANCELED;
import static com.esri.android.mapbook.download.DownloadActivity.ERROR_STRING;

public class DownloadPresenter implements DownloadContract.Presenter {

  private final String TAG = DownloadPresenter.class.getSimpleName();
  @Inject ConnectivityManager mConnectivityManager;
  @Inject Portal mPortal;
  @Inject @Named("mPortalItemId") String mPortalItemId;
  @Inject CredentialCryptographer mCredentialManager;

  private final DownloadContract.View mView;
  private boolean mSignInStarted = false;

  @Inject
  DownloadPresenter (final DownloadContract.View view){
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

  /**
   * The entry point for this class begins by checking
   * if the device has any network connectivity.
   */
  @Override final public void start() {
    Log.i(TAG, "Starting presenter and checking for internet connectivity");
    final boolean isConnected = checkForInternetConnectivity();
    if (isConnected ){
      // If we've returned to the fragment after signing in, don't sign in again
      if (!mSignInStarted){
        signIn();
      }

    }else{
      // Prompt user about wireless connectivity
      mView.promptForInternetConnectivity();
    }

  }

  /**
   * Fetches mapbook from Portal
   */
  @Override final public void downloadMapbook() {

    Log.i(TAG, "Downloading mapbook");
    final PortalItem portalItem = new PortalItem(mPortal, mPortalItemId);
    portalItem.loadAsync();
    portalItem.addDoneLoadingListener(new Runnable() {
      @Override public void run() {

        final ListenableFuture<InputStream> future = portalItem.fetchDataAsync();
        final long portalItemSize = portalItem.getSize();
        future.addDoneListener(new Runnable() {
          @Override public void run() {

            try {
              final InputStream inputStream = future.get();
              mView.executeDownload(portalItemSize, inputStream);


            } catch (final InterruptedException | ExecutionException e) {
              mView.showMessage("There was a problem downloading the file");
              Log.e(TAG, "Problem downloading file " + e.getMessage());
              mView.sendResult(RESULT_CANCELED, ERROR_STRING,  e.getMessage());
            }
          }
        });
      }
    });
  }

  /**
   * Initiates the authentication process against the Portal
   */
  @Override final public void signIn() {
    mSignInStarted = true;
    Log.i(TAG, "Signing In");
    mView.showProgressDialog("Portal", "Trying to connect to your portal...");

    mPortal.addDoneLoadingListener(new Runnable() {
      @Override
      public void run() {

        mView.dismissProgressDialog();

        if (mPortal.getLoadStatus() == LoadStatus.LOADED) {
          String jsonCredentials = AuthenticationManager.CredentialCache.toJson();
          Log.i(TAG, "JSON credential cache = " + jsonCredentials);
          String filePath = mCredentialManager.rsaEncryptData(jsonCredentials.getBytes(), Constants.CRED_FILE);
          Log.i(TAG, "Data encrypted to file path = " + filePath);
          String reconstitutedData = mCredentialManager.rsaDecrpytData(Constants.CRED_FILE);
          Log.i(TAG, "Reconstituted JSON data = " + reconstitutedData);

          Log.i(TAG, "User " + mPortal.getCredential().getUsername());
          final Handler handler = new Handler() ;
          handler.post(new Runnable() {
            @Override public void run() {
              // Download map book
              downloadMapbook();
            }
          });

        }else{
          final String errorMessage = mPortal.getLoadError().getMessage();
          final String cause = mPortal.getLoadError().getCause().getMessage();
          final String message = "Error accessing portal, " + errorMessage +". " + cause;
          Log.e(TAG, message);
          mView.sendResult(RESULT_CANCELED, ERROR_STRING, cause);
        }
      }
    });
    mPortal.loadAsync();
  }

  /**
   * Get the state of the network info
   * @return - boolean, false if network state is unavailable
   * and true if device is connected to a network.
   */
  @Override final public boolean checkForInternetConnectivity() {
    final NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();
    return  networkInfo != null && networkInfo.isConnected();
  }
}
