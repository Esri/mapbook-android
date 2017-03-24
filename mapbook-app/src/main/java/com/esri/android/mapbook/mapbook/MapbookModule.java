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

import android.content.Context;
import android.os.Environment;
import com.esri.android.mapbook.R;
import com.esri.android.mapbook.data.FileManager;
import com.esri.android.mapbook.util.FragmentScoped;
import dagger.Module;
import dagger.Provides;

import javax.inject.Named;
import java.io.File;

/**
 * This is a Dagger module. We use this to pass in the View and FileManager
 * dependencies to the
 * {@link MapbookPresenter}.
 */

@Module
public final class MapbookModule {

  private final MapbookContract.View mView;
  private  FileManager mFilemanager;

  public MapbookModule(MapbookContract.View view) {
    mView = view;

  }

  @Provides
  MapbookContract.View providesMapbookContractView() { return mView; }

  @Provides
  public FileManager providesFileManager(@Named("storageDirectory") final File storageDirectory,
      @Named("dataDirectory") final String subfolderName,
      @Named("mmpkName") final String fileName,
      @Named("mmpkExtension") final String extension) {
    return new FileManager(storageDirectory, subfolderName, fileName, extension);
  }

  @Provides
  @Named("dataDirectory")
  public String providesDataDirectory(Context context) {
    return context.getString(R.string.offlineDirectory);
  }

  @Provides
  @Named("mmpkName")
  public String providesMobileMapPackageName(Context context) { return context.getString(R.string.mobileMapPackageName); }

  @Provides
  @Named("mmpkExtension")
  public String providesMobileMapExtension(Context context) { return context.getString(R.string.mmpk_extension); }

  @Provides
  @Named("storageDirectory")
  public File providesStorageDirectory() { return Environment.getExternalStorageDirectory(); }
}
