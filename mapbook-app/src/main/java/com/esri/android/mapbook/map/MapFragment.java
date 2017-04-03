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
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.annotation.Nullable;
import android.support.transition.TransitionManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewGroupCompat;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.Layout;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.esri.android.mapbook.R;
import com.esri.android.mapbook.data.Entry;
import com.esri.android.mapbook.data.FeatureContent;
import com.esri.android.mapbook.mapbook.MapbookFragment;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.layers.Layer;
import com.esri.arcgisruntime.layers.LegendInfo;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.*;
import com.esri.arcgisruntime.symbology.PictureMarkerSymbol;
import com.esri.arcgisruntime.tasks.geocode.SuggestResult;

import java.util.ArrayList;
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
  private int mMapIndex = -1;
  private String mPath = null;
  private Callout mCallout = null;
  private int currentLayoutId = 0;

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

    mMapView.setOnTouchListener(new MapTouchListener(getActivity().getApplicationContext(), mMapView));

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

        if (!mPresenter.hasLocatorTask()){
          return false;
        }else{
          getSuggestions(mMapView.getVisibleArea(), newText);
        }
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
   * Provide a character by character suggestions for the search string
   * within the given area.
   *
   * @param geometry
   *            Geometry representing area to search
   * @param query
   *            String typed so far by the user to fetch the suggestions
   *
   */
  @Override public void getSuggestions(Geometry geometry, String query) {
    if (query == null || query.trim().length() ==0){
      return;
    }
    mPresenter.getSuggestions(geometry, query);
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

    //TODO This needs to be smoother.  Show/hide behavior isn't smooth enough.
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


  @Override public void displaySearchResult(Point resultPoint, View calloutContent, boolean zoomOut) {

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
    //Don't show a callout if we have no info
    if (calloutContent == null){
      return;
    }
    mGraphicPoint = resultPoint;

    mCallout = mMapView.getCallout();

    Callout.Style style = new Callout.Style(getContext());
    style.setMinWidth(350);
    style.setMaxWidth(350);
    style.setLeaderPosition(Callout.Style.LeaderPosition.UPPER_MIDDLE);
    mCallout.setLocation(resultPoint);
    mCallout.setContent(calloutContent);
    mCallout.setShowOptions(new Callout.ShowOptions(true,false,false));
    mCallout.setStyle(style);
//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//      mCallout.getStyle().setBackgroundColor(getActivity().getColor(R.color.colorPrimary));
//    }else{
//      mCallout.getStyle().setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.colorPrimary));
//    }
    mCallout.show();
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

  private View buildContentView(final List<FeatureContent> featureContents){
    currentLayoutId = 0;
    LayoutInflater inflater = (LayoutInflater)getActivity().getApplicationContext().getSystemService
        (Context.LAYOUT_INFLATER_SERVICE);
    View calloutView =  inflater.inflate(R.layout.callout_content,null);
    final LinearLayout mainLayout = (LinearLayout) calloutView.findViewById(R.id.calloutLinearLayout);
    Log.i(TAG, "Main layout width " + mainLayout.getWidth());

    // Set popup count
    final TextView txtPopupCount = (TextView) calloutView.findViewById(R.id.popupCount) ;
    txtPopupCount.setText("1 of " + featureContents.size());
    txtPopupCount.setTextColor(Color.BLACK);

    int layoutCt = 0;
    final List<LinearLayout> calloutLayouts = new ArrayList<>();
    final TextView layerTitle = (TextView) calloutView.findViewById(R.id.layerName);
    layerTitle.setText(featureContents.get(0).getLayerName());

    layerTitle.setTextColor(Color.BLACK);
    layerTitle.setTypeface(null, Typeface.BOLD);


    for (FeatureContent content : featureContents){
      LinearLayout linearLayout = new LinearLayout(getActivity());
      linearLayout.setOrientation(LinearLayout.VERTICAL);
      LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(600,
          ViewGroup.LayoutParams.WRAP_CONTENT);
     // layoutParams.setMargins(5, 3, 5, 3);
      linearLayout.setLayoutParams(layoutParams);
      linearLayout.setId(layoutCt);


      List<Entry> entries = content.getEntries();
      for (Entry entry : entries){
        LinearLayout entryLayout = new LinearLayout(getActivity());
        entryLayout.setOrientation(LinearLayout.HORIZONTAL);
        entryLayout.setBackgroundColor(Color.WHITE);
        entryLayout.setLayoutParams(layoutParams);
        linearLayout.addView(entryLayout);
        TextView label = new TextView(getActivity());

        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        labelParams.setMargins(0,0,10,0);
        label.setLayoutParams(labelParams);
        label.setTextColor(Color.BLACK);
        label.setTypeface(null, Typeface.BOLD);
        label.setText(entry.getField());

        TextView value = new TextView(getActivity());

        value.setTextColor(Color.BLACK);
        value.setText(entry.getValue());
        LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT,1);
        value.setLayoutParams(valueParams);
        entryLayout.addView(label);
        entryLayout.addView(value);

      }
      calloutLayouts.add(linearLayout);
      layoutCt = layoutCt +1;
    }
    mainLayout.addView(calloutLayouts.get(0));
    mainLayout.requestLayout();

    final Button btnPrev = (Button) calloutView.findViewById(R.id.btnPrev);
    final Button btnNext = (Button) calloutView.findViewById(R.id.btnNext);
    final Button btnClose = (Button) calloutView.findViewById(R.id.btnClose) ;

    btnPrev.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        int currentIndex = getCurrentLayoutId();
        View currentView = mainLayout.findViewById(currentIndex);
        if (currentIndex > 0){
          decrementCurrentLayoutId();
          View newView = calloutLayouts.get(getCurrentLayoutId());
          replaceView(mainLayout, currentView,newView);
          // Reset the layer name in the callout
          FeatureContent activeContent = featureContents.get(getCurrentLayoutId());
          String layerName = activeContent.getLayerName();
          Log.i(TAG, "Current Layer " +layerName);
          layerTitle.setText(layerName);
          // Reset popup index
          txtPopupCount.setText(getCurrentLayoutId() + 1 + " of " + featureContents.size() );

        }

      }
    });


    btnNext.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        int currentIndex = getCurrentLayoutId();
        View currentView = mainLayout.findViewById(currentIndex);
        if (currentIndex < featureContents.size() - 1){
          incrementCurrentLayoutId();
          View newView = calloutLayouts.get(getCurrentLayoutId());
          replaceView(mainLayout, currentView,newView);

          // Reset the layer name in the callout
          FeatureContent activeContent = featureContents.get(getCurrentLayoutId());
          String layerName = activeContent.getLayerName();
          Log.i(TAG, "Current Layer " +layerName);
          layerTitle.setText(layerName);

          // Reset popup index
          txtPopupCount.setText(getCurrentLayoutId() + 1 + " of " + featureContents.size() );
        }

        Log.i(TAG, "Current layout id " + getCurrentLayoutId());
      }
    });

    btnClose.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        if ( mCallout.isShowing()){
          mCallout.dismiss();
        }
      }
    });
    return  calloutView;
  }

  private int getCurrentLayoutId(){
    return currentLayoutId;
  }
  private void incrementCurrentLayoutId(){
    currentLayoutId = currentLayoutId + 1;
  }
  private void decrementCurrentLayoutId(){
    currentLayoutId = currentLayoutId - 1;
  }
  private void replaceView(LinearLayout mainView, View oldView, View newView){
    mainView.removeView(oldView);
    mainView.addView(newView);
  }

  private class MapTouchListener extends DefaultMapViewOnTouchListener {

    public MapTouchListener(Context context, MapView mapView) {
      super(context, mapView);
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {

      if (mGraphicsOverlay.getGraphics().size() > 0) {
        if (mGraphicsOverlay.getGraphics().get(0).isSelected()) {
          mGraphicsOverlay.getGraphics().get(0).setSelected(false);
        }
      }
      // get the screen point where user tapped
      final android.graphics.Point screenPoint = new android.graphics.Point((int) e.getX(), (int) e.getY());

      final ListenableFuture<List<IdentifyLayerResult>> identifyLayers = mMapView.identifyLayersAsync(screenPoint,5d,true);

      identifyLayers.addDoneListener(new Runnable() {
        @Override
        public void run() {
          try {
            Point clickedLocation = mMapView.screenToLocation(screenPoint);

            List<IdentifyLayerResult> results = identifyLayers.get();
            List<FeatureContent> content = mPresenter.identifyFeatures(clickedLocation, results);
            if (content.isEmpty()){
              showMessage("No features found at that location");
            }else{
              View v = buildContentView(content);
              displaySearchResult(clickedLocation,v, false);
            }

          } catch (InterruptedException | ExecutionException ie) {
            ie.printStackTrace();
          }

        }
      });

      return super.onSingleTapConfirmed(e);
    }
  }
}
