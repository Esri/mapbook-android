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

import android.app.SearchManager;
import android.content.Context;
import android.database.MatrixCursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.provider.BaseColumns;
import androidx.transition.TransitionManager;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuItemCompat;
import androidx.cursoradapter.widget.SimpleCursorAdapter;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.esri.arcgisruntime.opensourceapps.mapbook.R;
import com.esri.arcgisruntime.opensourceapps.mapbook.data.Entry;
import com.esri.arcgisruntime.opensourceapps.mapbook.data.FeatureContent;
import com.esri.arcgisruntime.opensourceapps.mapbook.mapbook.MapbookFragment;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.layers.Layer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.BookmarkList;
import com.esri.arcgisruntime.mapping.LayerList;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.Callout;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.IdentifyLayerResult;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.symbology.PictureMarkerSymbol;
import com.esri.arcgisruntime.tasks.geocode.SuggestResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * This fragment is responsible for manipulating the map view and
 * displaying map related data. It's the View in the MVP pattern.
 */
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
  private boolean mapLoaded = false; // Don't initialize map items when app returns from background state.

  /**
   * Default constructor
   */
  public MapFragment(){}

  /**
   * Static method that returns a Mapbook Fragment with an
   * empty Bundle that can be configured by caller.
   * @return - MapFragment
   */
  public static MapFragment newInstance(){
    final Bundle args = new Bundle();
    final MapFragment fragment = new MapFragment();
    fragment.setArguments(args);
    return fragment;
  }

  /**
   * When the view is created, unpack the arguments and check for
   * the mobile map package path, map index, and map title.
   * @param inflater -LayoutInflater
   * @param container - ViewGroup
   * @param savedInstanceState - Bundle
   * @return the View
   */
  @Override
  final public android.view.View onCreateView(final LayoutInflater inflater, final ViewGroup container,
      final Bundle savedInstanceState) {
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
      @Override public void onItemClick(final Viewpoint viewpoint) {
        mMapView.setViewpoint(viewpoint);
      }
    });
    mBookmarkRecyclerView.setAdapter(mBookmarkAdapter);

    mMapView= (MapView) mRoot.findViewById(R.id.mapView);

    mMapView.setOnTouchListener(new MapTouchListener(getActivity().getApplicationContext(), mMapView));

    // Enable fragment to have options menu
    setHasOptionsMenu(true);


    // Calling activity should pass the index and map title
    final Bundle args = getArguments();
    if (args.containsKey(getString(R.string.index))  && args.containsKey(MapbookFragment.FILE_PATH) && args.containsKey(getString(R.string.map_title))){
      mapLoaded = false;
      mPath = args.getString(MapbookFragment.FILE_PATH);
      mMapIndex = args.getInt(getString(R.string.index));
      mMapTitle = args.getString(getString(R.string.map_title));

    }else{
      // Otherwise, finish the activity
      getActivity().finish();
    }

    return null;
  }

  /**
   * Set the map title
   * @param savedInstanceState - Bundle
   */
  @Override
  public void onActivityCreated (final Bundle savedInstanceState){
    super.onActivityCreated(savedInstanceState);
    final ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
    if (actionBar != null && mMapTitle != null){
      actionBar.setTitle(mMapTitle);
    }
  }


  /**
   * Set up search view in the menu
   * @param menu - Menu
   * @param inflater - MenuInflater
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

        if (mPresenter.hasLocatorTask()){
          getSuggestions(mMapView.getVisibleArea(), newText);
        }else{
          return false;
        }
        return true;
      }
    });

    applySuggestionCursor();

    mSearchview.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
      @Override public boolean onSuggestionSelect(final int position) {
        return false;
      }

      @Override public boolean onSuggestionClick(final int position) {
        // Obtain the content of the selected suggesting place via
        // cursor
        final MatrixCursor cursor = (MatrixCursor) mSearchview.getSuggestionsAdapter().getItem(position);
        final int indexColumnSuggestion = cursor.getColumnIndex(COLUMN_NAME_ADDRESS);
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

  /**
   * Logic for menu options
   * @param menuItem - MenuItem
   * @return boolean
   */
  @Override
  public boolean onOptionsItemSelected(final MenuItem menuItem){
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
  @Override public void getSuggestions(final Geometry geometry, final String query) {
    if (query == null || query.trim().isEmpty()){
      return;
    }
    mPresenter.getSuggestions(geometry, query);
  }

  /**
   * Initialize Suggestion Cursor
   */
  private void initSuggestionCursor() {
    final String[] cols = { BaseColumns._ID, COLUMN_NAME_ADDRESS, COLUMN_NAME_X, COLUMN_NAME_Y};
    mSuggestionCursor = new MatrixCursor(cols);
  }

  /**
   * Set the suggestion cursor to an Adapter then set it to the search view
   */
  private void applySuggestionCursor() {
    final String[] cols = { COLUMN_NAME_ADDRESS};
    final int[] to = {R.id.suggestion_item_address};
    final SimpleCursorAdapter mSuggestionAdapter = new SimpleCursorAdapter(getActivity().getApplicationContext(),
        R.layout.search_suggestion_item, mSuggestionCursor, cols, to, 0);
    mSearchview.setSuggestionsAdapter(mSuggestionAdapter);
    mSuggestionAdapter.notifyDataSetChanged();
  }

  /**
   * Show/hide the list of map layers
   * by adjusting the layout
   */
  private void toggleLayerList() {

    final LinearLayout transitionsContainer = mRoot;
    TransitionManager.beginDelayedTransition(transitionsContainer);
    mBookmarkRecyclerView.setVisibility(View.GONE);
    if (mLayerRecyclerView.getVisibility() == android.view.View.GONE){
      //final ViewGroup.LayoutParams params = mLayerRecyclerView.getLayoutParams();
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

    final LinearLayout transitionsContainer = mRoot;
    TransitionManager.beginDelayedTransition(transitionsContainer);
    mLayerRecyclerView.setVisibility(View.GONE);
    if (mBookmarkRecyclerView.getVisibility() == android.view.View.GONE){
   //   final ViewGroup.LayoutParams params = mBookmarkRecyclerView.getLayoutParams();
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
    final InputMethodManager inputManager = (InputMethodManager) getActivity().getApplicationContext()
        .getSystemService(Context.INPUT_METHOD_SERVICE);
    inputManager.hideSoftInputFromWindow(mSearchview.getWindowToken(), 0);
  }

  /**
   * Set the presenter for the view
   * @param presenter - MapContract.Presenter
   */
  @Override public void setPresenter(final MapContract.Presenter presenter) {
    mPresenter = presenter;
  }

  /**
   * Display given map in the map view
   * @param map - ArcGIS map
   */
  @Override public void showMap(final ArcGISMap map) {
    mMapView.setMap(map);


    // Set up layers and bookmarks
    final List<Layer> layerList = map.getOperationalLayers();
    mContentAdapter.setLayerList(layerList);
    final BookmarkList bookmarks = map.getBookmarks();
    mBookmarkAdapter.setBoomarks(bookmarks);
  }

  /**
   * Display content for given location.
   * @param resultPoint - Point
   * @param calloutContent - View
   * @param zoomOut - boolean, true to zoom out
   */
  @Override public void displaySearchResult(final Point resultPoint, final View calloutContent, final boolean zoomOut) {

    //remove any previous graphics/search results
    mGraphicsOverlay.getGraphics().clear();

    // create graphic object for resulting location
    final Graphic resultLocGraphic = new Graphic(resultPoint, mPinSourceSymbol);
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

    final Callout.Style style = new Callout.Style(getContext());
    style.setMinWidth(350);
    style.setMaxWidth(350);
    style.setLeaderPosition(Callout.Style.LeaderPosition.UPPER_MIDDLE);
    mCallout.setLocation(resultPoint);
    mCallout.setContent(calloutContent);
    mCallout.setShowOptions(new Callout.ShowOptions(true,false,false));
    mCallout.setStyle(style);

    mCallout.show();
  }

  /**
   * Initialize map-related objects
   */
  @Override public void initializeMapItems() {
    //TODO Question for Dan, should this initialization be moved to the MapModule?
    if (!mapLoaded){
      if (mGraphicsOverlay ==  null){
        mGraphicsOverlay = new GraphicsOverlay();
        mMapView.getSelectionProperties().setColor(0xFF00FFFF);
        mMapView.getGraphicsOverlays().add(mGraphicsOverlay);
      }else{
        // Clean anything out
        mGraphicsOverlay.getGraphics().clear();
      }

      final BitmapDrawable startDrawable = (BitmapDrawable) ContextCompat.getDrawable(getActivity(), R.drawable.pin);
      try {
        mPinSourceSymbol = PictureMarkerSymbol.createAsync(startDrawable).get();
      } catch (InterruptedException | ExecutionException exception) {
        Log.e(TAG, "PictureMarkerSymbol failed to load: " + exception.getMessage());
      }
      mPinSourceSymbol.setHeight(90);
      mPinSourceSymbol.setWidth(20);
      mPinSourceSymbol.loadAsync();

      mPresenter.loadMap(mPath, mMapIndex);
      mapLoaded = true;
    }

  }

  /**
   * Show suggestions
   * @param suggestions List<SuggestResults></SuggestResults>
   */
  @Override public void showSuggestedPlaceNames(final List<SuggestResult> suggestions) {
    if (suggestions == null || suggestions.isEmpty()){
      return;
    }
    initSuggestionCursor();
    int key = 0;
    for (final SuggestResult result : suggestions) {
      // Add the suggestion results to the cursor
      mSuggestionCursor.addRow(new Object[]{key++, result.getLabel(), "0", "0"});
    }
    applySuggestionCursor();
  }
  /**
   * Show a message
   * @param message - String
   */
  @Override public void showMessage(final String message) {
    Toast.makeText(getActivity().getApplicationContext(), message,Toast.LENGTH_SHORT).show();
  }

  /**
   * Resume map view and start presenter
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

  /**
   * Clear selected features in all layers
   */
  private void clearSelections(){
    final LayerList layers = mMapView.getMap().getOperationalLayers();
    for (final Layer layer : layers){
      if (layer instanceof  FeatureLayer){
        final FeatureLayer featureLayer = (FeatureLayer)layer;
        featureLayer.clearSelection();
      }
    }
  }
  /**
   * Build the view for the callout
   * @param featureContents - List of FeatureContent
   * @return The constructed View
   */
  private View buildContentView(final List<FeatureContent> featureContents){
    currentLayoutId = 0;
    final LayoutInflater inflater = (LayoutInflater)getActivity().getApplicationContext().getSystemService
        (Context.LAYOUT_INFLATER_SERVICE);
    final View calloutView =  inflater.inflate(R.layout.callout_content,null);
    final LinearLayout mainLayout = (LinearLayout) calloutView.findViewById(R.id.calloutLinearLayout);


    // Set popup count
    final TextView txtPopupCount = (TextView) calloutView.findViewById(R.id.popupCount) ;
    txtPopupCount.setText("1 of " + featureContents.size());
    txtPopupCount.setTextColor(Color.BLACK);

    int layoutCt = 0;
    final List<LinearLayout> calloutLayouts = new ArrayList<>();
    final TextView layerTitle = (TextView) calloutView.findViewById(R.id.layerName);

    final FeatureContent featureContent = featureContents.get(0);

    //Use the first feature content item to set the title
    layerTitle.setText(featureContent.getLayerName());
    layerTitle.setTextColor(Color.BLACK);
    layerTitle.setTypeface(null, Typeface.BOLD);

    // Select the first feature
    featureContent.getFeatureLayer().selectFeature(featureContent.getFeature());

    // Iterate through the feature content and build out
    // a layout for each  featured content object.

    for (final FeatureContent content : featureContents){

      final LinearLayout linearLayout = new LinearLayout(getActivity());
      linearLayout.setOrientation(LinearLayout.VERTICAL);
      final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(600,
          ViewGroup.LayoutParams.WRAP_CONTENT);
      linearLayout.setLayoutParams(layoutParams);
      linearLayout.setId(layoutCt);

      // Populate each Linear Layout with the column name and value of the feature
      final List<Entry> entries = content.getEntries();
      for (final Entry entry : entries){
        final LinearLayout entryLayout = new LinearLayout(getActivity());
        entryLayout.setOrientation(LinearLayout.HORIZONTAL);
        entryLayout.setBackgroundColor(Color.WHITE);
        entryLayout.setLayoutParams(layoutParams);
        linearLayout.addView(entryLayout);
        final TextView label = new TextView(getActivity());

        final LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        labelParams.setMargins(0,0,10,0);
        label.setLayoutParams(labelParams);
        label.setTextColor(Color.BLACK);
        label.setTypeface(null, Typeface.BOLD);
        label.setText(entry.getField());

        final TextView value = new TextView(getActivity());

        value.setTextColor(Color.BLACK);
        value.setText(entry.getValue());
        final LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT,1);
        value.setLayoutParams(valueParams);
        entryLayout.addView(label);
        entryLayout.addView(value);

      }
      calloutLayouts.add(linearLayout);
      layoutCt = layoutCt +1;
    }
    mainLayout.addView(calloutLayouts.get(0));
    mainLayout.requestLayout();

    // Set up buttons in the callout that navigate across
    // all the feature content for the location
    final Button btnPrev = (Button) calloutView.findViewById(R.id.btnPrev);
    final Button btnNext = (Button) calloutView.findViewById(R.id.btnNext);
    final Button btnClose = (Button) calloutView.findViewById(R.id.btnClose) ;

    // Disable the next button if we only have one FeatureContent item
    if (featureContents.size() == 1){
      btnNext.setAlpha(0.3f);
    }else{
      btnNext.setAlpha(1.0f);
    }

    btnPrev.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(final View v) {
        processButtonEvent(mainLayout, featureContents, calloutLayouts, layerTitle, txtPopupCount, btnNext, btnPrev, "prev");
      }
    });


    btnNext.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(final View v) {
        processButtonEvent(mainLayout, featureContents, calloutLayouts, layerTitle, txtPopupCount, btnNext, btnPrev, "next");
      }
    });

    btnClose.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(final View v) {
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
    final View currentView = mainLayout.findViewById(currentIndex);
    if (btnName.equalsIgnoreCase("next") && currentIndex < featureContents.size() - 1){
      incrementCurrentLayoutId();

    }else if (btnName.equalsIgnoreCase("prev") && currentIndex > 0){
      decrementCurrentLayoutId();
    }

    final View newView = calloutLayouts.get(getCurrentLayoutId());
    replaceView(mainLayout, currentView,newView);

    // Reset the layer name in the callout
    final FeatureContent activeContent = featureContents.get(getCurrentLayoutId());
    final String layerName = activeContent.getLayerName();

    // Clear any selections
    clearSelections();

    // Select the feature
    activeContent.getFeatureLayer().selectFeature(activeContent.getFeature());

    layerTitle.setText(layerName);

    // Reset popup index
    txtPopupCount.setText(getCurrentLayoutId() + 1 + " of " + featureContents.size() );

    currentIndex = getCurrentLayoutId();

    // Adjust visual cues for navigating features
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
  // Methods for keeping track of the current layout index
  private int getCurrentLayoutId(){
    return currentLayoutId;
  }
  private void incrementCurrentLayoutId(){
    currentLayoutId = currentLayoutId + 1;
  }
  private void decrementCurrentLayoutId(){
    currentLayoutId = currentLayoutId - 1;
  }
  private void replaceView(final LinearLayout mainView, final View oldView, final View newView){
    mainView.removeView(oldView);
    mainView.addView(newView);
  }


  private class MapTouchListener extends DefaultMapViewOnTouchListener {

    public MapTouchListener(final Context context, final MapView mapView) {
      super(context, mapView);
    }

    /**
     * When a user taps on the map, an identify action is initiated and
     * any features found are displayed in a callout view.
     * @param e - MotionEvent
     * @return boolean
     */
    @Override
    public boolean onSingleTapConfirmed(final MotionEvent e) {

      if (!mGraphicsOverlay.getGraphics().isEmpty()) {
        if (mGraphicsOverlay.getGraphics().get(0).isSelected()) {
          mGraphicsOverlay.getGraphics().get(0).setSelected(false);
        }
      }
      // clear any previous selections
      clearSelections();

      // get the screen point where user tapped
      final android.graphics.Point screenPoint = new android.graphics.Point((int) e.getX(), (int) e.getY());

      final ListenableFuture<List<IdentifyLayerResult>> identifyLayers = mMapView.identifyLayersAsync(screenPoint,5d,false, 1);

      identifyLayers.addDoneListener(new Runnable() {
        @Override
        public void run() {
          try {
            final Point clickedLocation = mMapView.screenToLocation(screenPoint);

            final List<IdentifyLayerResult> results = identifyLayers.get();
            final List<FeatureContent> content = mPresenter.identifyFeatures(results);
            if (content.isEmpty()){
              showMessage(getString(R.string.no_features_found));
              displaySearchResult(clickedLocation, null, false);
            }else{
              final View v = buildContentView(content);
              displaySearchResult(clickedLocation,v, false);
            }

          } catch (InterruptedException | ExecutionException ie) {
            Log.e(TAG,ie.getMessage());
          }

        }
      });


      return super.onSingleTapConfirmed(e);
    }
  }
}
