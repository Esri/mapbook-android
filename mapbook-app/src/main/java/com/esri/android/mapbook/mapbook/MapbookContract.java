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

import com.esri.android.mapbook.BasePresenter;
import com.esri.android.mapbook.BaseView;
import com.esri.android.mapbook.data.DataManagerCallbacks;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Item;

import java.util.List;

/**
 * This is the contract between the Presenter and View components of the MVP pattern.
 * It defines methods to display the metadata about the mapbook.
 */
public interface MapbookContract {

  interface View extends BaseView<Presenter> {

    /**
     * Show details associated with the mapbook
     * given an Item
     * @param item Item
     */
    void populateMapbookLayout(Item item);

    /**
     * Set the map thumbnail given an array of bytes
     * @param bytes [] of bytes
     */
    void setThumbnailBitmap(byte[] bytes);

    /**
     * Show a Toast message given the message string
     * @param message - String
     */
    void showMessage(String message);

    /**
     * Display mapbook size, modified date and map count
     * for mapbook
     * @param size - long representing file size
     * @param modifiedDate - long representing modified file date
     * @param mapCount - int representing count of maps in mapbook
     */
    void setMapbookMetatdata(long size, long modifiedDate, int mapCount);

    /**
     * Logic for notifying user that mapbook couldn't be found on device
     */
    void showMapbookNotFound();

    /**
     * Assign a list of ArcGIS Maps to the view
     * @param maps - List of ArcGIS map items
     */
    void setMaps(List<ArcGISMap> maps);

    /**
     * Logic for initiating an activity dedicated
     * to downloading the mapbook to the
     * given path on the device.
     * @param path - String representing where on the device the
     *             download should be stored.
     */
    void downloadMapbook(String path);

    /**
     * Toggle the visibility of
     * the download button
     * @param display - boolean, true for show, false for hide
     */
    void toggleDownloadVisibility(boolean display);
  }

  interface Presenter extends BasePresenter {

    /**
     * Determine if mapbook is on the device
     */
    void checkForMapbook();

    /**
     * Load the mobil map package and execute the callback
     * @param callback MapbookCallback execute when mobile map package is loaded
     */
    void loadMapbook( DataManagerCallbacks.MapbookCallback callback);

    /**
     * Return a string representing path to mobile map package
     * @return String representing path to mobile map package on device
     */
    String getMapbookPath();

    /**
     * Process PortalItemUpdateService Broadcast
     * @param modifiedMillis long - The milliseconds representing modified date of PortalItem
     */
    void processBroadcast(long modifiedMillis);
  }
}
