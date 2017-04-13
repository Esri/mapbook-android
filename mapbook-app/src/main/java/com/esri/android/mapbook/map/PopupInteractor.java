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
import com.esri.android.mapbook.data.Entry;
import com.esri.arcgisruntime.data.Field;
import com.esri.arcgisruntime.mapping.popup.Popup;
import com.esri.arcgisruntime.mapping.popup.PopupField;
import com.esri.arcgisruntime.mapping.popup.PopupFieldFormat;
import com.esri.arcgisruntime.mapping.popup.PopupManager;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

public class PopupInteractor implements PopupInteractorContract {

  private PopupManager mPopupManager;
  private Context mContext;
  private static final String DATE_FORMAT = "MM-dd-yyyy";

  @Inject
  public PopupInteractor(Context context){
    mContext = context;
  }
  @Override public List<Entry> getPopupFields(Popup popup) {

    SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
    PopupFieldFormat dateFormat = new PopupFieldFormat();
    dateFormat.setDateFormat(PopupFieldFormat.DateFormat.SHORT_DATE_SHORT_TIME);

    mPopupManager = new PopupManager(mContext, popup);
    List<Entry> entries = new ArrayList<>();
    List<PopupField> fields = mPopupManager.getDisplayedFields();
    for (PopupField field : fields) {
      Field.Type fieldType = mPopupManager.getFieldType(field);
      Object fieldValue = mPopupManager.getFieldValue(field);
      String fieldLabel = field.getLabel();
      String value = "";
      if (fieldType == Field.Type.DATE && fieldValue !=null ){
        GregorianCalendar date = (GregorianCalendar) fieldValue;
        value = formatter.format(date.getTime());
        Entry entry = new Entry(field.getLabel(), value);
        entries.add(entry);

      }else if (fieldType == Field.Type.TEXT && fieldValue != null) {
        value = fieldValue.toString();
        Entry entry = new Entry(field.getLabel(), value);
        entries.add(entry);
      }

    }

    return entries;
  }

}
