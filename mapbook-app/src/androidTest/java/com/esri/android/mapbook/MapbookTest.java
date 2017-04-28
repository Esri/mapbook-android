package com.esri.android.mapbook;

import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v7.app.ActionBar;
import android.support.v7.view.menu.ActionMenuItemView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.esri.android.mapbook.map.MapActivity;
import com.esri.android.mapbook.mapbook.MapbookActivity;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.robotium.solo.Solo;
import org.junit.*;
import org.junit.runner.RunWith;

import java.util.ArrayList;

@RunWith(AndroidJUnit4.class)
public class MapbookTest {

  private Solo solo;

  private static final String TAG = MapbookTest.class.getSimpleName();

  @Rule
  public ActivityTestRule<MapbookActivity>  mActivityRule = new ActivityTestRule<MapbookActivity>(MapbookActivity.class);

  @Before
  public void setUp(){

    solo = new Solo(InstrumentationRegistry.getInstrumentation(), mActivityRule.getActivity());
    Log.i(TAG, "SetUp");
  }

  @After
  public void tearDown() throws Exception {
    //tearDown() is run after a test case has finished.
    //finishOpenedActivities() will finish all
    // the activities that have been opened during the test execution.
    solo.finishOpenedActivities();
    Log.i(TAG, "TearDown");
  }

  /**
   * Test that MapBookActivity is displayed on start up
   * @throws Exception
   */
  @Test
  public void checkIfMapbookActivityIsDisplayed() throws Exception {
    Assert.assertTrue(solo.waitForActivity("MapbookActivity", 2000));
    solo.assertCurrentActivity(TAG + " not displayed", MapbookActivity.class);

  }

  /**
   * Test that the toolbar title is set correctly
   * @throws Exception
   */
  @Test
  public void checkIfTitleIsDisplayed() throws Exception{
    Assert.assertNotNull(solo.waitForActivity("MapbookActivity", 2000));
    final ActionBar actionBar = mActivityRule.getActivity().getSupportActionBar();
    Assert.assertNotNull(actionBar);
    Assert.assertTrue(solo.waitForText( solo.getString(R.string.title)));
  }

  /**
   * Test that mapbook information is present and
   * at least one map is present in the mapbook
   * @throws Exception
   */
  @Test
  public void checkForMapbookContent() throws Exception{
    Assert.assertTrue(solo.waitForActivity("MapbookActivity", 2000));
    Assert.assertTrue(solo.waitForFragmentById(R.id.mapbookViewFragment));

    final TextView title = (TextView) solo.getView(R.id.txtTitle);
    Assert.assertTrue(title.isShown());
    Assert.assertTrue(title.getText().length() > 0);
    Log.i(TAG, title.getText().toString());

    final TextView bookDate = (TextView) solo.getView(R.id.txtCrDate);
    Assert.assertTrue(bookDate.isShown());
    Assert.assertTrue(bookDate.getText().length() > 0);
    Log.i(TAG, bookDate.getText().toString());

    final TextView bookSize = (TextView) solo.getView(R.id.txtFileSize);
    Assert.assertTrue(bookSize.isShown());
    Assert.assertTrue(bookSize.getText().length()>0);
    Log.i(TAG, bookSize.getText().toString());

    final TextView mapCount = (TextView) solo.getView(R.id.txtMapCount);
    Assert.assertTrue(mapCount.isShown());
    Assert.assertTrue(mapCount.getText().length() > 0);
    Log.i(TAG, mapCount.getText().toString());

    // We assume at least one map is present
    final RecyclerView mapRecyclerView = (RecyclerView) solo.getView(R.id.recyclerView) ;
    Assert.assertTrue(mapRecyclerView.isShown());
    Assert.assertTrue(mapRecyclerView.getAdapter().getItemCount()>0);

    // Get the content for the map
    final ArrayList<View> views = solo.getViews(mapRecyclerView);
    final ImageView mapThumbnail = solo.getImage(0);
    solo.clickOnView(mapThumbnail);
    Assert.assertNotNull(mapThumbnail);
    Assert.assertTrue(mapThumbnail.isShown());

    solo.takeScreenshot("MAPBOOK_ACTIVITY");
  }

  /**
   * Test that clicking on the recycler view
   * shows a map.
   * @throws Exception
   */
  @Test
  public void checkClickOnMapThumbnailShowsMap() throws Exception{
    solo.waitForActivity("MapbookActivity", 2000);
    solo.waitForFragmentById(R.id.mapbookViewFragment);

    final RecyclerView mapRecyclerView = (RecyclerView) solo.getView(R.id.recyclerView) ;
    Assert.assertNotNull(mapRecyclerView);
    // Click on first item in recycler view
    solo.clickOnView(mapRecyclerView);

    // This should trigger the map activity to be shown
    solo.waitForActivity(MapActivity.class, 2000);
    solo.waitForFragmentById(R.id.mapLinearLayout);

    final MapView mapView = (MapView) solo.getView(R.id.mapView);
    mapView.getMap().addDoneLoadingListener(new Runnable() {
      @Override public void run() {
        solo.sleep(1000);
        solo.takeScreenshot("MAP_ACTIVITY");

      }
    });
  }

  /**
   * Test that checks for the presence of three menu items
   * (show layers, show bookmarks, and search)
   */
  @Test
  public void checkMapMenuItemsVisible(){

    navigateToMap();

    final SearchView search = (SearchView) solo.getView(R.id.action_search);
    Assert.assertTrue(search.isShown());

    final ActionMenuItemView bookmarks = (ActionMenuItemView) solo.getView(R.id.bookmarks);
    Assert.assertTrue(bookmarks.isShown());

    final ActionMenuItemView layers = (ActionMenuItemView) solo.getView(R.id.action_layers);
    Assert.assertTrue(layers.isShown());
  }

  /**
   * Test that the TOC is shown when layers menu item is clicked
   */
  @Test
  public void checkShowLayers() throws Exception{

    navigateToMap();

    final ActionMenuItemView layers = (ActionMenuItemView) solo.getView(R.id.action_layers);
    solo.clickOnView(layers);

    // Clicking on layers should reveal the TOC
    final View toc = solo.getView(R.id.mapLayerRecyclerView);
    Assert.assertTrue(toc.isShown());

    solo.sleep(2000);
    solo.takeScreenshot("TOC");
  }

  /**
   * Test that the query hint is shown when
   * the search view is clicked.  Test that
   * suggestions are displayed when "123" is typed
   * into search view.
   * @throws Exception
   */
  @Test
  public void checkShowSearchSuggestions() throws Exception {

    navigateToMap();

    final SearchView search = (SearchView) solo.getView(R.id.action_search);
    solo.clickOnView(search);

    final String queryHint = search.getQueryHint().toString();

    Assert.assertTrue(queryHint.equalsIgnoreCase(mActivityRule.getActivity().getString(R.string.query_hint)));

    solo.sleep(2000);
    solo.takeScreenshot("SEARCH_VIEW");

    // Type some text into search view
    final EditText editText = solo.getEditText(0);
    solo.typeText(editText, "123");
    solo.sleep(4000);

    // Grab the list displaying the suggestions
    final ListView listView = solo.getView(ListView.class,0);
    Assert.assertNotNull(listView);
    // Assert there are at least 1  or more suggestions
    Assert.assertTrue(listView.getAdapter().getCount()>0);

  }

  @Test
  public void checkSearchForAddress() throws Exception{

    navigateToMap();

    final SearchView search = (SearchView) solo.getView(R.id.action_search);
    solo.clickOnView(search);

    // Type an address into search view
    final EditText editText = solo.getEditText(0);
    solo.typeText(editText, "123 Spring Ave");

    solo.sleep(4000);
    solo.pressSoftKeyboardSearchButton();
    solo.sleep(4000);

    // Map view should have a pin in the graphics layer
    final MapView mapView = (MapView) solo.getView(R.id.mapView);
    final Graphic graphic = mapView.getGraphicsOverlays().get(0).getGraphics().get(0);
    Assert.assertNotNull(graphic);
    Assert.assertTrue(mapView.getGraphicsOverlays().get(0).getGraphics().size()==1);


    solo.sleep(3000);

    // Simulate the same location being tapped
    final Geometry point = graphic.getGeometry();
    final android.graphics.Point derivedScreenLocation = deriveScreenPointForLocation(point);
    solo.clickOnScreen(derivedScreenLocation.x, derivedScreenLocation.y);

    solo.sleep(3000);
    Assert.assertTrue(solo.waitForView(R.id.calloutLinearLayout));
  }

  private android.graphics.Point deriveScreenPointForLocation(final Geometry location){
    final MapView mapView = (MapView) solo.getView(R.id.mapView) ;
    final DisplayMetrics metrics = new DisplayMetrics();
    mActivityRule.getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);;
    final float screenHeight = metrics.heightPixels;
    final float mapViewHeight = mapView.getHeight();
    final float buffer = screenHeight - mapViewHeight;
    final Point projectedPoint = (Point) GeometryEngine.project(location, SpatialReference.create(3857));

    final android.graphics.Point derivedPoint =  mapView.locationToScreen(projectedPoint);

    return new android.graphics.Point(derivedPoint.x,derivedPoint.y+Math.round(buffer));

  }
  /**
   * Test that bookmarks can be shown and that clicking
   * on them results in a change in map scale for
   * at least one bookmark.  Assumes that a bookmark
   * with a different scale exists in the map.
   * @throws Exception
   */
  @Test
  public void checkBookmarksShown() throws Exception {

    navigateToMap();

    final ActionMenuItemView bookmarks = (ActionMenuItemView) solo.getView(R.id.bookmarks);
    solo.clickOnView(bookmarks);

    // Clicking on layers should reveal the bookmarks
    final View bookmarkView = solo.getView(R.id.mapBookmarkRecyclerView);
    Assert.assertTrue(bookmarkView.isShown());

    solo.sleep(2000);
    solo.takeScreenshot("BOOKMARKS");

    // Get the current extent of the map view
    final MapView mapView = (MapView) solo.getView(R.id.mapView);
    final double mapScale = mapView.getMapScale();
    boolean bookmarkExtentsDiffer = false;

    // Click on the second bookmark
    final RecyclerView bookmarkRecylerView = (RecyclerView) bookmarkView;
    final int bookmarkCount = bookmarkRecylerView.getAdapter().getItemCount();
    if (bookmarkCount > 1) {
      for (int x = 0; x < bookmarkCount; x++) {
        solo.clickInRecyclerView(x);
        solo.sleep(2000);
        final double bookmarkScale = mapView.getMapScale();
        Log.i(TAG, mapScale + " bookmark scale "+ bookmarkScale);
        if (bookmarkScale != mapScale) {
          bookmarkExtentsDiffer = true;
          break;
        }
      }
    }
    Assert.assertTrue(bookmarkExtentsDiffer);
  }

  /**
   * Clicking on the back button while in a map view
   * should take the user back to the main screen.
   * @throws Exception
   */
  @Test
  public void checkNavigationBackToMainScreen() throws Exception{

    navigateToMap();

    // Go back to main screen
    solo.goBack();

    Assert.assertTrue(solo.waitForActivity("MapbookActivity", 2000));
    Assert.assertTrue(solo.waitForFragmentById(R.id.mapbookViewFragment));
  }

  /**
   * Navigate to the first map in the mabook
   */
  private void navigateToMap(){
    Assert.assertTrue(solo.waitForActivity("MapbookActivity", 2000));
    Assert.assertTrue(solo.waitForFragmentById(R.id.mapbookViewFragment));

    final RecyclerView mapRecyclerView = (RecyclerView) solo.getView(R.id.recyclerView) ;
    Assert.assertNotNull(mapRecyclerView);
    // Click on first item in recycler view
    solo.clickOnView(mapRecyclerView);

    // This should trigger the map activity to be shown
    Assert.assertTrue(solo.waitForActivity(MapActivity.class, 2000));
    Assert.assertTrue(solo.waitForFragmentById(R.id.mapLinearLayout));
  }
}
