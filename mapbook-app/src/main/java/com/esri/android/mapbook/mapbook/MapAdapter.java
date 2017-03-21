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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.esri.android.mapbook.R;
import com.esri.android.mapbook.util.ActivityUtils;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.BookmarkList;
import com.esri.arcgisruntime.mapping.Item;

import java.util.Collections;
import java.util.List;

/**
 * The adapter used by the recycler view to display maps in the mapbook
 */

public class MapAdapter extends RecyclerView.Adapter<MapAdapter.RecycleViewHolder>{

  public interface OnItemClickListener{
    void onItemClick(ImageView image, String mapTitle, int position);
  }

  private  List<ArcGISMap> maps = Collections.emptyList();
  private final OnItemClickListener mListener;

  public MapAdapter( OnItemClickListener listener){

    mListener = listener;
  }


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
        if (i != null){
          holder.bind(i, mListener);
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
    public void bind (final Item item, final OnItemClickListener listener){
      final String title = item.getTitle();
      mapName.setText(title);
      snippet.setText(item.getSnippet());
     // final String description = item.getDescription();
    //  final String extractedDescription = description.replaceAll("<[^>]*>", "");

      String dateCreated = ActivityUtils.getDateString(item.getCreated());
      mapCreateDate.setText(dateCreated);
      if (item != null){
        final ListenableFuture<byte[]> future = item.fetchThumbnailAsync();
        future.addDoneListener(new Runnable() {
          @Override public void run() {
            try {
              byte[] t = future.get();

              final Bitmap bitmap = BitmapFactory.decodeByteArray(t, 0, t.length);
              mapView.setImageBitmap(bitmap);

              mapView.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                  mListener.onItemClick(mapView, title, getAdapterPosition());
                }
              });

            } catch (Exception e) {
              e.printStackTrace();


            }
          }
        });
      }
    }

  }
}
