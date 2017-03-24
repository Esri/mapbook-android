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

import android.util.Log;

import java.io.File;

public class FileManager {
  private static File extStorDir;
  private static String  extSDCardDirName;
  private static String fileName;
  private static String extension;
  private static File file;

  public FileManager(final File directory, final String subfolderName, final String fName, final String fileExtension){
    extStorDir = directory;
    extSDCardDirName = subfolderName;
    fileName = fName;
    extension = fileExtension;
    file = new File(createMobileMapPackageFilePath());
  }
  /**
   * Create the mobile map package file location and name structure
   */
  public  String createMobileMapPackageFilePath(){
    return extStorDir.getAbsolutePath() + File.separator + extSDCardDirName + File.separator + fileName + extension;
  }

  public long getModifiedDate(){
    long modifiedDate = 0;
    if (file!= null){
      modifiedDate =  file.lastModified();
    }
    return modifiedDate;
  }

  public long getSize(){
    long size = 0;
    if (file != null){
      size = file.length();
    }
    return size;
  }
  public boolean fileExists(){
    file = new File(createMobileMapPackageFilePath());
    Log.i("FileManager", "Searching for file " + createMobileMapPackageFilePath());
    return file.exists();
  }
}
