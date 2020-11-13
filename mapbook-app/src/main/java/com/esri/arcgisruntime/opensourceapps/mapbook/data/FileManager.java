/*
 *  Copyright 2017 Esri
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * https://www.apache.org/licenses/LICENSE-2.0
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

package com.esri.arcgisruntime.opensourceapps.mapbook.data;

import android.util.Log;

import java.io.File;

public class FileManager implements FileManagerContract {
  private static File fileDir;
  private static String fileName;
  private static String extension;
  private static File file;

  public FileManager(final File directory,  final String fName, final String fileExtension){
    fileDir = directory;
    fileName = fName;
    extension = fileExtension;
    file = new File(createMobileMapPackageFilePath());
  }
  /**
   /**
   * Generate the path for the location of the mobile map package.
   *
   * @return String
   */
  final public String createMobileMapPackageFilePath(){
    return fileDir.getAbsolutePath() +  File.separator + fileName + extension;
  }

  /**
   * Return the modified date of the file
   * @return long representing milliseconds
   */
  final public long getModifiedDate(){
    long modifiedDate = 0;
    if (file!= null){
      modifiedDate =  file.lastModified();
    }
    return modifiedDate;
  }

  /**
   * Return the file size
   * @return - long representing file size
   */
  final public long getSize(){
    long size = 0;
    if (file != null){
      size = file.length();
    }
    return size;
  }

  /**
   * Returns a string representing file path of mapbook.
   * Returns a null string if file doesn't exist.
   * @return - String
   */
  final public String fileExists(){
    String path = null;
    file = new File(createMobileMapPackageFilePath());
    Log.i("FileManager", "Searching for file " + createMobileMapPackageFilePath());
    if (file.exists()){
      path = createMobileMapPackageFilePath();
    }
    return path;
  }

  /**
   * Delete the mobile map package
   * @return boolean - true if deleted, false if not deleted.
   */
  @Override public boolean deleteMmpk() {
    boolean fileDeleted = false;
    if (file.exists()){
      fileDeleted = file.delete();
    }
    return fileDeleted;
  }
}
