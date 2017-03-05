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
package com.esri.android.mapbook;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.portal.Portal;
import com.esri.arcgisruntime.portal.PortalItem;
import com.esri.arcgisruntime.security.AuthenticationManager;
import com.esri.arcgisruntime.security.DefaultAuthenticationChallengeHandler;
import com.esri.arcgisruntime.security.OAuthConfiguration;

import java.io.*;
import java.net.MalformedURLException;
import java.util.concurrent.ExecutionException;

public class SignInActivity extends AppCompatActivity {
  Portal mPortal = null;
  String mFileName = null;
  ProgressDialog mProgressDialog = null;
  long mPortalItemSize;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mFileName =  getIntent().getStringExtra(MainActivity.FILE_PATH);
    // Set up an authentication handler
    // to be used when loading remote
    // resources or services.
    try {
      OAuthConfiguration oAuthConfiguration = new OAuthConfiguration(getString(R.string.portal),
          getString( R.string.client_id),
          getString(R.string.redirect_uri));
      DefaultAuthenticationChallengeHandler authenticationChallengeHandler = new DefaultAuthenticationChallengeHandler(
          this);
      AuthenticationManager.setAuthenticationChallengeHandler(authenticationChallengeHandler);
      AuthenticationManager.addOAuthConfiguration(oAuthConfiguration);
      signIn();
    } catch (MalformedURLException e) {
      Log.i("MainActivity","OAuth problem : " + e.getMessage());
      Toast.makeText(this, "The was a problem authenticating against the portal.", Toast.LENGTH_LONG).show();
    }
  }

  private void signIn(){
    final Activity  activity= this;
    mProgressDialog = new ProgressDialog(this);
    mProgressDialog.setMessage("Trying to connect to your portal...");
    mProgressDialog.setTitle("Authenticating");
    mProgressDialog.show();
    mPortal = new Portal(getString(R.string.portal));
    mPortal.addDoneLoadingListener(new Runnable() {
      @Override
      public void run() {
        if (mPortal.getLoadStatus() == LoadStatus.LOADED) {

          mProgressDialog.setMessage("Connected to your portal...");
          mProgressDialog.setTitle("Downloading mapbook");

          Handler handler = new Handler() ;
          handler.post(new Runnable() {
            @Override public void run() {
              // Download map book
              downloadMapBook();
            }
          });


        }else{
          String errorMessage = mPortal.getLoadError().getMessage();
          String cause = mPortal.getLoadError().getCause().getMessage();
          String message = "Error accessing " + getString(R.string.portal) + ". " + errorMessage +". " + cause;
          Log.i("SignInActivity", message);
          Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
          Log.e("SignInActivity", message);
          mProgressDialog.dismiss();
        }

      }
    });
    mPortal.loadAsync();
  }

  private void downloadMapBook(){
    final Activity activity = this;
    final ProgressDialog progressDialog = new ProgressDialog(this);
    progressDialog.setMessage("Please wait... ");
    progressDialog.setTitle("Downloading Mapbook");
    progressDialog.setIndeterminate(false);
    progressDialog.setMax(100);
    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
    progressDialog.setCancelable(true);
    progressDialog.show();
    final PortalItem portalItem = new PortalItem(mPortal, getString(R.string.portalId));
    portalItem.loadAsync();
    portalItem.addDoneLoadingListener(new Runnable() {
      @Override public void run() {
        progressDialog.dismiss();
        final ListenableFuture<InputStream> future = portalItem.fetchDataAsync();
        mPortalItemSize = portalItem.getSize();
        Log.i("SignInActivity", "Portal item size = " + mPortalItemSize);
        future.addDoneListener(new Runnable() {
          @Override public void run() {

            try {
              InputStream inputStream = future.get();
              new DownloadMobileMapPackage().execute(inputStream);

            } catch (Exception e) {
              mProgressDialog.dismiss();

              Intent intent = new Intent();
              Toast.makeText(activity, "Problem downloading file, " + e.getMessage(), Toast.LENGTH_LONG);
              setResult(RESULT_CANCELED, intent);
              finish();
            }
          }
        });
      }
    });
  }

  private void setProgressPercent(Integer progressPercent){
    Log.i("SignInActivity", "Progress " + progressPercent);
  }
  private class DownloadMobileMapPackage extends AsyncTask<InputStream, String, String> {

    @Override protected String doInBackground(InputStream... params) {
      String path = null;
      try {
        InputStream inputStream = params[0];
        File storageDirectory = Environment.getExternalStorageDirectory();
        File data = new File(mFileName);
        OutputStream os = new FileOutputStream(data);
        byte[] buffer = new byte[1024];
        int bytesRead;
        //read from is to buffer
        int y = 1;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
          os.write(buffer, 0, bytesRead);
          publishProgress(""+(int)((y*100)/mPortalItemSize));
          y = y + 1;

        }

        inputStream.close();

        //flush OutputStream to write any buffered data to file
        os.flush();
        os.close();

        path =  data.getPath();

      }catch (Exception io){
        Log.i("SignInActivity", "Async Task Exception " + io.getMessage());
      }
      return path;
    }
    protected void onProgressUpdate(String... progress) {
      super.onProgressUpdate(progress);
      Log.i("SignInActivity","Progress..." + progress[0]);
      mProgressDialog.setProgress(Integer.parseInt(progress[0]));
    }

    protected void onPostExecute(String result) {
      Log.i("SignInActivity" ,"Downloaded " + result);
      Log.i("SignInActivity" ,"Portal item size = " + mPortalItemSize);
      mProgressDialog.dismiss();
      Intent intent = new Intent();
      setResult(RESULT_OK, intent);
      finish();

    }
  }
}
