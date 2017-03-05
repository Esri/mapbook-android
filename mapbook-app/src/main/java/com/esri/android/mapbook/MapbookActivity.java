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

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import android.widget.TextView;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Item;
import com.esri.arcgisruntime.mapping.MobileMapPackage;

import java.io.File;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class MapbookActivity extends AppCompatActivity {

  private long mapbookSize = 0;
  private long mapbookModified = 0;
  private List<ArcGISMap> maps = new ArrayList<>();
  private RecyclerView mMapView;
  private Activity activity;
  private MapAdapter mapAdapter;
  private String mmpkFilePath = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_main);
    final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    if (toolbar != null){
      setSupportActionBar(toolbar);
      final ActionBar actionBar = (this).getSupportActionBar();
      if (actionBar != null){
        actionBar.setTitle(R.string.title);
      }
      toolbar.setNavigationIcon(null);
    }

    mMapView = (RecyclerView) findViewById(R.id.recyclerView) ;
    LinearLayoutManager layoutManager = new LinearLayoutManager(this);
    mMapView.setLayoutManager( layoutManager);
    mapAdapter = new MapAdapter();
    mMapView.setAdapter(mapAdapter);
    layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);

    mmpkFilePath =  getIntent().getStringExtra(MainActivity.FILE_PATH);

    activity = this;

    if (mmpkFilePath != null){
      // Get the file size and date
      final File f = new File(mmpkFilePath);
      mapbookSize = f.length();
      mapbookModified = f.lastModified();

      loadMapBookThumbnails(mmpkFilePath);
    }
  }

  /**
   * Load the mobile map package
   * @param path - String representing file path of mobile map package
   */
  private void loadMapBookThumbnails(String path){
    final MobileMapPackage mmp = new MobileMapPackage(path);

    mmp.addDoneLoadingListener(new Runnable() {
      @Override
      public void run() {
        if (mmp.getLoadStatus() == LoadStatus.LOADED) {
          maps = mmp.getMaps();
          mapAdapter.setMaps(maps);
          mapAdapter.notifyDataSetChanged();

          Item item = mmp.getItem();
          populateUI(item);

          final byte[] thumbnailData = item.getThumbnailData();
          if (thumbnailData != null && thumbnailData.length > 0) {
            setThumbnailBitmap(thumbnailData);
          }else{
            final ListenableFuture<byte[]> futureThumbnail = item.fetchThumbnailAsync();
            futureThumbnail.addDoneListener(new Runnable() {
              @Override public void run() {
                try {
                  final byte[] itemThumbnailData = futureThumbnail.get();
                  setThumbnailBitmap(itemThumbnailData);
                } catch (InterruptedException e) {
                  e.printStackTrace();
                } catch (ExecutionException e) {
                  e.printStackTrace();
                }
              }
            });
          }

        } else {
          Log.e("MapbookActivity", "There was a problem loading the map " + mmp.getLoadStatus().name());
          Throwable err = mmp.getLoadError();
          err.printStackTrace();
          Log.i("MapbookActivity", err.getMessage());
        }
      }
    });
    mmp.loadAsync();
  }

  /**
   * Populate the layout with item details
   * @param item - Portal Item
   */
  private void populateUI(final Item item){
    final String description = item.getDescription();
    final String name = item.getTitle();

    final TextView txtDescription = (TextView) findViewById(R.id.txtDescription);
    txtDescription.setText(description);

    final TextView txtCrdate = (TextView) findViewById(R.id.txtCrDate);
    txtCrdate.setText(getDateString(item.getCreated()));

    final TextView txtName = (TextView) findViewById(R.id.txtTitle);
    txtName.setText(name);

    final TextView txtCount = (TextView) findViewById(R.id.txtMapCount);
    if (maps != null){
      txtCount.setText(""+ maps.size() + " Maps");
    }
    if (mapbookSize > 0){
      TextView txtSize = (TextView) findViewById(R.id.txtFileSize);
      final long fileSizeInKB = mapbookSize / 1024;
      // Convert the KB to MegaBytes (1 MB = 1024 KBytes)
      final long fileSizeInMB = fileSizeInKB / 1024;
      txtSize.setText(fileSizeInMB + " MB");
    }
    if (mapbookModified > 0){
      final Calendar c = Calendar.getInstance();
      c.setTimeInMillis(mapbookModified);
      final String downloadedDate =  getDateString(c);
      final TextView txtDowndate = (TextView) findViewById(R.id.txtDownloadDate);
      txtDowndate.setText(downloadedDate);
    }
  }

  /**
   * Set the bitmap for the map package using a thumbnail
   * @param data - byte[] containing thumbnail data
   */
  private void setThumbnailBitmap(byte[] data){
    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
    ImageView image = (ImageView) findViewById(R.id.mapBookThumbnail);
    image.setImageBitmap(bitmap);
  }

  /**
   * Retrun a date string for given Calendar object
   * @param calendar - Calendar
   * @return String with date format YEAR/MONTH/DAY
   */
  private String getDateString(Calendar calendar){

    int year = calendar.get(Calendar.YEAR) ;
    int month = calendar.get(Calendar.MONTH);
    int day = calendar.get(Calendar.DAY_OF_MONTH);

    return String.format("%4d/%02d/%02d",  year, month+1, day);
  }


  public class MapAdapter extends RecyclerView.Adapter<RecycleViewHolder>{

    private List<ArcGISMap> maps = Collections.emptyList();

    public MapAdapter(List<ArcGISMap> arcGISMaps){
      maps = arcGISMaps;
    }

    public MapAdapter(){}

    @Override public RecycleViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
      View itemView = LayoutInflater.
          from(viewGroup.getContext()).
          inflate(R.layout.card_view, viewGroup, false);
      return new RecycleViewHolder(itemView);
    }

    @Override public void onBindViewHolder(final RecycleViewHolder holder, final int position) {
      holder.mapName.setText("Map "+ (position+1));
      final ArcGISMap map = maps.get(position);
      map.addDoneLoadingListener(new Runnable() {
        @Override public void run() {
          final Item i = map.getItem();
          Log.i("MapbookActivity", "Snippet " + i.getSnippet() );
          Log.i("MapbookActivity", "Description " + i.getDescription()  );
          final String title = i.getTitle();
          holder.mapName.setText(title);
//          final String description = i.getDescription();
//          final String extractedDescription = description.replaceAll("<[^>]*>", "");
//          Log.i("MapbookActivity", "Description " + extractedDescription );
//          holder.description.setText(extractedDescription);
          holder.snippet.setText(i.getSnippet());
          String dateCreated = getDateString(i.getCreated());
          Log.i("MapbookActivity", "Date created " + dateCreated );
          holder.mapCreateDate.setText("Created " + dateCreated);
          if (i != null){
            final ListenableFuture<byte[]> future = i.fetchThumbnailAsync();
            future.addDoneListener(new Runnable() {
              @Override public void run() {
                try {
                  byte[] t = future.get();
                  Log.i("MapbookActivity", "Length of bytes " + t.length);
                  Bitmap bitmap = BitmapFactory.decodeByteArray(t, 0, t.length);

                  holder.mapView.setImageBitmap(bitmap);
                  holder.mapView.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                      Intent intent = new Intent(activity, MapViewActivity.class);
                      intent.putExtra(MainActivity.FILE_PATH, mmpkFilePath);
                      intent.putExtra("INDEX", position);
                      intent.putExtra("TITLE", title);
                      startActivity(intent);
                    }
                  });

                } catch (Exception e) {
                  e.printStackTrace();

                }
              }
            });
          }


        }
      });
      map.loadAsync();

    }


    @Override public int getItemCount() {
      return maps.size();
    }

    public void setMaps(List<ArcGISMap> mapList){
      maps = mapList;
    }
  }
  public class RecycleViewHolder extends RecyclerView.ViewHolder{

    public final ImageView mapView;
    public final TextView mapName;
    public final TextView snippet;
    public final TextView mapCreateDate;

    public RecycleViewHolder(final View view){
      super(view);
      mapView = (ImageView) view.findViewById(R.id.mapThumbnail);
      mapName = (TextView) view.findViewById(R.id.mapName);
      snippet = (TextView) view.findViewById(R.id.txtMapSnippet);
      mapCreateDate = (TextView) view.findViewById(R.id.txtMapCreateDate);
    }
  }

}
