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

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.support.v4.app.Fragment;
import com.esri.android.mapbook.R;
import com.esri.android.mapbook.mapbook.MapbookContract;

public class DownloadFragment extends Fragment implements DownloadContract.View {
  @Override public void setPresenter(MapbookContract.Presenter presenter) {

  }

  @Override public void promptForInternetConnectivity() {
    final ProgressDialog progressDialog = new ProgressDialog(getActivity());
    progressDialog.setMessage(getString(R.string.internet_connectivity));
    progressDialog.setTitle(getString(R.string.wireless_problem));
    progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "CANCEL", new DialogInterface.OnClickListener() {
      @Override public void onClick(final DialogInterface dialog, final int which) {
        progressDialog.dismiss();
      }
    });
    progressDialog.show();
  }
}
