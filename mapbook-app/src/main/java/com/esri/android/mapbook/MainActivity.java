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

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.esri.android.mapbook.download.DownloadActivity;
import com.esri.android.mapbook.mapbook.MapbookActivity;

import javax.inject.Inject;
import java.io.File;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

  private static final int PERMISSION_TO_READ_EXTERNAL_STORAGE = 5;
  private static final int REQUEST_DOWNLOAD = 1;
  private View mLayout = null;
  public static final String FILE_PATH = "mmpk file path";



  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);


    setContentView(R.layout.download);

    mLayout = findViewById(R.id.downloadLayout);


  }

  /**
   * Logic for checking external storage for mapbook
   */
  private void checkForMapBook(){
//    File mapBook = getMapBook(mmpkFilePath);
//
//    if (mapBook == null){
//      // Check for internet connectivity
//      if (!checkForInternetConnectivity()){
//        final ProgressDialog progressDialog = new ProgressDialog(this);
//        progressDialog.setMessage(getString(R.string.internet_connectivity));
//        progressDialog.setTitle(getString(R.string.wireless_problem));
//        progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "CANCEL", new DialogInterface.OnClickListener() {
//          @Override public void onClick(final DialogInterface dialog, final int which) {
//            progressDialog.dismiss();
//            finish();
//          }
//        });
//        progressDialog.show();
//
//      }else{
//        // Kick off the sign in activity
//        Intent intent = new Intent(this, DownloadActivity.class);
//        intent.putExtra(FILE_PATH, mmpkFilePath);
//        startActivityForResult(intent, MainActivity.REQUEST_DOWNLOAD);
//      }
//    }else{
//      Intent mapbookIntent = new Intent(this, MapbookActivity.class);
//      mapbookIntent.putExtra(FILE_PATH, mmpkFilePath);
//      startActivity(mapbookIntent);
//    }
  }

  /**
   * Do we have a copy of the mapbook?
   * @param  path String representing full path of mapbook
   * @return File representing map book if present in external storage
   */
  private File getMapBook(String path){
    File mapBook = null;
    File storageDirectory = Environment.getExternalStorageDirectory();
    File data = new File(storageDirectory, File.separator + getString(R.string.offlineDirectory));
    File [] maps = data.listFiles();
    if (maps!=null && maps.length > 0){
      for (int z=0; z < maps.length ; z++){
        File file= maps[z];
        Log.i("MainActivity", "File path = " + file.getPath());
        if (file.getPath().equalsIgnoreCase(path)){
          mapBook = file;
          break;
        }
      }
    }

    return mapBook;
  }
}
