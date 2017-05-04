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

package com.esri.android.mapbook.map;

import android.content.Context;
import com.esri.android.mapbook.data.DataManager;
import com.esri.android.mapbook.util.MapbookApplicationScope;
import dagger.Module;
import dagger.Provides;

/**
 * This is a Dagger module. We use this to pass in the
 * {@link com.esri.android.mapbook.data.DataManager} and the
 * {@link ContentExtractor}
 *  to the{@link MapActivity}.
 */
@Module
public class MapModule {

  private final MapContract.View mView;

  public MapModule(final MapContract.View view){
    mView = view;
  }

  @Provides
  @MapbookApplicationScope
  public DataManager provideDataManager(){ return new DataManager();}

  @Provides
  @MapbookApplicationScope
  public MapContract.View providesMapContractView(){ return mView; }

  @Provides
  public ContentExtractorContract providesPopupInteractor(final Context context){ return new ContentExtractor(context); }

}
