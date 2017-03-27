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

package com.esri.android.mapbook.download;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import com.esri.android.mapbook.R;
import com.esri.arcgisruntime.portal.Portal;
import com.esri.arcgisruntime.security.DefaultAuthenticationChallengeHandler;
import com.esri.arcgisruntime.security.OAuthConfiguration;
import dagger.Module;
import dagger.Provides;

import javax.inject.Named;
import java.net.MalformedURLException;

/**
 * This is a Dagger module. We use this to pass in the DefaultAuthenticationHandler
 * dependencies to the
 * {@link DownloadActivity}.
 */
@Module
public final class DownloadModule {

  private final Activity mActivity;
  private final DownloadContract.View mView;

  public DownloadModule(DownloadContract.View view, Activity activity ){
    mView = view;
    mActivity = activity;
  }

  @Provides
  DownloadContract.View providesDownloadContractView(){return mView;}

  @Provides
  public DefaultAuthenticationChallengeHandler providesDefaultAuthenticationChallengeHandler( ){
    return new DefaultAuthenticationChallengeHandler( mActivity);
  }

  @Provides
  @Named("mPortalItemId")
  public String providesPortalItemId(Context context) {
    return context.getString(R.string.portalId);
  }

  @Provides
  @Named("clientId")
  public String providesClientId(Context context){return context.getString(R.string.client_id);}

  @Provides
  @Named("redirectUri")
  public String providesRedirectUri(Context context){ return context.getString(R.string.redirect_uri);}

  @Provides
  @Named("portalUrl")
  public String providesPortalUrl(Context context){return context.getString(R.string.portal);}

  @Provides
  public OAuthConfiguration providesOAuthConfiguration(@Named("clientId") String clientId,
      @Named("redirectUri") String redirectUri,
      @Named("portalUrl") String portalUrl){
    OAuthConfiguration oAuthConfiguration = null;
    try {
      oAuthConfiguration = new OAuthConfiguration(portalUrl, clientId, redirectUri);
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
    return oAuthConfiguration;
  }

  @Provides
  public Portal providesPortal(@Named("portalUrl") String portalUrl){
    return new Portal(portalUrl,true);
  }


  @Provides
  public ConnectivityManager providesNetworkInfo(){
    final ConnectivityManager connManager = (ConnectivityManager) mActivity.getSystemService(Context.CONNECTIVITY_SERVICE);
    return connManager;
  }

}
