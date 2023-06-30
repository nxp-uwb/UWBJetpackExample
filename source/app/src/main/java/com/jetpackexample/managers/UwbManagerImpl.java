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
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.uwb.RangingParameters;
import androidx.core.uwb.RangingResult;
import androidx.core.uwb.UwbAddress;
import androidx.core.uwb.UwbComplexChannel;
import androidx.core.uwb.UwbControleeSessionScope;
import androidx.core.uwb.UwbControllerSessionScope;
import androidx.core.uwb.UwbDevice;
import androidx.core.uwb.UwbManager;
import androidx.core.uwb.rxjava3.UwbClientSessionScopeRx;
import androidx.core.uwb.rxjava3.UwbManagerRx;

import com.jetpackexample.utils.Utils;
import com.jetpackexample.UwbDeviceConfigData;
import com.jetpackexample.UwbPhoneConfigData;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subscribers.DisposableSubscriber;

public class UwbManagerImpl {

    private static final String TAG = UwbManagerImpl.class.getName();

    public static final int UWB_CHANNEL = 9;
    public static final int UWB_PREAMBLE_INDEX = 10;

    private UwbManager uwbManager = null;


    private Single<UwbControllerSessionScope> controllerSessionScopeSingle = null;
    private UwbControllerSessionScope controllerSessionScope = null;
    private Single<UwbControleeSessionScope> controleeSessionScopeSingle = null;
    private UwbControleeSessionScope controleeSessionScope = null;
    private Disposable disposable = null;

    private static UwbManagerImpl mInstance = null;

    public interface UwbRangingListener {
        void onRangingStarted(UwbPhoneConfigData uwbPhoneConfigData);

        void onRangingResult(RangingResult rangingResult);

        void onRangingError(Throwable error);

        void onRangingComplete();
    }

    private UwbManagerImpl(final Context context) {
        // Create the Uwb Manager if supported by this device
        PackageManager packageManager = context.getPackageManager();
        if (packageManager.hasSystemFeature("android.hardware.uwb")) {
            uwbManager = UwbManager.createInstance(context);
        }
    }

    public static synchronized UwbManagerImpl getInstance(final Context context) {
        if (mInstance == null) {
            mInstance = new UwbManagerImpl(context);
        }

        return mInstance;
    }

    public boolean isSupported() {
        return uwbManager != null;
    }

    public boolean isEnabled() {
        return true;
    }

    public void startRanging(UwbDeviceConfigData uwbDeviceConfigData, UwbRangingListener uwbRangingListener) {
        Thread t = new Thread(() -> {

            byte uwbDeviceRangingRole = selectUwbDeviceRangingRole(uwbDeviceConfigData.getSupportedDeviceRangingRoles());
            Log.d(TAG, "Uwb device supported ranging roles: " + uwbDeviceConfigData.getSupportedDeviceRangingRoles() + ", selected role for UWB device: " + uwbDeviceRangingRole);

            byte uwbProfileId = selectUwbProfileId(uwbDeviceConfigData.getSupportedUwbProfileIds());
            Log.d(TAG, "Uwb device supported UWB profile IDs: " + uwbDeviceConfigData.getSupportedUwbProfileIds() + ", selected UWB profile ID: " + uwbProfileId);

            UwbAddress localAddress;
            if (uwbDeviceRangingRole == 0x01) {
                Log.d(TAG, "Android device will act as Controlee!");
                controleeSessionScopeSingle = UwbManagerRx.controleeSessionScopeSingle(uwbManager);
                controleeSessionScope = controleeSessionScopeSingle.blockingGet();
                localAddress = controleeSessionScope.getLocalAddress();
            } else {
                Log.d(TAG, "Android device will act as Controller!");
                controllerSessionScopeSingle = UwbManagerRx.controllerSessionScopeSingle(uwbManager);
                controllerSessionScope = controllerSessionScopeSingle.blockingGet();
                localAddress = controllerSessionScope.getLocalAddress();
            }

            // Assign a random Session ID
            int sessionId = new Random().nextInt();
            Log.d(TAG, "UWB sessionId: " + sessionId);

            UwbComplexChannel uwbComplexChannel = new UwbComplexChannel(UWB_CHANNEL, UWB_PREAMBLE_INDEX);
            Log.d(TAG, "UWB Channel params, Channel: " + UWB_CHANNEL + " preambleIndex: " + UWB_PREAMBLE_INDEX);

            // Need to pass the local address to the other peer
            Log.d(TAG, "UWB Local Address: " + localAddress);

            // UWB Shield device
            UwbAddress shieldUwbAddress = new UwbAddress(uwbDeviceConfigData.getDeviceMacAddress());
            UwbDevice shieldUwbDevice = new UwbDevice(shieldUwbAddress);
            Log.d(TAG, "UWB Destination Address: " + shieldUwbAddress);

            List<UwbDevice> listUwbDevices = new ArrayList<>();
            listUwbDevices.add(shieldUwbDevice);

            // https://developer.android.com/guide/topics/connectivity/uwb#known_issue_byte_order_reversed_for_mac_address_and_static_sts_vendor_id_fields
            // GMS Core update is doing byte reverse as per UCI spec
            // SessionKey is used to match Vendor ID in UWB Device firmware
            byte[] sessionKey = Utils.hexStringToByteArray("0807010203040506");

            Log.d(TAG, "Configure ranging parameters for Profile ID: " + uwbProfileId);
            RangingParameters rangingParameters = new RangingParameters(
                    uwbProfileId,
                    sessionId,
                    sessionKey,
                    uwbComplexChannel,
                    listUwbDevices,
                    RangingParameters.RANGING_UPDATE_RATE_FREQUENT
            );

            Flowable<RangingResult> rangingResultFlowable;
            if (uwbDeviceRangingRole == 0x01) {
                Log.d(TAG, "Configure controlee flowable");
                rangingResultFlowable =
                        UwbClientSessionScopeRx.rangingResultsFlowable(controleeSessionScope,
                                rangingParameters);
            } else {
                Log.d(TAG, "Configure controller flowable");
                rangingResultFlowable =
                        UwbClientSessionScopeRx.rangingResultsFlowable(controllerSessionScope,
                                rangingParameters);
            }

            // Consume ranging results from Flowable using Disposable
            disposable = rangingResultFlowable
                    .delay(199, TimeUnit.MILLISECONDS)
                    .subscribeWith(new DisposableSubscriber<RangingResult>() {
                        @Override
                        public void onStart() {
                            Log.d(TAG, "UWB Disposable started");
                            request(1);
                        }

                        @Override
                        public void onNext(RangingResult rangingResult) {
                            Log.d(TAG, "UWB Ranging notification received");
                            uwbRangingListener.onRangingResult(rangingResult);
                            request(1);
                        }

                        @Override
                        public void onError(Throwable error) {
                            Log.d(TAG, "UWB Ranging error received");
                            uwbRangingListener.onRangingError(error);
                        }

                        @Override
                        public void onComplete() {
                            Log.d(TAG, "UWB Ranging session completed");
                            uwbRangingListener.onRangingComplete();
                        }
                    });

            // Create ShareableData with configured UWB Session params
            UwbPhoneConfigData uwbPhoneConfigData = new UwbPhoneConfigData();
            uwbPhoneConfigData.setSpecVerMajor((short) 0x0100);
            uwbPhoneConfigData.setSpecVerMinor((short) 0x0000);
            uwbPhoneConfigData.setSessionId(sessionId);
            uwbPhoneConfigData.setPreambleId((byte) UWB_PREAMBLE_INDEX);
            uwbPhoneConfigData.setChannel((byte) UWB_CHANNEL);
            uwbPhoneConfigData.setProfileId(uwbProfileId);
            uwbPhoneConfigData.setDeviceRangingRole(uwbDeviceRangingRole);
            uwbPhoneConfigData.setPhoneMacAddress(localAddress.getAddress());

            // Send the UWB ranging session configuration data back to the listener
            uwbRangingListener.onRangingStarted(uwbPhoneConfigData);
        });

        t.start();
    }

    public void stopRanging() {
        if (disposable != null) {
            disposable.dispose();
        }
    }

    public void close() {
        if (disposable != null) {
            disposable.dispose();
        }
    }

    private byte selectUwbProfileId(int supportedUwbProfileIds) {
        if (BigInteger.valueOf(supportedUwbProfileIds).testBit(RangingParameters.CONFIG_UNICAST_DS_TWR)) {
            return (byte) RangingParameters.CONFIG_UNICAST_DS_TWR;
        }

        return 0;
    }

    private byte selectUwbDeviceRangingRole(int supportedUwbDeviceRangingRoles) {
        if (BigInteger.valueOf(supportedUwbDeviceRangingRoles).testBit(0)) {
            return 1;
        } else if (BigInteger.valueOf(supportedUwbDeviceRangingRoles).testBit(1)) {
            return 2;
        }

        return 0;
    }
}
