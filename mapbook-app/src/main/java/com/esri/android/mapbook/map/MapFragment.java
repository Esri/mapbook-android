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

import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.database.MatrixCursor;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.annotation.Nullable;
import android.support.transition.TransitionManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.esri.android.mapbook.R;
import com.esri.android.mapbook.mapbook.MapbookFragment;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.layers.Layer;
import com.esri.arcgisruntime.layers.LegendInfo;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.PictureMarkerSymbol;
import com.esri.arcgisruntime.tasks.geocode.SuggestResult;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MapFragment extends Fragment implements MapContract.View {

  private RecyclerView mRecycleMapContentView = null;
  private  MapContentAdapter mContentAdapter = null;
  MapContract.Presenter mPresenter;
  private MapView mMapView = null;
  private SearchView mSearchview;
  private LinearLayout mRoot = null;
  private GraphicsOverlay mGraphicsOverlay = null;
  private static final String TAG = MapbookFragment.class.getSimpleName();
  private ProgressDialog mProgressDialog = null;
  private PictureMarkerSymbol mPinSourceSymbol;
  private String mGraphicPointAddress = null;
  private Point mGraphicPoint = null;
  private static final String COLUMN_NAME_ADDRESS = "address";
  private static final String COLUMN_NAME_X = "x";
  private static final String COLUMN_NAME_Y = "y";
  private MatrixCursor mSuggestionCursor;
  private TextView mCalloutContent = null;
  private int mMapIndex = -1;
  private String mPath = null;

  public MapFragment(){}

  public static MapFragment newInstance(){
    final Bundle args = new Bundle();
    final MapFragment fragment = new MapFragment();
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);


  }
  @Override
  final public android.view.View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    mRoot = (LinearLayout) container;
    mRecycleMapContentView = (RecyclerView) mRoot.findViewById(R.id.mapContentRecyclerView);
    mRecycleMapContentView.setLayoutManager(new LinearLayoutManager(getActivity()));
    mContentAdapter = new MapContentAdapter();
    mRecycleMapContentView.setAdapter(mContentAdapter);

    mMapView= (MapView) mRoot.findViewById(R.id.mapView);

    // Enable fragment to have options menu
    setHasOptionsMenu(true);

    mProgressDialog = new ProgressDialog(getActivity());

    // Calling activity should pass the index and map title
    Bundle args = getArguments();
    if (args.containsKey("INDEX")  && args.containsKey(MapbookFragment.FILE_PATH)){
      mPath = args.getString(MapbookFragment.FILE_PATH);
      mMapIndex = args.getInt("INDEX");

    }else{
      // Otherwise, finish the activity
      getActivity().finish();
    }

    return null;
  }

  /**
   *
   * @param menu
   * @param inflater
   */
  @Override
  public final void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
    getActivity().getMenuInflater().inflate(R.menu.menu, menu);

    // Retrieve the SearchView and plug it into SearchManager
    mSearchview= (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.action_search));
    mSearchview.setQueryHint(getString(R.string.query_hint));
    final SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
    mSearchview.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
    mSearchview.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
      @Override public boolean onQueryTextSubmit(final String query) {

        hideKeyboard();
        mPresenter.geoCodeAddress(query);
        mSearchview.clearFocus();
        return true;
      }

      @Override public boolean onQueryTextChange(final String newText) {

        if (!mPresenter.hasLocatorTask())
          return false;
        mPresenter.getSuggestions(newText);
        return true;
      }
    });

    applySuggestionCursor();

    mSearchview.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
      @Override public boolean onSuggestionSelect(int position) {
        return false;
      }

      @Override public boolean onSuggestionClick(int position) {
        // Obtain the content of the selected suggesting place via
        // cursor
        MatrixCursor cursor = (MatrixCursor) mSearchview.getSuggestionsAdapter().getItem(position);
        int indexColumnSuggestion = cursor.getColumnIndex(COLUMN_NAME_ADDRESS);
        final String address = cursor.getString(indexColumnSuggestion);

        // Find the Location of the suggestion
        mPresenter.geoCodeAddress(address);

        cursor.close();
        mSearchview.setQuery(address,false);
        hideKeyboard();
        return true;
      }
    });
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem menuItem){
    super.onOptionsItemSelected(menuItem);

    if (menuItem.getItemId() == R.id.action_layers){
      toggleLayerList();
    }
    if (menuItem.getItemId() == R.id.bookmarks){

    }
    return true;
  }


  /**
   * Initialize Suggestion Cursor
   */
  private void initSuggestionCursor() {
    String[] cols = { BaseColumns._ID, COLUMN_NAME_ADDRESS, COLUMN_NAME_X, COLUMN_NAME_Y};
    mSuggestionCursor = new MatrixCursor(cols);
  }

  /**
   * Set the suggestion cursor to an Adapter then set it to the search view
   */
  private void applySuggestionCursor() {
    String[] cols = { COLUMN_NAME_ADDRESS};
    int[] to = {R.id.suggestion_item_address};
    SimpleCursorAdapter mSuggestionAdapter = new SimpleCursorAdapter(getActivity().getApplicationContext(),
        R.layout.search_suggestion_item, mSuggestionCursor, cols, to, 0);
    mSearchview.setSuggestionsAdapter(mSuggestionAdapter);
    mSuggestionAdapter.notifyDataSetChanged();
  }

  /**
   * Show/hide the list of map layers
   * by adjusting the layout
   */
  private void toggleLayerList() {

    LinearLayout transitionsContainer = mRoot;
    TransitionManager.beginDelayedTransition(transitionsContainer);
    if (mRecycleMapContentView.getVisibility() == android.view.View.GONE){
      ViewGroup.LayoutParams params = mRecycleMapContentView.getLayoutParams();
      mRecycleMapContentView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT, 4));
      mRecycleMapContentView.requestLayout();
      mRecycleMapContentView.setVisibility(android.view.View.VISIBLE);
    }else{
      mRecycleMapContentView.setVisibility(android.view.View.GONE);
      mRecycleMapContentView.setLayoutParams(new LinearLayout.LayoutParams(0,
          ViewGroup.LayoutParams.MATCH_PARENT, 4));
      mRecycleMapContentView.requestLayout();
    }
  }

  /**
   * Hides soft keyboard
   */
  private void hideKeyboard() {
    mSearchview.clearFocus();
    InputMethodManager inputManager = (InputMethodManager) getActivity().getApplicationContext()
        .getSystemService(Context.INPUT_METHOD_SERVICE);
    inputManager.hideSoftInputFromWindow(mSearchview.getWindowToken(), 0);
  }

  @Override public void setPresenter(MapContract.Presenter presenter) {
    mPresenter = presenter;
  }

  @Override public void showMap(ArcGISMap map) {
    mMapView.setMap(map);
    List<Layer> layerList = map.getOperationalLayers();
    mContentAdapter.setLayerList(layerList);
    Iterator<Layer> iter = layerList.iterator();
    while (iter.hasNext()) {
      Layer layer = iter.next();
      if (layer instanceof FeatureLayer) {
        ((FeatureLayer) layer).setSelectionWidth(3.0d);
        final ListenableFuture<List<LegendInfo>> legendInfoFuture = layer.fetchLegendInfosAsync();
        legendInfoFuture.addDoneListener(new Runnable() {
          @Override public void run() {
            try {
              List<LegendInfo> legendList = legendInfoFuture.get();
              Log.i("MapViewActivity", "Legend list size " + legendList.size());
            } catch (InterruptedException e) {
              e.printStackTrace();
            } catch (ExecutionException e) {
              e.printStackTrace();
            }
          }
        });

      }
    }

  }

  @Override public void displaySearchResult(Point resultPoint, String address, boolean zoomOut) {
    if (mMapView.getCallout().isShowing()) {
      mMapView.getCallout().dismiss();
    }
    //remove any previous graphics/search results
    mGraphicsOverlay.getGraphics().clear();
    // create graphic object for resulting location
    Graphic resultLocGraphic = new Graphic(resultPoint, mPinSourceSymbol);
    // add graphic to location layer
    mGraphicsOverlay.getGraphics().add(resultLocGraphic);

    if (zoomOut){
      // Zoom map to geocode result location
      mMapView.setViewpointAsync(new Viewpoint(resultPoint, 8000), 3);
    }

    mGraphicPoint = resultPoint;
    mGraphicPointAddress = address;
  }


  @Override public void displayBookmarks() {

  }

  @Override public void setUpMap() {
    //TODO Can this initialization be moved to the MapModule?
    if (mGraphicsOverlay ==  null){
      mGraphicsOverlay = new GraphicsOverlay();
      mGraphicsOverlay.setSelectionColor(0xFF00FFFF);
      mMapView.getGraphicsOverlays().add(mGraphicsOverlay);
    }else{
      // Clean anything out
      mGraphicsOverlay.getGraphics().clear();
    }

    BitmapDrawable startDrawable = (BitmapDrawable) ContextCompat.getDrawable(getActivity(), R.drawable.pin);
    mPinSourceSymbol = new PictureMarkerSymbol(startDrawable);
    mPinSourceSymbol.setHeight(90);
    mPinSourceSymbol.setWidth(20);
    mPinSourceSymbol.loadAsync();

    mCalloutContent = new TextView(getActivity().getApplicationContext());
    mCalloutContent.setTextColor(Color.BLACK);
    mCalloutContent.setTextIsSelectable(true);

    mPresenter.loadMap(mPath, mMapIndex);
  }

  @Override public void showSuggestedPlaceNames(List<SuggestResult> suggestions) {
    if (suggestions == null || suggestions.isEmpty()){
      return;
    }
    initSuggestionCursor();
    int key = 0;
    for (SuggestResult result : suggestions) {
      // Add the suggestion results to the cursor
      mSuggestionCursor.addRow(new Object[]{key++, result.getLabel(), "0", "0"});
    }
    applySuggestionCursor();
  }

  @Override public void showMessage(String message) {
    Toast.makeText(getActivity().getApplicationContext(), message,Toast.LENGTH_LONG).show();
  }

  /**
   * Resume map view
   */
  @Override
  public final void onResume(){
    super.onResume();
    if (mMapView != null){
      mMapView.resume();
    }
    mPresenter.start();
  }

  /**
   * Pause map view
   */
  @Override
  public final void onPause() {
    super.onPause();
    if (mMapView != null){
      mMapView.pause();
    }
  }
}
