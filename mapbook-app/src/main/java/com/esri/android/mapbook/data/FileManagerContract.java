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

package com.esri.android.mapbook.data;

/**
 * This is an interface defining methods for managing mapbook files on the device.
 */
public interface FileManagerContract {

  /**
   * Generate the path for the location of the mobile map package.
   *
   * @return String
   */
  String createMobileMapPackageFilePath();

  /**
   * Return the modified date of the file
   * @return long representing milliseconds
   */
  long getModifiedDate();

  /**
   * Return the file size
   * @return - long representing file size
   */
  long getSize();

  /**
   * Returns a string representing file path of mapbook.
   * Returns a null string if file doesn't exist.
   * @return - String
   */
  String fileExists();

  /**
   * Delete mobile map package
   */
  boolean deleteMmpk();
}
