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

import java.io.File;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

  private static final int PERMISSION_TO_READ_EXTERNAL_STORAGE = 5;
  private static final int REQUEST_DOWNLOAD = 1;
  private View mLayout = null;
  private static final String FILE_EXTENSION = ".mmpk";
  public static final String FILE_PATH = "mmpk file path";
  public static File extStorDir;
  public static String extSDCardDirName;
  private static String filename;
  public static String locatorName;
  private static String mmpkFilePath;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.download);

    mLayout = findViewById(R.id.downloadLayout);

    // get sdcard resource name
    extStorDir = Environment.getExternalStorageDirectory();
    // get the directory
    extSDCardDirName = this.getResources().getString(R.string.offlineDirectory);
    // get mobile map package filename
    filename = this.getResources().getString(R.string.mapName);
    // create the full path to the mobile map package file
    mmpkFilePath = createMobileMapPackageFilePath();
    Log.i("MainActivity", "Expecting a file here: " +  mmpkFilePath);

    // Can we read external storage?
    checkForReadStoragePermissions();
  }

  /**
   * Logic for checking external storage for mapbook
   */
  private void checkForMapBook(){
    File mapBook = getMapBook(mmpkFilePath);

    if (mapBook == null){
      // Check for internet connectivity
      if (!checkForInternetConnectivity()){
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.internet_connectivity));
        progressDialog.setTitle(getString(R.string.wireless_problem));
        progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "CANCEL", new DialogInterface.OnClickListener() {
          @Override public void onClick(final DialogInterface dialog, final int which) {
            progressDialog.dismiss();
            finish();
          }
        });
        progressDialog.show();

      }else{
        // Kick off the sign in activity
        Intent intent = new Intent(this, SignInActivity.class);
        startActivityForResult(intent, MainActivity.REQUEST_DOWNLOAD);
      }
    }else{
      Intent mapbookIntent = new Intent(this, MapbookActivity.class);
      mapbookIntent.putExtra(FILE_PATH, mmpkFilePath);
      startActivity(mapbookIntent);
    }
  }
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_DOWNLOAD){
      if (resultCode == RESULT_OK){
        Log.i("MainActivity", "Retrieved file = " + data.getStringExtra("FILE_NAME"));
        Intent mapbookIntent = new Intent(this, MapbookActivity.class);
        startActivity(mapbookIntent);
      }else if (resultCode == RESULT_CANCELED){
        Log.i("MainActivity", "RESULT_CANCELED");
      }
    }
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
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (requestCode == PERMISSION_TO_READ_EXTERNAL_STORAGE) {

      // Request for reading external storage
      if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        // Permission has been granted
        checkForMapBook();

      } else {
        // Permission request was denied.
        Snackbar.make(mLayout, "Permission to read external storage was denied.", Snackbar.LENGTH_SHORT).show();
      }
    }

  }
  /**
   * Get the state of the network info
   * @return - boolean, false if network state is unavailable
   * and true if device is connected to a network.
   */
  private boolean checkForInternetConnectivity(){
    final ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    final NetworkInfo wifi = connManager.getActiveNetworkInfo();
    return  wifi != null && wifi.isConnected();
  }

  /**
   * Determine if we're able to read external storage
    */
  private void checkForReadStoragePermissions(){
    // Explicitly check for file system privs
    int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
    if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
      Log.i("MainActivity", "This application has proper permissions for reading external storage...");

      // Proceed with remaining logic
      checkForMapBook();

    } else {
      Log.i("MainActivity", "This application DOES NOT have appropriate permissions for reading external storage");
      ActivityCompat.requestPermissions(this,
          new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
          PERMISSION_TO_READ_EXTERNAL_STORAGE);
    }
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
  /**
   * Create the mobile map package file location and name structure
   */
  private static String createMobileMapPackageFilePath(){
    return extStorDir.getAbsolutePath() + File.separator + extSDCardDirName + File.separator + filename + FILE_EXTENSION;
  }
}
