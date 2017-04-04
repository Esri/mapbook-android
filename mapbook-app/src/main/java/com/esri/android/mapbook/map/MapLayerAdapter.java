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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import com.esri.android.mapbook.R;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.layers.Layer;
import com.esri.arcgisruntime.layers.LegendInfo;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class MapLayerAdapter extends RecyclerView.Adapter<MapLayerAdapter.MapLayerViewHolder> {

  private List<Layer> mLayers ;
  private Context mContext;
  private final String TAG = MapLayerAdapter.class.getSimpleName();

  public MapLayerAdapter(Context context){
    mContext = context;
  }

  public void setLayerList(List layers){

    mLayers = layers;

  }

  @Override public MapLayerViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
    View itemView = LayoutInflater.
        from(viewGroup.getContext()).
        inflate(R.layout.map_layer_view, viewGroup, false);

    return new MapLayerViewHolder(itemView);
  }

  @Override public void onBindViewHolder(final MapLayerViewHolder holder, final int position) {
    final Layer layer = mLayers.get(position);
    holder.mapContentName.setText(layer.getName());

    boolean layerVisible = (layer.isVisible());
    holder.checkBox.setChecked(layerVisible);
    holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (layer.isVisible()){
          layer.setVisible(false);
        }else{
          layer.setVisible(true);
        }
      }
    });
    final MapLegendAdapter legendAdapter = new MapLegendAdapter(mContext);
    holder.legendItems.setLayoutManager(new LinearLayoutManager (mContext));
    holder.legendItems.setAdapter(legendAdapter);
    // Retrieve any legend info
    if (layer instanceof FeatureLayer) {
      final ListenableFuture<List<LegendInfo>> legendInfoFuture = layer.fetchLegendInfosAsync();
      legendInfoFuture.addDoneListener(new Runnable() {
        @Override public void run() {
          try {
            List<LegendInfo> legendList = legendInfoFuture.get();
            legendAdapter.setLegendInfo(legendList);
            legendAdapter.notifyDataSetChanged();
          } catch (InterruptedException | ExecutionException e) {
            Log.e(TAG, e.getMessage());
          }
        }
      });

    }
  }

  @Override public int getItemCount() {
    if (mLayers == null){
      return 0;
    }else{
      return mLayers.size();
    }
  }

  public class MapLayerViewHolder extends RecyclerView.ViewHolder{

    public final TextView mapContentName;
    public final CheckBox checkBox;
    public final RecyclerView legendItems;

    public MapLayerViewHolder(final View view){
      super(view);
      checkBox = (CheckBox) view.findViewById(R.id.cbLayer) ;
      mapContentName = (TextView) view.findViewById(R.id.txtMapContentName);
      legendItems = (RecyclerView) view.findViewById(R.id.legendRecylerView);
    }
  }
}
