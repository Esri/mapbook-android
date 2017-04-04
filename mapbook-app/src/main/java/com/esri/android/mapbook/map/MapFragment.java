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

import android.app.SearchManager;
import android.content.Context;
import android.database.MatrixCursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.annotation.Nullable;
import android.support.transition.TransitionManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.*;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.widget.Toolbar;
import com.esri.android.mapbook.R;
import com.esri.android.mapbook.data.Entry;
import com.esri.android.mapbook.data.FeatureContent;
import com.esri.android.mapbook.mapbook.MapbookAdapter;
import com.esri.android.mapbook.mapbook.MapbookFragment;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.layers.Layer;
import com.esri.arcgisruntime.layers.LegendInfo;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.BookmarkList;
import com.esri.arcgisruntime.mapping.LayerList;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.*;
import com.esri.arcgisruntime.symbology.PictureMarkerSymbol;
import com.esri.arcgisruntime.tasks.geocode.SuggestResult;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MapFragment extends Fragment implements MapContract.View {

  private RecyclerView mLayerRecyclerView = null;
  private RecyclerView mBookmarkRecyclerView = null;
  private MapLayerAdapter mContentAdapter = null;
  private MapBookmarkAdapter mBookmarkAdapter = null;
  MapContract.Presenter mPresenter;
  private MapView mMapView = null;
  private SearchView mSearchview;
  private LinearLayout mRoot = null;
  private GraphicsOverlay mGraphicsOverlay = null;
  private static final String TAG = MapbookFragment.class.getSimpleName();
  private PictureMarkerSymbol mPinSourceSymbol;

  private static final String COLUMN_NAME_ADDRESS = "address";
  private static final String COLUMN_NAME_X = "x";
  private static final String COLUMN_NAME_Y = "y";
  private MatrixCursor mSuggestionCursor;
  private int mMapIndex = -1;
  private String mPath = null;
  private Callout mCallout = null;
  private int currentLayoutId = 0;
  private String mMapTitle = null;

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

    // View for map layers (visibility set to "gone" by default)
    mLayerRecyclerView = (RecyclerView) mRoot.findViewById(R.id.mapLayerRecyclerView);
    mLayerRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
    mContentAdapter = new MapLayerAdapter(getContext());
    mLayerRecyclerView.setAdapter(mContentAdapter);

    // View for map bookmarks (visibility set to "gone" by default)
    mBookmarkRecyclerView = (RecyclerView) mRoot.findViewById(R.id.mapBookmarkRecyclerView) ;
    mBookmarkRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
    mBookmarkAdapter = new MapBookmarkAdapter(new MapBookmarkAdapter.OnBookmarkClickListener() {
      @Override public void onItemClick(Viewpoint viewpoint) {
        mMapView.setViewpoint(viewpoint);
      }
    });
    mBookmarkRecyclerView.setAdapter(mBookmarkAdapter);

    mMapView= (MapView) mRoot.findViewById(R.id.mapView);

    mMapView.setOnTouchListener(new MapTouchListener(getActivity().getApplicationContext(), mMapView));

    // Enable fragment to have options menu
    setHasOptionsMenu(true);


    // Calling activity should pass the index and map title
    Bundle args = getArguments();
    if (args.containsKey("INDEX")  && args.containsKey(MapbookFragment.FILE_PATH) && args.containsKey("TITLE")){
      mPath = args.getString(MapbookFragment.FILE_PATH);
      mMapIndex = args.getInt("INDEX");
      mMapTitle = args.getString("TITLE");

    }else{
      // Otherwise, finish the activity
      getActivity().finish();
    }

    return null;
  }

  @Override
  public void onActivityCreated (Bundle savedInstanceState){
    super.onActivityCreated(savedInstanceState);
    final ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
    if (actionBar != null && mMapTitle != null){
      actionBar.setTitle(mMapTitle);
    }
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
      toggleBookmarkList();
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
    mBookmarkRecyclerView.setVisibility(View.GONE);
    if (mLayerRecyclerView.getVisibility() == android.view.View.GONE){
      ViewGroup.LayoutParams params = mLayerRecyclerView.getLayoutParams();
      mLayerRecyclerView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT, 4));
      mLayerRecyclerView.requestLayout();
      mLayerRecyclerView.setVisibility(android.view.View.VISIBLE);
    }else{
      mLayerRecyclerView.setVisibility(android.view.View.GONE);
      mLayerRecyclerView.setLayoutParams(new LinearLayout.LayoutParams(0,
          ViewGroup.LayoutParams.MATCH_PARENT, 4));
      mLayerRecyclerView.requestLayout();
    }
  }

  /**
   * Show/hide the list of bookmarks
   * by adjusting the layout
   */
  private void toggleBookmarkList() {

    //TODO This needs to be smoother.  Show/hide behavior isn't smooth enough.
    LinearLayout transitionsContainer = mRoot;
    TransitionManager.beginDelayedTransition(transitionsContainer);
    mLayerRecyclerView.setVisibility(View.GONE);
    if (mBookmarkRecyclerView.getVisibility() == android.view.View.GONE){
      ViewGroup.LayoutParams params = mBookmarkRecyclerView.getLayoutParams();
      mBookmarkRecyclerView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT, 4));
      mBookmarkRecyclerView.requestLayout();
      mBookmarkRecyclerView.setVisibility(android.view.View.VISIBLE);
    }else{
      mBookmarkRecyclerView.setVisibility(android.view.View.GONE);
      mBookmarkRecyclerView.setLayoutParams(new LinearLayout.LayoutParams(0,
          ViewGroup.LayoutParams.MATCH_PARENT, 4));
      mBookmarkRecyclerView.requestLayout();
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


    // Set up layers and bookmarks
    List<Layer> layerList = map.getOperationalLayers();
    mContentAdapter.setLayerList(layerList);
    BookmarkList bookmarks = map.getBookmarks();
    mBookmarkAdapter.setBoomarks(bookmarks);
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

    mCallout = mMapView.getCallout();

    //Don't show a callout if we have no view info
    if (calloutContent == null){
      mCallout.dismiss();
      return;
    }

    Callout.Style style = new Callout.Style(getContext());
    style.setMinWidth(350);
    style.setMaxWidth(350);
    style.setLeaderPosition(Callout.Style.LeaderPosition.UPPER_MIDDLE);
    mCallout.setLocation(resultPoint);
    mCallout.setContent(calloutContent);
    mCallout.setShowOptions(new Callout.ShowOptions(true,false,false));
    mCallout.setStyle(style);

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
    Toast.makeText(getActivity().getApplicationContext(), message,Toast.LENGTH_SHORT).show();
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

  private void clearSelections(){
    LayerList layers = mMapView.getMap().getOperationalLayers();
    for (Layer layer : layers){
      if (layer instanceof  FeatureLayer){
        ((FeatureLayer) layer).clearSelection();
      }
    }
  }
  /**
   * Build the view for the callout
   * @param featureContents
   * @return The constructed view
   */
  private View buildContentView(final List<FeatureContent> featureContents){
    currentLayoutId = 0;
    LayoutInflater inflater = (LayoutInflater)getActivity().getApplicationContext().getSystemService
        (Context.LAYOUT_INFLATER_SERVICE);
    View calloutView =  inflater.inflate(R.layout.callout_content,null);
    final LinearLayout mainLayout = (LinearLayout) calloutView.findViewById(R.id.calloutLinearLayout);


    // Set popup count
    final TextView txtPopupCount = (TextView) calloutView.findViewById(R.id.popupCount) ;
    txtPopupCount.setText("1 of " + featureContents.size());
    txtPopupCount.setTextColor(Color.BLACK);

    int layoutCt = 0;
    final List<LinearLayout> calloutLayouts = new ArrayList<>();
    final TextView layerTitle = (TextView) calloutView.findViewById(R.id.layerName);
    FeatureContent featureContent = featureContents.get(0);
    layerTitle.setText(featureContent.getLayerName());

    // Highlight the first feature
    featureContent.getFeatureLayer().selectFeature(featureContent.getFeature());

    layerTitle.setTextColor(Color.BLACK);
    layerTitle.setTypeface(null, Typeface.BOLD);


    for (FeatureContent content : featureContents){
      LinearLayout linearLayout = new LinearLayout(getActivity());
      linearLayout.setOrientation(LinearLayout.VERTICAL);
      LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(600,
          ViewGroup.LayoutParams.WRAP_CONTENT);
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
        processButtonEvent(mainLayout, featureContents, calloutLayouts, layerTitle, txtPopupCount, btnNext, btnPrev, "prev");
      }
    });


    btnNext.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        processButtonEvent(mainLayout, featureContents, calloutLayouts, layerTitle, txtPopupCount, btnNext, btnPrev, "next");
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

  /**
   * Toggle visibility of previous and next buttons based on currently viewed feature info
   * @param mainLayout  LinearLayout
   * @param featureContents List of FeatureContents
   * @param calloutLayouts List of LinearLayouts
   * @param layerTitle TextView representing title of layer where feature is located
   * @param txtPopupCount TextView representing count of features identified
   * @param btnNext Button for navigating to next feature
   * @param btnPrev Button for navigating to previous feature
   * @param btnName String representing flag for which button to process logic for
   */
  private void processButtonEvent(final LinearLayout mainLayout, final List<FeatureContent> featureContents,
      final List<LinearLayout> calloutLayouts, final TextView layerTitle, final TextView txtPopupCount, final Button btnNext, final Button btnPrev, final String btnName ){

    int currentIndex = getCurrentLayoutId();
    View currentView = mainLayout.findViewById(currentIndex);
    if (btnName.equalsIgnoreCase("next") && currentIndex < featureContents.size() - 1){
      incrementCurrentLayoutId();

    }else if (btnName.equalsIgnoreCase("prev") && currentIndex > 0){
      decrementCurrentLayoutId();
    }

    View newView = calloutLayouts.get(getCurrentLayoutId());
    replaceView(mainLayout, currentView,newView);

    // Reset the layer name in the callout
    FeatureContent activeContent = featureContents.get(getCurrentLayoutId());
    String layerName = activeContent.getLayerName();

    // Clear any selections
    clearSelections();

    // Select the feature
    activeContent.getFeatureLayer().selectFeature(activeContent.getFeature());

    layerTitle.setText(layerName);

    // Reset popup index
    txtPopupCount.setText(getCurrentLayoutId() + 1 + " of " + featureContents.size() );
    // Adjust visual cues for navigating features
    currentIndex = getCurrentLayoutId();
    if (currentIndex < featureContents.size() - 1){
      btnNext.setAlpha(1.0f);
    }else{
      btnNext.setAlpha(0.3f);
    }
    if (currentIndex > 0){
      btnPrev.setAlpha(1.0f);
    }else{
      btnPrev.setAlpha(0.3f);
    }
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
      // clear any previous selections
      clearSelections();

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
              displaySearchResult(clickedLocation, null, false);
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
