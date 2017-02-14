/* Copyright 2017 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * For additional information, contact:
 * Environmental Systems Research Institute, Inc.
 * Attn: Contracts Dept
 * 380 New York Street
 * Redlands, California, USA 92373
 *
 * email: contracts@esri.com
 *
 */
package com.esri.android.mapbook;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.MobileMapPackage;
import com.esri.arcgisruntime.mapping.view.MapView;

import java.util.List;

public class MapViewActivity extends AppCompatActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.map_view);
    final Toolbar toolbar = (Toolbar) findViewById(R.id.mapToolbar);
    if (toolbar != null) {
      setSupportActionBar(toolbar);
      final ActionBar actionBar = (this).getSupportActionBar();
      if (actionBar != null) {
        actionBar.setTitle(R.string.title);
      }
      toolbar.setNavigationIcon(null);
    }

    Intent intent = getIntent();
    String mmpkPath = intent.getStringExtra(MainActivity.FILE_PATH);
    int index = intent.getIntExtra("INDEX",0);
    loadMapInView(mmpkPath, index);
  }
  /**
   *
   * @param menu Menu
   * @return boolean
   */
  @Override
  public boolean onCreateOptionsMenu(final Menu menu) {
    super.onCreateOptionsMenu(menu);
    getMenuInflater().inflate(R.menu.menu, menu);
    // Retrieve the SearchView and plug it into SearchManager
    final SearchView searchView= (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.action_search));
    searchView.setQueryHint(getString(R.string.query_hint));
    final SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
    searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
      @Override public boolean onQueryTextSubmit(final String query) {

        return true;
      }

      @Override public boolean onQueryTextChange(final String newText) {
        return false;
      }
    });
    return true;
  }
  private void loadMapInView(String mmpkPath, final int index){
    final MobileMapPackage mmp = new MobileMapPackage(mmpkPath);
    mmp.addDoneLoadingListener(new Runnable() {
      @Override public void run() {
        if (mmp.getLoadStatus() == LoadStatus.LOADED) {
          List<ArcGISMap> maps = mmp.getMaps();
          ArcGISMap map = maps.get(index);
          MapView mapView = (MapView) findViewById(R.id.mapView);
          mapView.setMap(map);

        }else{
          Log.e("MapViewActivity", "There was a problem loading the map " + mmp.getLoadStatus().name());
          Throwable err = mmp.getLoadError();
          err.printStackTrace();
          Log.i("MapViewActivity", err.getMessage());
        }
      }
    });
    mmp.loadAsync();
  }

}
