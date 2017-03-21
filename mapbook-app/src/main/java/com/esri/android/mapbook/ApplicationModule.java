/* Copyright 2017 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * For additional information, contact:
 * Environmental Systems Research Institute, Inc.
 * Attn: Contracts Dept
 * 380 New York Street
 * Redlands, California, USA 92373
 *
 * email: contracts@esri.com
 *
 */
package com.esri.android.mapbook;

import android.app.Application;
import android.content.Context;
import android.os.Environment;
import com.esri.android.mapbook.data.DataManager;
import com.esri.android.mapbook.data.FileManager;
import dagger.Module;
import dagger.Provides;

import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;

/**
 * This is a Dagger module.  We use this to pass in the DataManager dependency to
 */
@Module
public class ApplicationModule {

  private Context mContext;

  public ApplicationModule(Context context)
  {
    mContext = context;
  }

  @Provides
  public Context provideContext(){
    return mContext;
  }

  @Provides
  @Singleton
  public DataManager provideDataManager(){ return new DataManager();}




}
