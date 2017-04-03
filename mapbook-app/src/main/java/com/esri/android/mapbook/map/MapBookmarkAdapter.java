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

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.esri.android.mapbook.R;
import com.esri.arcgisruntime.mapping.Bookmark;
import com.esri.arcgisruntime.mapping.BookmarkList;
import com.esri.arcgisruntime.mapping.Viewpoint;

public class MapBookmarkAdapter extends RecyclerView.Adapter<MapBookmarkAdapter.MapBookmarkViewHolder> {

  public interface OnBookmarkClickListener {
    void onItemClick(Viewpoint viewpoint);
  }

  private BookmarkList mBookmarkList;
  private final OnBookmarkClickListener mListener;

  public MapBookmarkAdapter(OnBookmarkClickListener listener){
    mListener = listener;
  }

  public void setBoomarks(BookmarkList bookmarks){
    mBookmarkList = bookmarks;
  }

  @Override public MapBookmarkViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View itemView = LayoutInflater.
        from(parent.getContext()).
        inflate(R.layout.map_bookmark_view, parent, false);

    return new MapBookmarkViewHolder(itemView);
  }

  @Override public void onBindViewHolder(MapBookmarkViewHolder holder, int position) {
      final Bookmark bookmark= mBookmarkList.get(position);
      String bookmarkName = bookmark.getName();
      holder.mBookmarkTxt.setText(bookmarkName);
      holder.mViewpoint = bookmark.getViewpoint();
      holder.mBookmarkTxt.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View v) {
          mListener.onItemClick(bookmark.getViewpoint());
        }
      });
  }

  @Override public int getItemCount() {
    if (mBookmarkList == null){
      return 0;
    }else{
      return mBookmarkList.size();
    }
  }

  public class MapBookmarkViewHolder extends RecyclerView.ViewHolder{

    final TextView mBookmarkTxt;
    public Viewpoint mViewpoint;

    public MapBookmarkViewHolder(View itemView) {
      super(itemView);
      mBookmarkTxt = (TextView) itemView.findViewById(R.id.txtBookmarkName);
    }
}


}
