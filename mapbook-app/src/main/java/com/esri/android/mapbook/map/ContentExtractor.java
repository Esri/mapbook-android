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
import android.util.Log;
import com.esri.android.mapbook.data.Entry;
import com.esri.arcgisruntime.data.Field;
import com.esri.arcgisruntime.mapping.GeoElement;
import com.esri.arcgisruntime.mapping.Item;
import com.esri.arcgisruntime.mapping.popup.Popup;
import com.esri.arcgisruntime.mapping.popup.PopupField;
import com.esri.arcgisruntime.mapping.popup.PopupFieldFormat;
import com.esri.arcgisruntime.mapping.popup.PopupManager;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * This class extract a list of Entry items from a given popup
 */
public class ContentExtractor implements ContentExtractorContract {

  private final Context mContext;
  private static final String DATE_FORMAT = "MM-dd-yyyy";
  private static final String TAG = ContentExtractor.class.getSimpleName();

  @Inject
  public ContentExtractor(final Context context){
    mContext = context;
  }

  @Override public List<Entry> getPopupFields(final Popup popup) {

    final SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
    final PopupFieldFormat dateFormat = new PopupFieldFormat();
    dateFormat.setDateFormat(PopupFieldFormat.DateFormat.SHORT_DATE_SHORT_TIME);

    final PopupManager mPopupManager = new PopupManager(mContext, popup);
    final List<Entry> entries = new ArrayList<>();
    final List<PopupField> fields = mPopupManager.getDisplayedFields();
    for (final PopupField field : fields) {
      final Field.Type fieldType = mPopupManager.getFieldType(field);
      final Object fieldValue = mPopupManager.getFieldValue(field);
      final String fieldLabel = field.getLabel();
      String value = "";
      if (fieldType == Field.Type.DATE && fieldValue !=null ){
        final GregorianCalendar date = (GregorianCalendar) fieldValue;
        value = formatter.format(date.getTime());
        final Entry entry = new Entry(fieldLabel, value);
        entries.add(entry);

      }else if (fieldType == Field.Type.TEXT && fieldValue != null) {
        value = fieldValue.toString();
        final Entry entry = new Entry(fieldLabel, value);
        entries.add(entry);
      }

    }

    return entries;
  }

  /**
   * Extract attributes from a GeoElement and return
   * a list of <Entry></Entry> items
   * @param geoElement - GeoElement
   * @return - List<Entry></Entry>
   */
  @Override public List<Entry> getEntriesFromGeoElement(GeoElement geoElement) {
    final SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
    final PopupFieldFormat dateFormat = new PopupFieldFormat();
    dateFormat.setDateFormat(PopupFieldFormat.DateFormat.SHORT_DATE_SHORT_TIME);

    Map<String,Object> attrMap =geoElement.getAttributes();
    List<Entry> entries = new ArrayList<>(attrMap.size());
    Set<String> keys = attrMap.keySet();
    for (String key: keys){
      Object o = attrMap.get(key);
      if (o != null){
        String camelCase = key.substring(0,1) + key.substring(1).toLowerCase();
        if (o instanceof GregorianCalendar){
          final GregorianCalendar date = (GregorianCalendar) o;
          final String value = formatter.format(date.getTime());

          entries.add(new Entry(camelCase, value));
        }else{
          entries.add(new Entry(camelCase, o.toString()));
        }

        Log.i(TAG,o.toString());
      }
    }
    return entries;
  }

}
