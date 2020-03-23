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

import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.esri.arcgisruntime.opensourceapps.mapbook.R;
import com.esri.arcgisruntime.mapping.Bookmark;
import com.esri.arcgisruntime.mapping.BookmarkList;
import com.esri.arcgisruntime.mapping.Viewpoint;

/**
 * The adapter used by the recycler view to display bookmarks in the map
 */
public class MapBookmarkAdapter extends RecyclerView.Adapter<MapBookmarkAdapter.MapBookmarkViewHolder> {

  /**
   * An interface for defining what happens when a bookmark is tapped
   */

  public interface OnBookmarkClickListener {
    void onItemClick(Viewpoint viewpoint);
  }

  private BookmarkList mBookmarkList;
  private final OnBookmarkClickListener mListener;

  /**
   * Constructor relies on a implementation of the OnBookmarkClickListener
   * for handling logic when map bookmarks are tapped.
   * @param listener - OnBookmarkClickListener
   */
  public MapBookmarkAdapter(final OnBookmarkClickListener listener){
    mListener = listener;
  }

  /**
   * Set the data items for this adapter
   * @param bookmarks - BookmarkList
   */
  public void setBoomarks(final BookmarkList bookmarks){
    mBookmarkList = bookmarks;
  }

  /**
   *This method calls onCreateViewHolder(ViewGroup, int) to create a new RecyclerView.ViewHolder
   * and initializes some private fields to be used by RecyclerView.
   * @param parent - ViewGroup
   * @param viewType - int
   * @return MapBookmarkViewHolder
   */
  @Override public MapBookmarkViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
    final View itemView = LayoutInflater.
        from(parent.getContext()).
        inflate(R.layout.map_bookmark_view, parent, false);

    return new MapBookmarkViewHolder(itemView);
  }

  /**
   * Called by RecyclerView to display the data at the specified position.
   * @param holder RecycleViewHolder
   * @param position - int
   */
  @Override public void onBindViewHolder(final MapBookmarkViewHolder holder, final int position) {
      final Bookmark bookmark= mBookmarkList.get(position);
      final String bookmarkName = bookmark.getName();
      holder.mBookmarkTxt.setText(bookmarkName);
      holder.mViewpoint = bookmark.getViewpoint();
      holder.mBookmarkTxt.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(final View v) {
          mListener.onItemClick(bookmark.getViewpoint());
        }
      });
  }

  /**
   * Returns the total number of items in the data set held by the adapter.
   * @return int
   */
  @Override public int getItemCount() {
    return mBookmarkList == null ? 0 : mBookmarkList.size();
  }

  public class MapBookmarkViewHolder extends RecyclerView.ViewHolder{

    final TextView mBookmarkTxt;
    public Viewpoint mViewpoint;

    public MapBookmarkViewHolder(final View itemView) {
      super(itemView);
      mBookmarkTxt = (TextView) itemView.findViewById(R.id.txtBookmarkName);
    }
}


}
