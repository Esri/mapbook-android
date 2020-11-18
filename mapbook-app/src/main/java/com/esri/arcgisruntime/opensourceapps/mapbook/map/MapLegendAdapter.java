/*
 *  Copyright 2017 Esri
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * https://www.apache.org/licenses/LICENSE-2.0
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
import android.graphics.Bitmap;
import android.graphics.Color;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.esri.arcgisruntime.opensourceapps.mapbook.R;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.layers.LegendInfo;
import com.esri.arcgisruntime.symbology.Symbol;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MapLegendAdapter extends RecyclerView.Adapter<MapLegendAdapter.MapLegendViewHodler> {

  private List<LegendInfo> mLegendInfoList = new ArrayList<>();
  private final Context mContext;
  private final String TAG = MapLegendAdapter.class.getSimpleName();

  public MapLegendAdapter(final Context context){
    mContext = context;

  }

  /**
   * Set the data for this adapter
   * @param legendInfo List<LegendInfo></LegendInfo>
   */
  public void setLegendInfo(final List<LegendInfo> legendInfo){
    mLegendInfoList = legendInfo;
  }

  /**
   *This method calls onCreateViewHolder(ViewGroup, int) to create a new RecyclerView.ViewHolder
   * and initializes some private fields to be used by RecyclerView.
   * @param parent - ViewGroup
   * @param viewType - int
   * @return MapLayerViewHolder
   */
  @Override public MapLegendViewHodler onCreateViewHolder(final ViewGroup parent, final int viewType) {
    final View itemView = LayoutInflater.
        from(parent.getContext()).
        inflate(R.layout.legend_view, parent, false);

    return new MapLegendViewHodler(itemView);
  }

  /**
   * Called by RecyclerView to display the data at the specified position.
   * @param holder RecycleViewHolder
   * @param position - int
   */
  @Override public void onBindViewHolder(final MapLegendViewHodler holder, final int position) {
    final LegendInfo legendInfo = mLegendInfoList.get(position);
    holder.legendName.setText(legendInfo.getName());
    final Symbol symbol = legendInfo.getSymbol();

    final TypedValue a = new TypedValue();
    final int color;

    // Match the background color of the bitmap to the background of the theme.
    mContext.getTheme().resolveAttribute(android.R.attr.colorBackground, a, true);
    color =
        a.type >= TypedValue.TYPE_FIRST_COLOR_INT && a.type <= TypedValue.TYPE_LAST_COLOR_INT ? a.data : Color.WHITE;

    final ListenableFuture<Bitmap> future = symbol.createSwatchAsync(mContext, color);
    future.addDoneListener(new Runnable() {
      @Override public void run() {
        try {
          final Bitmap bitmap = future.get();

          holder.legendSymbol.setImageBitmap(bitmap);
        } catch (ExecutionException | InterruptedException e) {
          Log.e(TAG, e.getMessage());
        }
      }
    });

  }
  /**
   * Returns the total number of items in the data set held by the adapter.
   * @return int
   */
  @Override public int getItemCount() {
    return mLegendInfoList.size();
  }

  public class MapLegendViewHodler extends RecyclerView.ViewHolder{

    final ImageView legendSymbol;
    final TextView legendName;

    public MapLegendViewHodler(final View itemView) {
      super(itemView);
      legendSymbol = (ImageView) itemView.findViewById(R.id.imgSymbol);
      legendName = (TextView) itemView.findViewById(R.id.txtLegend);
    }
  }
}
