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

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
  private RecyclerView mRecyclerView;
  private ConstraintLayout mRoot = null;
  private MapAdapter mapAdapter = null;
  private static final int REQUEST_DOWNLOAD = 1;
  public static final String FILE_PATH = "mmpk file path";
  private final String TAG = MapbookFragment.class.getSimpleName();

  public MapbookFragment(){}

  public static MapbookFragment newInstance() {

    Bundle args = new Bundle();

    MapbookFragment fragment = new MapbookFragment();
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    mRoot = (ConstraintLayout) container;
    mRecyclerView = (RecyclerView) mRoot.findViewById(R.id.recyclerView) ;
    LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
    mRecyclerView.setLayoutManager( layoutManager);
    mapAdapter = new MapAdapter(new MapAdapter.OnItemClickListener() {
      @Override public void onItemClick(ImageView image, String title, int position) {
        Intent intent = new Intent(getContext(), MapActivity.class);
       // intent.putExtra(MainActivity.FILE_PATH, mmpkFilePath);
        intent.putExtra("INDEX", position);
        intent.putExtra("TITLE", title);
        startActivity(intent);
      }
    });
    mRecyclerView.setAdapter(mapAdapter);
    layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);

    return null;
  }

  @Override
  public void onResume() {
    super.onResume();
    mPresenter.start();
  }

  @Override public void setPresenter(MapbookContract.Presenter presenter) {

    mPresenter = presenter;
  }

  /**
   * Populate the layout with item details
   * @param item - Portal Item
   */
  @Override public void populateMapbookLayout(Item item) {
    final String description = item.getDescription();
    final String name = item.getTitle();

    final TextView txtDescription = (TextView) mRoot.findViewById(R.id.txtDescription);
    txtDescription.setText(description);

    final TextView txtCrdate = (TextView) mRoot.findViewById(R.id.txtCrDate);
    txtCrdate.setText(ActivityUtils.getDateString(item.getCreated()));

    final TextView txtName = (TextView) mRoot.findViewById(R.id.txtTitle);
    txtName.setText(name);
  }

  @Override public void setThumbnailBitmap(byte[] bytes) {
    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    ImageView image = (ImageView) mRoot.findViewById(R.id.mapBookThumbnail);
    image.setImageBitmap(bitmap);
  }

  @Override public void showMessage(String message) {
    Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
  }

  @Override public void setMapbookMetatdata(long size, long modifiedDate, int mapCount) {
    final TextView txtCount = (TextView) mRoot.findViewById(R.id.txtMapCount);
    txtCount.setText(""+ mapCount+ " Maps");

    if (size > 0){
      TextView txtSize = (TextView) mRoot.findViewById(R.id.txtFileSize);
      final long fileSizeInKB = size / 1024;
      // Convert the KB to MegaBytes (1 MB = 1024 KBytes)
      final long fileSizeInMB = fileSizeInKB / 1024;
      txtSize.setText(fileSizeInMB + " MB");
    }
    if (modifiedDate > 0) {
      final Calendar c = Calendar.getInstance();
      c.setTimeInMillis(modifiedDate);
      final String downloadedDate = ActivityUtils.getDateString(c);
      final TextView txtDowndate = (TextView) mRoot.findViewById(R.id.txtDownloadDate);
      txtDowndate.setText(downloadedDate);
    }
  }

  @Override public void showMapbookNotFound() {

    final TextView txtDescription = (TextView) mRoot.findViewById(R.id.txtDescription);
    txtDescription.setText("Unable to download the mobile map package");

    final ImageView btnRefresh = (ImageView) mRoot.findViewById(R.id.imageView2);
    btnRefresh.setVisibility(View.INVISIBLE);
  }

  @Override public void setMaps(List<ArcGISMap> maps) {
    mapAdapter.setMaps(maps);
    mapAdapter.notifyDataSetChanged();
  }

  @Override public void downloadMapbook(String mmpkFilePath) {
    // Kick off the DownloadActivity
    Intent intent = new Intent(getActivity(), DownloadActivity.class);
    intent.putExtra(FILE_PATH, mmpkFilePath);
    startActivityForResult(intent, REQUEST_DOWNLOAD);
  }
  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_DOWNLOAD){
      if (resultCode == RESULT_OK){
        String fileName = data.getStringExtra(FILE_PATH);
        Log.i(TAG, "Retrieved file = " + data.getStringExtra(FILE_PATH));

      }else if (resultCode == RESULT_CANCELED){
        if (data.hasExtra(ERROR_STRING)){
          String error = data.getStringExtra(ERROR_STRING);
          Toast.makeText(getActivity(),error, Toast.LENGTH_LONG).show();
          final TextView errorText = (TextView) mRoot.findViewById(R.id.txtError);
          errorText.setText("There was a problem downloading the mapbook");
          errorText.setVisibility(View.VISIBLE);
        }

      }
    }
  }

}
