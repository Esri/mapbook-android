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

package com.esri.arcgisruntime.opensourceapps.mapbook.map;

import android.content.Context;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import com.esri.arcgisruntime.opensourceapps.mapbook.R;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.layers.Layer;
import com.esri.arcgisruntime.layers.LegendInfo;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Adapter used in the TOC (Table of Contents that display layers in map)
 */

public class MapLayerAdapter extends RecyclerView.Adapter<MapLayerAdapter.MapLayerViewHolder> {

  private List<Layer> mLayers ;
  private final Context mContext;
  private final String TAG = MapLayerAdapter.class.getSimpleName();

  public MapLayerAdapter(final Context context){
    mContext = context;
  }

  /**
   * Set the data for this adapter
   * @param layers - List
   */
  public void setLayerList(final List layers){

    mLayers = layers;

  }
  /**
   *This method calls onCreateViewHolder(ViewGroup, int) to create a new RecyclerView.ViewHolder
   * and initializes some private fields to be used by RecyclerView.
   * @param viewGroup - ViewGroup
   * @param i - int
   * @return MapLayerViewHolder
   */
  @Override public MapLayerViewHolder onCreateViewHolder(final ViewGroup viewGroup, final int i) {
    final View itemView = LayoutInflater.
        from(viewGroup.getContext()).
        inflate(R.layout.map_layer_view, viewGroup, false);

    return new MapLayerViewHolder(itemView);
  }

  /**
   * Called by RecyclerView to display the data at the specified position.
   * @param holder RecycleViewHolder
   * @param position - int
   */
  @Override public void onBindViewHolder(final MapLayerViewHolder holder, final int position) {
    final Layer layer = mLayers.get(position);
    holder.layerName.setText(layer.getName());

    final boolean layerVisible = (layer.isVisible());
    holder.checkBox.setChecked(layerVisible);
    holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
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
            final List<LegendInfo> legendList = legendInfoFuture.get();
            legendAdapter.setLegendInfo(legendList);
            legendAdapter.notifyDataSetChanged();
          } catch (InterruptedException | ExecutionException e) {
            Log.e(TAG, e.getMessage());
          }
        }
      });

    }
  }
  /**
   * Returns the total number of items in the data set held by the adapter.
   * @return int
   */
  @Override public int getItemCount() {
    return mLayers == null ? 0 : mLayers.size();
  }

  public class MapLayerViewHolder extends RecyclerView.ViewHolder{

    public final TextView layerName;
    public final CheckBox checkBox;
    public final RecyclerView legendItems;

    public MapLayerViewHolder(final View view){
      super(view);
      checkBox = (CheckBox) view.findViewById(R.id.cbLayer) ;
      layerName = (TextView) view.findViewById(R.id.txtLayerName);
      legendItems = (RecyclerView) view.findViewById(R.id.legendRecylerView);
    }
  }
}
