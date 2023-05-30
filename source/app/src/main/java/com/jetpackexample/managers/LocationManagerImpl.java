/*
 * Copyright 2022 NXP
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jetpackexample.managers;

import android.content.Context;
import android.location.LocationManager;

public class LocationManagerImpl {

    private static final String TAG = LocationManagerImpl.class.getName();

    private LocationManager locationManager;

    private static LocationManagerImpl mInstance = null;

    private LocationManagerImpl(final Context context) {
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    public static synchronized LocationManagerImpl getInstance(final Context context) {
        if (mInstance == null) {
            mInstance = new LocationManagerImpl(context);
        }

        return mInstance;
    }

    public boolean isSupported() {
        return locationManager != null;
    }

    public boolean isEnabled() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }
}
