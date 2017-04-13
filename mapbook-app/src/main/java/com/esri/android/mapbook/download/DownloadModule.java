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
import android.util.Log;
import com.esri.android.mapbook.R;
import com.esri.android.mapbook.util.MapbookApplicationScope;
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
public  class DownloadModule {

  private final Activity mActivity;
  private final DownloadContract.View mView;

  public DownloadModule(final DownloadContract.View view, final Activity activity ){
    mView = view;
    mActivity = activity;
  }

  @Provides
  @MapbookApplicationScope
  DownloadContract.View providesDownloadContractView(){return mView;}

  @Provides
  @MapbookApplicationScope
  public DefaultAuthenticationChallengeHandler providesDefaultAuthenticationChallengeHandler( ){
    return new DefaultAuthenticationChallengeHandler( mActivity);
  }

  @Provides
  @Named("mPortalItemId")
  @MapbookApplicationScope
  public String providesPortalItemId(final Context context) {
    return context.getString(R.string.portalId);
  }

  @Provides
  @Named("clientId")
  @MapbookApplicationScope
  public String providesClientId(final Context context){return context.getString(R.string.client_id);}

  @Provides
  @Named("redirectUri")
  @MapbookApplicationScope
  public String providesRedirectUri(final Context context){ return context.getString(R.string.redirect_uri);}

  @Provides
  @Named("portalUrl")
  @MapbookApplicationScope
  public String providesPortalUrl(final Context context){return context.getString(R.string.portal);}

  @Provides
  @MapbookApplicationScope
  public OAuthConfiguration providesOAuthConfiguration(@Named("clientId") final String clientId,
      @Named("redirectUri") final String redirectUri,
      @Named("portalUrl") final String portalUrl){
    OAuthConfiguration oAuthConfiguration = null;
    try {
      oAuthConfiguration = new OAuthConfiguration(portalUrl, clientId, redirectUri);
    } catch (final MalformedURLException e) {
      Log.e("DownloadModule", e.getMessage());
    }
    return oAuthConfiguration;
  }

  @Provides
  @MapbookApplicationScope
  public Portal providesPortal(@Named("portalUrl") final String portalUrl){
    return new Portal(portalUrl,true);
  }


  @Provides
  @MapbookApplicationScope
  public ConnectivityManager providesNetworkInfo(){
    return (ConnectivityManager) mActivity.getSystemService(Context.CONNECTIVITY_SERVICE);
  }

}
