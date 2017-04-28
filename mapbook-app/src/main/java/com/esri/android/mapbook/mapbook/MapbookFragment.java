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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.esri.android.mapbook.R;
import com.esri.android.mapbook.download.DownloadActivity;
import com.esri.android.mapbook.map.MapActivity;
import com.esri.android.mapbook.util.ActivityUtils;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Item;

import java.util.Calendar;
import java.util.List;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static com.esri.android.mapbook.download.DownloadActivity.ERROR_STRING;

public class MapbookFragment extends Fragment implements MapbookContract.View {

  MapbookContract.Presenter mPresenter;

  private ConstraintLayout mRoot = null;
  private MapbookAdapter mapAdapter = null;
  private static final int REQUEST_DOWNLOAD = 1;
  public static final String FILE_PATH = "mmpk file path";
  private final String TAG = MapbookFragment.class.getSimpleName();

  /**
   * Default constructor
   */
  public MapbookFragment(){}

  /**
   * Static method that returns a Mapbook Fragment with an
   * empty Bundle that can be configured by caller.
   * @return - MapbookFragment
   */
  public static MapbookFragment newInstance() {

    final Bundle args = new Bundle();

    final MapbookFragment fragment = new MapbookFragment();
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  final public void onCreate (final Bundle savedInstanceState){
    super.onCreate(savedInstanceState);

    // The filter's action is BROADCAST_ACTION
    final IntentFilter latestVersionIntentFilter = new IntentFilter(
        getString(R.string.BROADCAST_ACTION));

    // Instantiates a new PortalItemBroadcastReceiver
    final PortalItemBroadcastReceiver portalItemBroadcastReceiver =
        new PortalItemBroadcastReceiver();
    // Registers the PortalItemBroadcastReceiver and its intent filters
    LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
        portalItemBroadcastReceiver,
        latestVersionIntentFilter);
  }

  /**
   * Set up the menu options for the view
   * @param menu
   * @param inflater
   */
  @Override
  public final void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
    getActivity().getMenuInflater().inflate(R.menu.logout, menu);

  }

  /**
   * Logic for menu options
   * @param menuItem - MenuItem
   * @return boolean
   */
  @Override
  public boolean onOptionsItemSelected(final MenuItem menuItem){
    super.onOptionsItemSelected(menuItem);

    if (menuItem.getItemId() == R.id.action_logout){
      mPresenter.logout();
    }

    return true;
  }
  /**
   * Set up the view components and attach a listener
   * to the map book adapter
   * @param inflater - LayoutInflater
   * @param container - ViewGroup
   * @param savedInstanceState - Bundle
   * @return the View (can be null)
   */
  @Override
  final public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
      final Bundle savedInstanceState) {
    mRoot = (ConstraintLayout) container;
    final RecyclerView mRecyclerView = (RecyclerView) mRoot.findViewById(R.id.recyclerView);
    final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
    mRecyclerView.setLayoutManager( layoutManager);
    mapAdapter = new MapbookAdapter(new MapbookAdapter.OnItemClickListener() {
      @Override public void onItemClick(final ImageView image, final String title, final int position) {
        final Intent intent = new Intent(getContext(), MapActivity.class);
        intent.putExtra(getString(R.string.index), position);
        intent.putExtra(getString(R.string.map_title), title);
        intent.putExtra(MapbookFragment.FILE_PATH, mPresenter.getMapbookPath());
        startActivity(intent);
      }
    });
    mRecyclerView.setAdapter(mapAdapter);
    layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);

    // Set on click listener for download button
    final ImageView downloadBtn = (ImageView) getActivity().findViewById(R.id.imageDownloadBtn);
    downloadBtn.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(final View v) {
        Log.i(TAG, "Download button clicked");
        downloadMapbook(mPresenter.getMapbookPath());
      }
    });

    // Enable fragment to have options menu
    setHasOptionsMenu(true);

    return null;
  }

  /**
   * Start the presenter every time the fragment resumes.
   */
  @Override
  final public void onResume() {
    super.onResume();
    mPresenter.start();
  }

  /**
   * Set the presenter for the view
   * @param presenter - MapbookContract.Presenter
   */
  @Override final public void setPresenter(final MapbookContract.Presenter presenter) {

    mPresenter = presenter;
  }

  /**
   * Show details associated with the mapbook
   * given an Item
   * @param item Item
   */
  @Override final public void populateMapbookLayout(final Item item) {
    final String description = item.getDescription();
    final String name = item.getTitle();

    final TextView txtDescription = (TextView) mRoot.findViewById(R.id.txtDescription);
    txtDescription.setText(description);

    final TextView txtCrdate = (TextView) mRoot.findViewById(R.id.txtCrDate);
    txtCrdate.setText("Created " + ActivityUtils.getDateString(item.getCreated()));

    final TextView txtName = (TextView) mRoot.findViewById(R.id.txtTitle);
    txtName.setText(name);
  }

  /**
   * Set the map thumbnail given an array of bytes
   * @param bytes [] of bytes
   */
  @Override final public void setThumbnailBitmap(final byte[] bytes) {
    final Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    final ImageView image = (ImageView) mRoot.findViewById(R.id.mapBookThumbnail);
    image.setImageBitmap(bitmap);
  }

  /**
   * Show a Toast message given the message string
   * @param message - String
   */
  @Override final public void showMessage(final String message) {
    Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
  }

  /**
   * Display mapbook size, modified date and map count
   * for mapbook
   * @param size - long representing file size
   * @param modifiedDate - long representing modified file date
   * @param mapCount - int representing count of maps in mapbook
   */
  @Override final public void setMapbookMetatdata(final long size, final long modifiedDate, final int mapCount) {
    final TextView txtCount = (TextView) mRoot.findViewById(R.id.txtMapCount);
    txtCount.setText(""+ mapCount+ " Maps");

    if (size > 0){
      final TextView txtSize = (TextView) mRoot.findViewById(R.id.txtFileSize);
      final long fileSizeInKB = size / 1024;
      // Convert the KB to MegaBytes (1 MB = 1024 KBytes)
      final long fileSizeInMB = fileSizeInKB / 1024;
      txtSize.setText("Size " + fileSizeInMB + " MB");
    }
    if (modifiedDate > 0) {
      final Calendar c = Calendar.getInstance();
      c.setTimeInMillis(modifiedDate);
      final String downloadedDate = ActivityUtils.getDateString(c);
      final TextView txtDowndate = (TextView) mRoot.findViewById(R.id.txtDownloadDate);
      txtDowndate.setText("Last downloaded " + downloadedDate);
    }
  }
  /**
   * Logic for notifying user that mapbook couldn't be found on device
   */
  @Override final public void showMapbookNotFound() {

    final TextView txtDescription = (TextView) mRoot.findViewById(R.id.txtDescription);
    txtDescription.setText("Unable to download the mobile map package");

    final ImageView btnRefresh = (ImageView) mRoot.findViewById(R.id.imageDownloadBtn);
    btnRefresh.setVisibility(View.INVISIBLE);
  }

  /**
   * Assign a list of ArcGIS Maps to the view
   * @param maps - List of ArcGIS map items
   */
  @Override final public void setMaps(final List<ArcGISMap> maps) {
    mapAdapter.setMaps(maps);
    mapAdapter.notifyDataSetChanged();
  }

  /**
   * Logic for initiating an activity dedicated
   * to downloading the mapbook to the
   * given path on the device.
   * @param mmpkFilePath - String representing where on the device the
   *             download should be stored.
   */
  @Override final public void downloadMapbook(final String mmpkFilePath) {
    // Kick off the DownloadActivity
    final Intent intent = new Intent(getActivity(), DownloadActivity.class);
    intent.putExtra(FILE_PATH, mmpkFilePath);
    startActivityForResult(intent, REQUEST_DOWNLOAD);
  }

  /**
   * Toggle the visibility of the download button and text
   * @param display - boolean, true for show, false for hide
   */
  @Override public void toggleDownloadVisibility(final boolean display) {
    final ImageView downloadBtn = (ImageView) getActivity().findViewById(R.id.imageDownloadBtn);
    final TextView txtUpdateFile = (TextView) getActivity().findViewById(R.id.txtUpdate);
    if (display){
      downloadBtn.setVisibility(View.VISIBLE);
      txtUpdateFile.setVisibility(View.VISIBLE);
    }else{
      downloadBtn.setVisibility(View.INVISIBLE);
      txtUpdateFile.setVisibility(View.INVISIBLE);
    }
  }

  /**
   * Set the user name field in the view
   * @param userName - String
   */
  @Override public void setUserName(final String userName) {
    final TextView txtUserName = (TextView) getActivity().findViewById(R.id.txtUserName);
    txtUserName.setText(userName);
  }

  /**
   * Exit the app
   */
  @Override public void exit() {
    getActivity().finish();
  }


  @Override public void setDownloadText(final String text) {
    final TextView downloadMessage = (TextView) getActivity().findViewById(R.id.txtUpdate);
    downloadMessage.setText(text);
  }

  /**
   * Logic for handling results from the returned activity.
   * @param requestCode - int
   * @param resultCode -int
   * @param data - Intent
   */
  @Override
  final public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
    if (requestCode == REQUEST_DOWNLOAD){
      if (resultCode == RESULT_OK){
        // If the result comes back from download successfully, ensure the download image
        // and text are set back to invisible.
        toggleDownloadVisibility(false);
        // Logic is executed in the presenter which is
        // started when this fragment resumes onActivityResult.
        Log.i(TAG, "Retrieved file = " + data.getStringExtra(FILE_PATH));

      }else if (resultCode == RESULT_CANCELED){
        if (data.hasExtra(ERROR_STRING)){
          final String error = data.getStringExtra(ERROR_STRING);
          Toast.makeText(getActivity(),error, Toast.LENGTH_LONG).show();
          //TODO:  Better view needed here for displaying errors
        }
      }
    }
  }

  /**
   * This class listens for broadcasts from the PortalItemUpdateService.
   * If there's a newer version of the mobile map package available,
   * show a download button in the UI.
   */
  private class PortalItemBroadcastReceiver extends BroadcastReceiver {

    // Prevents instantiation TODO: Does this comment make sense?
    private PortalItemBroadcastReceiver(){}

    @Override public void onReceive(final Context context, final Intent intent) {

      /*
       * Handle Intents here.
       */
      if (intent.getExtras() !=  null){
        final long timeUpdated = intent.getLongExtra(getString(R.string.LATEST_DATE),0);
        mPresenter.processBroadcast(timeUpdated);
        Log.i(TAG, "Portal item's modified date in milliseconds " + timeUpdated);
      }
    }
  }

}
