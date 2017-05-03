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

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.widget.Toast;
import com.esri.android.mapbook.R;

import java.io.InputStream;
import java.io.File;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static com.esri.android.mapbook.download.DownloadActivity.ERROR_STRING;
import static com.esri.android.mapbook.mapbook.MapbookFragment.FILE_PATH;

/**
 * This fragment is responsible for showing progress dialogs and executing the
 * AsyncTask for downloading the mapbook from the Portal. It's the View in the MVP pattern.
 */
public class DownloadFragment extends Fragment implements DownloadContract.View {

  DownloadContract.Presenter mPresenter;
  private static final String TAG = DownloadFragment.class.getSimpleName();
  private ProgressDialog mProgressDialog = null;
  private String mFileName = null;

  /**
   * Default constructor
   */
  public DownloadFragment(){}

  /**
   * Static method that returns a Mapbook Fragment with an
   * empty Bundle that can be configured by caller.
   * @return - DownloadFragment
   */
  public static DownloadFragment newInstance(){
    final Bundle args = new Bundle();

    final DownloadFragment fragment = new DownloadFragment();
    fragment.setArguments(args);
    return fragment;

  }

  /**
   * On creation of fragment, check for the path of the
   * mobile map package.
   * @param savedInstanceState - Bundle
   */
  @Override
  final public void onCreate(@Nullable final Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    mProgressDialog = new ProgressDialog(getActivity());
    // Calling activity should pass the file path
    final Bundle args = getArguments();
    if (args.containsKey(FILE_PATH)){
      mFileName = args.getString(FILE_PATH);
    }else{
      sendResult(RESULT_CANCELED, ERROR_STRING, getString(R.string.path_not_found));
    }
  }

  /**
   * Set the presenter for this view
   * @param presenter - DownloadPresenter
   */
  @Override final public void setPresenter(final DownloadContract.Presenter presenter) {
    mPresenter = presenter;
  }

  /**
   * Show a Toast with given message string
   * @param message - String
   */
  @Override final public void showMessage(final String message) {
    Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
  }

  /**
   * Notifies calling activity given an int representing a result code,
   * a key represented by a string, and a message.
   * @param resultCode - int
   * @param key - String
   * @param message - String
   */
  @Override final public void sendResult( final int resultCode, final String key, final String message) {
    final Intent intent = new Intent();
    getActivity().setResult(resultCode, intent);
    if (key != null){
      intent.putExtra(key, message);
    }
    getActivity().finish();
  }

  /**
   * Shows a progress dialog with given title and message
   * @param title - String representing message title
   * @param message - String representing message
   */
  @Override final public void showProgressDialog(final String title, final String message) {
    mProgressDialog.dismiss();
    mProgressDialog.setMessage(message);
    mProgressDialog.setTitle(title);
    mProgressDialog.show();
  }

  /**
   * Dismiss the progress dialog
   */
  @Override final public void dismissProgressDialog() {
    mProgressDialog.dismiss();
  }

  /**
   * Prompts user to enable WIFI connectivity
   */
  @Override final public void promptForInternetConnectivity() {

    final ProgressDialog networkDialog = new ProgressDialog(getContext());
    networkDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.enable_wifi), new DialogInterface.OnClickListener() {
      @Override final public void onClick(final DialogInterface dialog, final int which) {
        networkDialog.dismiss();
        startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
      }
    });
    networkDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.cancel), new DialogInterface.OnClickListener() {
      @Override final public void onClick(final DialogInterface dialog, final int which) {
        networkDialog.dismiss();
        sendResult(RESULT_CANCELED, ERROR_STRING, "A Network Connection is Required to Download the Mobile Map Package");
      }
    });
    networkDialog.setTitle(getString(R.string.wireless_problem));
    networkDialog.setMessage(getString(R.string.internet_connectivity));
    networkDialog.show();
  }

  @Override final public void executeDownload(final long itemSize, final InputStream inputStream) {
    new DownloadMobileMapPackage().execute(inputStream, mFileName, itemSize);
  }

  /**
   * Start the presenter every time the fragment resumes.
   */
  @Override
  public final void onResume() {
    super.onResume();
    Log.i(TAG, "onResume");
    mPresenter.start();
  }

  /**
   * AsyncTask used for downloading mobile map package and writing it to disk.
   */
  private final class DownloadMobileMapPackage extends AsyncTask<Object, Integer, String> {

    private ProgressDialog mTaskProgressDialog = null;

    @Override protected String doInBackground(final Object... params) {
      Log.i(TAG, "Starting DownloadTask");
      String path = null;
      final InputStream inputStream = (InputStream) params[0];
      final String fileName = (String) params[1];
      final File data = new File(fileName);
      final Long portalItemSize = (Long) params[2];
      OutputStream os = null;

      try {
        os = new FileOutputStream(data);

        final byte[] buffer = new byte[1024];
        int bytesRead;
        long total = 0;

        while ((bytesRead = inputStream.read(buffer)) != -1) {

          // allow canceling with back button
          if (isCancelled()) {
            inputStream.close();
            break;
          }

          total = total + bytesRead;

          final long progress = (total*100)/portalItemSize;
          publishProgress((int)progress);
          os.write(buffer, 0, bytesRead);
        }

        inputStream.close();

        //flush OutputStream to write any buffered data to file
        os.flush();
        os.close();

        path =  data.getPath();


      } catch (final Exception e){
        Log.e(TAG, e.getClass().getSimpleName()+ " " + e.getMessage());
      }
      finally {
        try {
          if (os != null)
            os.close();
          if (inputStream != null)
            inputStream.close();
        } catch (final IOException ioException) {
          Log.e(TAG, ioException.getMessage());
        }

      }
      return path;
    }

    /**
     * Show user a progress dialog as file is downloaded
     */
    @Override
    final protected void onPreExecute() {
      super.onPreExecute();
      mTaskProgressDialog = new ProgressDialog(getActivity());
      mTaskProgressDialog.setIndeterminate(false);
      mTaskProgressDialog.setMax(100);
      mTaskProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
      mTaskProgressDialog.setCancelable(false);
      mTaskProgressDialog.setCanceledOnTouchOutside(false);
      mTaskProgressDialog.setMessage(getString(R.string.wait));
      mTaskProgressDialog.setTitle(getString(R.string.downloading_mapbook));
      mTaskProgressDialog.show();
    }

    /**
     * Update the progress of the download
     * @param progress Integer
     */
    @Override
    final protected void onProgressUpdate(final Integer... progress) {
      super.onProgressUpdate(progress);
      final Integer p = progress[0];
      mTaskProgressDialog.setProgress(p);
    }

    /**
     * Once the download is complete, return to the calling activity.
     * @param result - String
     */
    @Override
    final protected void onPostExecute(final String result) {
      mTaskProgressDialog.dismiss();
      if (result == null){
        sendResult(RESULT_CANCELED, ERROR_STRING,  null);
      }else{
        sendResult(RESULT_OK, FILE_PATH, result);
      }

    }
  }
}
