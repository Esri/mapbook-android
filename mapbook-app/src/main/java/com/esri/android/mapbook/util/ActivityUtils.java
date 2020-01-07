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

package com.esri.android.mapbook.util;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.util.Calendar;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This provides methods to help Activities load their UI.
 */
public class ActivityUtils {

  /**
   * The {@code fragment} is added to the container view with id {@code frameId}. The operation is
   * performed by the {@code fragmentManager}.
   *
   */
  public static void addFragmentToActivity (final FragmentManager fragmentManager,
      final Fragment fragment, final int frameId) {
    checkNotNull(fragmentManager);
    checkNotNull(fragment);
    final FragmentTransaction transaction = fragmentManager.beginTransaction();
    transaction.add(frameId, fragment);
    transaction.commit();
  }

  /**
   * The {@code fragment} is added to the container view with id {@code tag}. The operation is
   * performed by the {@code fragmentManager}.
   *
   */
  public static void addFragmentToActivity (final FragmentManager fragmentManager,
      final Fragment fragment, final String tag) {
    checkNotNull(fragmentManager);
    checkNotNull(fragment);
    final FragmentTransaction transaction = fragmentManager.beginTransaction();
    transaction.add(fragment, tag);
    transaction.commit();
  }
  /**
   * Retrun a date string for given Calendar object
   * @param calendar - Calendar
   * @return String with date format YEAR/MONTH/DAY
   */
  public static String getDateString(final Calendar calendar){

    final int year = calendar.get(Calendar.YEAR) ;
    final int month = calendar.get(Calendar.MONTH);
    final int day = calendar.get(Calendar.DAY_OF_MONTH);

    return String.format("%4d/%02d/%02d",  year, month+1, day);
  }
}
