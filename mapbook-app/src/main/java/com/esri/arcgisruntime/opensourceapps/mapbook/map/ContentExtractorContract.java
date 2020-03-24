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

package com.esri.arcgisruntime.opensourceapps.mapbook.map;

import com.esri.arcgisruntime.opensourceapps.mapbook.data.Entry;
import com.esri.arcgisruntime.mapping.GeoElement;
import com.esri.arcgisruntime.mapping.popup.Popup;

import java.util.List;

/**
 * An interface for wrapping extracting data from features.  The PopupManager
 * relies on a Context object.  Application logic in the MapPresenter needs a PopupManager but
 * should have no knowledge of Android specific objects (like Context), so this interface
 * is used to interact with the PopupManager.
 *
 * TODO Question for Dan, thoughts on this design?
 * See this link for more:
 * http://stackoverflow.com/questions/34303510/does-the-presenter-having-knowledge-of-the-activity-context-a-bad-idea-in-the/34664466#34664466
 */

public interface ContentExtractorContract {

  List<Entry> getPopupFields(Popup popup);

  List<Entry> getEntriesFromGeoElement(GeoElement geoElement);


}
