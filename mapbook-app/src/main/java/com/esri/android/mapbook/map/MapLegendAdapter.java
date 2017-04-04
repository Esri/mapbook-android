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
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.esri.android.mapbook.R;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.layers.LegendInfo;
import com.esri.arcgisruntime.symbology.Symbol;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MapLegendAdapter extends RecyclerView.Adapter<MapLegendAdapter.MapLegendViewHodler> {

  private List<LegendInfo> mLegendInfoList = new ArrayList<>();
  private Context mContext;
  private final String TAG = MapLegendAdapter.class.getSimpleName();

  public MapLegendAdapter(Context context){
    mContext = context;

  }

  public void setLegendInfo(List<LegendInfo> legendInfo){
    mLegendInfoList = legendInfo;
  }

  @Override public MapLegendViewHodler onCreateViewHolder(ViewGroup parent, int viewType) {
    View itemView = LayoutInflater.
        from(parent.getContext()).
        inflate(R.layout.legend_view, parent, false);

    return new MapLegendViewHodler(itemView);
  }

  @Override public void onBindViewHolder(final MapLegendViewHodler holder, int position) {
    LegendInfo legendInfo = mLegendInfoList.get(position);
    holder.legendName.setText(legendInfo.getName());
    Symbol symbol = legendInfo.getSymbol();

    TypedValue a = new TypedValue();
    int color;

    // Match the background color of the bitmap to the background of the theme.
    mContext.getTheme().resolveAttribute(android.R.attr.colorBackground, a, true);
    if (a.type >= TypedValue.TYPE_FIRST_COLOR_INT && a.type <= TypedValue.TYPE_LAST_COLOR_INT) {
      // colorBackground is a color
      color = a.data;
    }else{
      color = Color.WHITE;
    }

    final ListenableFuture<Bitmap> future = symbol.createSwatchAsync(mContext, color);
    future.addDoneListener(new Runnable() {
      @Override public void run() {
        try {
          Bitmap bitmap = future.get();

          holder.legendSymbol.setImageBitmap(bitmap);
        } catch (ExecutionException | InterruptedException e) {
          Log.e(TAG, e.getMessage());
        }
      }
    });

  }

  @Override public int getItemCount() {
    return mLegendInfoList.size();
  }

  public class MapLegendViewHodler extends RecyclerView.ViewHolder{

    final ImageView legendSymbol;
    final TextView legendName;

    public MapLegendViewHodler(View itemView) {
      super(itemView);
      legendSymbol = (ImageView) itemView.findViewById(R.id.imgSymbol);
      legendName = (TextView) itemView.findViewById(R.id.txtLegend);
    }
  }
}
