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

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

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
    final ProgressDialog progressDialog = new ProgressDialog(this);
    progressDialog.setMessage("Trying to connect to your portal...");
    progressDialog.setTitle("Authenticating");
    progressDialog.show();
    mPortal = new Portal(getString(R.string.portal));
    mPortal.addDoneLoadingListener(new Runnable() {
      @Override
      public void run() {
        if (mPortal.getLoadStatus() == LoadStatus.LOADED) {

          progressDialog.dismiss();

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
        }
        progressDialog.dismiss();
      }
    });
    mPortal.loadAsync();
  }

  private void downloadMapBook(){
    final ProgressDialog progressDialog = new ProgressDialog(this);
    progressDialog.setMessage("Please wait... ");
    progressDialog.setTitle("Downloading Mapbook");
    progressDialog.show();
    final PortalItem portalItem = new PortalItem(mPortal, getString(R.string.portalId));
    portalItem.loadAsync();
    portalItem.addDoneLoadingListener(new Runnable() {
      @Override public void run() {
        progressDialog.dismiss();
        final ListenableFuture<InputStream> future = portalItem.fetchDataAsync();
        future.addDoneListener(new Runnable() {
          @Override public void run() {
            Intent intent = new Intent();
            try {
              InputStream inputStream = future.get();
              new DownloadMobileMapPackage().execute(inputStream);
//              Log.i("SignInActivity", "Total bytes = " + inputStream.available());
//              File storageDirectory = Environment.getExternalStorageDirectory();
//              File data = new File(storageDirectory, "/Data/Risk_Data.mmpk");
//              OutputStream os = new FileOutputStream(data);
//              byte[] buffer = new byte[1024];
//              int bytesRead;
//              //read from is to buffer
//              while((bytesRead = inputStream.read(buffer)) !=-1){
//                os.write(buffer, 0, bytesRead);
//              }
//
//              inputStream.close();
//              Log.i("SignInActivity", "Input stream closed");
//              //flush OutputStream to write any buffered data to file
//              os.flush();
//              os.close();
//              intent.putExtra("FILE_NAME", data.getAbsolutePath());
//              setResult(RESULT_OK, intent);
            } catch (InterruptedException e) {
              e.printStackTrace();
           //   setResult(RESULT_CANCELED, intent);
            } catch (ExecutionException e) {
              e.printStackTrace();
            //  setResult(RESULT_CANCELED, intent);
            }
           // finish();
          }
        });
      }
    });
  }

  private void setProgressPercent(Integer progressPercent){
    Log.i("SignInActivity", "Progress " + progressPercent);
  }
  private class DownloadMobileMapPackage extends AsyncTask<InputStream, Integer, String> {

    @Override protected String doInBackground(InputStream... params) {
      String path = null;
      try {
        InputStream inputStream = params[0];
        File storageDirectory = Environment.getExternalStorageDirectory();
        File data = new File(storageDirectory, "/Data/OfflineMap.mmpk");
        OutputStream os = new FileOutputStream(data);
        byte[] buffer = new byte[1024];
        int bytesRead;
        //read from is to buffer
        while ((bytesRead = inputStream.read(buffer)) != -1) {
          os.write(buffer, 0, bytesRead);
        }

        inputStream.close();
        Log.i("SignInActivity", "Input stream closed");
        //flush OutputStream to write any buffered data to file
        os.flush();
        os.close();

        path =  data.getPath();
      }catch (IOException io){
        Log.i("SignInActivity", "Async Task Exception " + io.getMessage());
      }
      return path;
    }
    protected void onProgressUpdate(Integer... progress) {
      setProgressPercent(progress[0]);
    }

    protected void onPostExecute(Long result) {
      Log.i("SignInActivity" ,"Downloaded " + result + " bytes");
    }
  }
}
