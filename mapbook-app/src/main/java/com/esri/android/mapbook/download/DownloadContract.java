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

import com.esri.android.mapbook.BasePresenter;
import com.esri.android.mapbook.BaseView;

import java.io.InputStream;

/**
 * This is the contract between the Presenter and View components of the MVP pattern.
 * It defines methods and logic used when downloading a mapbook.
 */
public interface DownloadContract {

  interface Presenter extends BasePresenter {
    /**
     * Fetches mapbook from Portal
     */
    void downloadMapbook();

    /**
     * Initiates the authentication process against the Portal
     */
    void signIn();

    /**
     * Checks if the device has any network connectivity
     * @return true for connected, false for no connection
     */
    boolean checkForInternetConnectivity();

    /**
     * Update mobile map package with latest version
     */
    void update();
  }
  interface View extends BaseView<DownloadContract.Presenter> {
    /**
     * Show a Toast with given message string
     * @param message - String
     */
    void showMessage(String message);

    /**
     * Notifies calling activity given an int representing a result code,
     * a key represented by a string, and a message.
     * @param resultCode - int
     * @param key - String
     * @param message - String
     */
    void sendResult( int resultCode, String key, String message);

    /**
     * Shows a progress dialog with given title and message
     * @param title - String representing message title
     * @param message - String representing message
     */
    void showProgressDialog(String title, String message);

    /**
     * Dismiss the progress dialog
     */
    void dismissProgressDialog();

    /**
     * Prompts user to enable WIFI connectivity
     */
    void promptForInternetConnectivity();

    /**
     * Starts an async task to download the file
     * @param itemSize - long representing size of item to download
     * @param inputStream - InputStream representing file
     */
    void executeDownload(long itemSize, InputStream inputStream);
  }
}
