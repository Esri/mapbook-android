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

package com.esri.arcgisruntime.opensourceapps.mapbook.download;

import com.esri.arcgisruntime.opensourceapps.mapbook.ApplicationComponent;
import com.esri.arcgisruntime.opensourceapps.mapbook.ApplicationModule;
import com.esri.arcgisruntime.opensourceapps.mapbook.util.MapbookApplicationScope;
import dagger.Component;

/**
 * This is a Dagger component. Refer to {@link com.esri.arcgisruntime.opensourceapps.mapbook.MapBookApplication} for the list of Dagger components
 * used in this application.
 * <P>
 * Because this component depends on the {@link ApplicationComponent}, which is a singleton, a
 * scope must be specified. All fragment components use a custom scope for this purpose.
 */

@MapbookApplicationScope
@Component(dependencies = ApplicationComponent.class, modules = {DownloadModule.class, ApplicationModule.class})
public interface DownloadComponent {

  void inject (DownloadActivity activity);
  void inject (DownloadPresenter presenter);
  void inject (CredentialCryptographer manager);
  void inject (PortalItemUpdateService service);
}
