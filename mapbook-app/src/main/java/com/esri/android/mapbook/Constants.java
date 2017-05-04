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

package com.esri.android.mapbook;

/**
 * These are constants used by classes that don't have an
 * Android context (thus the getString(R.string.id) methods
 * can't be used).  These constants define the names of two files used
 * for persisting credentials on the device.
 */
public class Constants {

  // Name of the file for storing encrypted credentials
  public static final String CRED_FILE = "cred_file";

  // Name of the file for storing bytes for GCM algorithm
  public static final String IV_FILE = "iv_file";

  // Message indicating a new mmpk is available
  public static final String UPDATE_AVAILABLE = "Download latest version";

  // Message indicating there's no update
  public static final String NO_UPDATE_AVAILABLE = "You have the latest version";
}
