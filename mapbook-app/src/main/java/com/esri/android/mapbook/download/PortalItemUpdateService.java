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

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.esri.android.mapbook.ApplicationModule;
import com.esri.android.mapbook.MapBookApplication;
import com.esri.android.mapbook.R;
import com.esri.arcgisruntime.portal.Portal;
import com.esri.arcgisruntime.portal.PortalItem;
import com.esri.arcgisruntime.security.AuthenticationManager;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Calendar;

/**
 * An IntentService responsible for retrieving metadata about a
 * PortalItem and sending a
 */
public class PortalItemUpdateService extends IntentService {

  @Inject Portal mPortal;
  @Inject @Named("mPortalItemId") String mPortalItemId;
  @Inject CredentialCryptographer mCredCryptographer;

  private static final String TAG = PortalItemUpdateService.class.getSimpleName();

  public PortalItemUpdateService() {super("PortalItemUpdateService"); }

  /**
   * Inject dependencies
   */
  @Override
  public void onCreate() {
    super.onCreate();
    DaggerDownloadComponent.builder().applicationComponent(((MapBookApplication) getApplication())
        .getComponent())
        .applicationModule(new ApplicationModule(getApplicationContext()))
        .downloadModule(new DownloadModule())
        .build()
        .inject(this);
  }
  /**
   * Broadcast the modified date for a given portal item
   * @param intent - Intent
   */
  @Override protected void onHandleIntent(@Nullable final Intent intent) {
    Log.i(TAG, "onHandleIntent");

    final String credentialString;

    try {

      credentialString = mCredCryptographer.decrypt();

      if ( credentialString == null || credentialString.length() == 0  ) {
        handleDecryptionFailure();
      }
      Log.i(TAG, "Decrypted credential string = " +credentialString);

      // Reconstitute the AuthenticationManager from cached credentials
      AuthenticationManager.CredentialCache.restoreFromJson(credentialString);

      final PortalItem portalItem = new PortalItem(mPortal, mPortalItemId);
      portalItem.loadAsync();
      portalItem.addDoneLoadingListener(new Runnable() {
        @Override public void run() {
          final Calendar dateModified = portalItem.getModified();
          final long timeInMillis = dateModified.getTimeInMillis();

          // Send a broadcast out with latest time stamp from portal item
          final Intent localIntent =
              new Intent(getString(R.string.BROADCAST_ACTION))
                  // Puts the status into the Intent
                  .putExtra(getString(R.string.LATEST_DATE), timeInMillis);
          broadcastIntent(localIntent);
        }
      });


    } catch (Exception e) {
      Log.e(TAG, e.getClass().getSimpleName() + " " + e.getMessage());
      if (e.getCause() != null){
        Log.e(TAG, e.getCause().getMessage());
      }
      handleDecryptionFailure();
    }
  }

  private void handleDecryptionFailure(){
    Log.e(TAG, "Unable to decrypt credentials on device");
    // Send a broadcast with no extras
    final Intent errorIntent =
        new Intent(getString(R.string.BROADCAST_ACTION));
    broadcastIntent(errorIntent);
  }

  /**
   * Send the broadcast
   * @param intent - Intent
   */
  private void broadcastIntent(final Intent intent) {
    // Broadcasts the Intent to receivers in this app.
    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    Log.i(TAG, "Broadcast sent");
  }
}
