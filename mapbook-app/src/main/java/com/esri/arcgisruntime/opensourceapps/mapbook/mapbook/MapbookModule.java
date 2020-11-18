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

package com.esri.arcgisruntime.opensourceapps.mapbook.mapbook;

import android.content.Context;
import android.os.Environment;
import androidx.core.content.ContextCompat;
import com.esri.arcgisruntime.opensourceapps.mapbook.R;
import com.esri.arcgisruntime.opensourceapps.mapbook.data.FileManager;
import com.esri.arcgisruntime.opensourceapps.mapbook.util.MapbookApplicationScope;
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
public class MapbookModule {

  private final MapbookContract.View mView;

  public MapbookModule(final MapbookContract.View view) {
    mView = view;

  }

  @Provides
  @MapbookApplicationScope
  MapbookContract.View providesMapbookContractView() { return mView; }

  @Provides
  @MapbookApplicationScope
  public FileManager providesFileManager(@Named("storageDirectory") final File storageDirectory,
      @Named("mmpkName") final String fileName,
      @Named("mmpkExtension") final String extension) {
    return new FileManager(storageDirectory, fileName, extension);
  }


  @Provides
  @MapbookApplicationScope
  @Named("mmpkName")
  public String providesMobileMapPackageName(final Context context) { return context.getString(R.string.mobileMapPackageName); }

  @Provides
  @MapbookApplicationScope
  @Named("mmpkExtension")
  public String providesMobileMapExtension(final Context context) { return context.getString(R.string.mmpk_extension); }

  @Provides
  @MapbookApplicationScope
  @Named("storageDirectory")
  public File providesStorageDirectory(final Context context) {
    File directory = null;
    final File [] files = ContextCompat.getExternalFilesDirs(context, null);
    if (files.length > 0){
      directory = files[0];
    }else{
      directory = Environment.getDataDirectory();
    }
    return directory;
  }
}
