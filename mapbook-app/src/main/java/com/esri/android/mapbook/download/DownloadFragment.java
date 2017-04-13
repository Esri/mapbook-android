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

import java.io.*;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static com.esri.android.mapbook.download.DownloadActivity.ERROR_STRING;
import static com.esri.android.mapbook.mapbook.MapbookFragment.FILE_PATH;

public class DownloadFragment extends Fragment implements DownloadContract.View {

  DownloadContract.Presenter mPresenter;
  private static final String TAG = DownloadFragment.class.getSimpleName();
  private ProgressDialog mProgressDialog = null;
  private String mFileName = null;

  public DownloadFragment(){}

  public static DownloadFragment newInstance(){
    final Bundle args = new Bundle();

    final DownloadFragment fragment = new DownloadFragment();
    fragment.setArguments(args);
    return fragment;

  }
  @Override
  final public void onCreate(@Nullable Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    mProgressDialog = new ProgressDialog(getActivity());
    // Calling activity should pass the file path
    Bundle args = getArguments();
    if (args.containsKey(FILE_PATH)){
      mFileName = args.getString(FILE_PATH);
    }else{
      sendResult(RESULT_CANCELED, ERROR_STRING, "File path not found in fragment arguments");
    }
  }

  @Override final public void setPresenter(final DownloadContract.Presenter presenter) {
    mPresenter = presenter;
  }


  @Override final public void showMessage(final String message) {
    Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
  }

  @Override final public void sendResult( final int resultCode, final String key, final String message) {
    final Intent intent = new Intent();
    getActivity().setResult(resultCode, intent);
    if (key != null){
      intent.putExtra(key, message);
    }
    getActivity().finish();
  }

  @Override final public void showProgressDialog(final String title, final String message) {
    mProgressDialog.dismiss();
    mProgressDialog.setMessage(message);
    mProgressDialog.setTitle(title);
    mProgressDialog.show();
  }

  @Override final public void dismissProgressDialog() {
    mProgressDialog.dismiss();
  }

  @Override final public void promptForInternetConnectivity() {

    final ProgressDialog networkDialog = new ProgressDialog(getContext());
    networkDialog.setButton(DialogInterface.BUTTON_POSITIVE, "ENABLE WI-FI", new DialogInterface.OnClickListener() {
      @Override final public void onClick(final DialogInterface dialog, final int which) {
        networkDialog.dismiss();
        startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
      }
    });
    networkDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "CANCEL", new DialogInterface.OnClickListener() {
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

  @Override
  public final void onResume() {
    super.onResume();
    Log.i(TAG, "onResume");
    mPresenter.start();
  }

  private final class DownloadMobileMapPackage extends AsyncTask<Object, Integer, String> {

    private ProgressDialog mTaskProgressDialog = null;

    @Override protected String doInBackground(Object... params) {
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

      } catch (Exception e) {
        Log.e(TAG, "Async Task Exception " + e.getMessage());
      } finally {
        try {
          if (os != null)
            os.close();
          if (inputStream != null)
            inputStream.close();
        } catch (IOException ioException) {
          Log.e(TAG, ioException.getMessage());
        }

      }
      return path;
    }
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
    @Override
    final protected void onProgressUpdate(final Integer... progress) {
      super.onProgressUpdate(progress);
      Integer p = progress[0];
      mTaskProgressDialog.setProgress(p);
    }

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
