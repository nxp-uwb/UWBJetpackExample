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

package com.jetpackexample;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.uwb.RangingResult;

import com.jetpackexample.managers.BluetoothManagerImpl;
import com.jetpackexample.managers.LocationManagerImpl;
import com.jetpackexample.managers.UwbManagerImpl;
import com.jetpackexample.utils.Utils;

public class MainActivity extends AppCompatActivity
        implements BluetoothManagerImpl.BluetoothConnectionListener, BluetoothManagerImpl.BluetoothDataReceivedListener {

    private static final String TAG = MainActivity.class.getName();

    public static final int PERMISSION_REQUEST_CODE = 0x0001;

    private LocationManagerImpl locationManagerImpl = null;
    private BluetoothManagerImpl bluetoothManagerImpl = null;
    private UwbManagerImpl uwbManagerImpl = null;

    private String remoteDeviceName;

    private TextView bleState;
    private TextView uwbState;
    private TextView uwbDistanceInfo;
    private TextView uwbAoaInfo;
    private ImageView uwbAoaArrow;
    private TextView uwbRangingDevice;

    private Handler mHandler;

    // App states
    public enum AppState {
        notStarted,
        bleScanning,
        bleConnected,
        uwbConfiguring,
        uwbStarted,
        uwbStopped
    }

    // Android UWB OoB protocol
    public enum MessageId {
        // Messages from the Uwb device
        uwbDeviceConfigurationData((byte) 0x01),
        uwbDidStart((byte) 0x02),
        uwbDidStop((byte) 0x03),

        // Messages from the Uwb phone
        initialize((byte) 0xA5),
        uwbPhoneConfigurationData((byte) 0x0B),
        stop((byte) 0x0C);

        private final byte value;

        MessageId(final byte newValue) {
            value = newValue;
        }

        public byte getValue() {
            return value;
        }
    }

    /*
    Runnable mUpdater = new Runnable() {
        @Override
        public void run() {
            try {
                updateRangingAoaInfo(new Random().nextInt(91) -45);
                updateRangingDistanceInfo(new Random().nextFloat()*3);
            } finally {
                // 100% guarantee that this always happens, even if
                // your update method throws an exception
                mHandler.postDelayed(mUpdater, 1000);
            }
        }
    };*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHandler = new Handler();

        uwbManagerImpl = UwbManagerImpl.getInstance(MainActivity.this);
        locationManagerImpl = LocationManagerImpl.getInstance(MainActivity.this);
        bluetoothManagerImpl = BluetoothManagerImpl.getInstance(MainActivity.this);

        initViews();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Automatically proceed with demo
        initializeBleUwb();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (bluetoothManagerImpl != null) {
            bluetoothManagerImpl.stopLeDeviceScan();
            bluetoothManagerImpl.close();
        }

        if (uwbManagerImpl != null) {
            uwbManagerImpl.close();
        }

        updateAppState(AppState.notStarted);
        resetRangingInfo();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        if (bluetoothManagerImpl != null) {
            bluetoothManagerImpl.stopLeDeviceScan();
            bluetoothManagerImpl.close();
        }

        if (uwbManagerImpl != null) {
            uwbManagerImpl.close();
        }

        updateAppState(AppState.notStarted);
        resetRangingInfo();
    }

    private void initViews() {
        bleState = findViewById(R.id.ble_state);
        uwbState = findViewById(R.id.uwb_state);
        uwbDistanceInfo = findViewById(R.id.uwb_distance_info);
        uwbAoaInfo = findViewById(R.id.uwb_aoa_info);
        uwbAoaArrow = findViewById(R.id.imgArrow);
        uwbRangingDevice = findViewById(R.id.uwb_ranging_device);
    }

    private void initializeBleUwb() {
        if (checkPermissions()) {
            if (bluetoothManagerImpl.isSupported() && locationManagerImpl.isSupported() && uwbManagerImpl.isSupported()) {
                if (bluetoothManagerImpl.isEnabled()) {
                    if (locationManagerImpl.isEnabled()) {
                        if (uwbManagerImpl.isEnabled()) {
                            if (!bluetoothManagerImpl.isConnected()) {
                                updateAppState(AppState.bleScanning);

                                Log.d(TAG, "Start Bluetooth LE Device scanning");
                                bluetoothManagerImpl.startLeDeviceScan(device -> {
                                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                                        // Ignore devices that do not define name or address
                                        if ((device.getName() == null) || (device.getAddress() == null)) {
                                            return;
                                        }

                                        Log.d(TAG, "Let's proceed to connect to: " + device.getName());

                                        // Stop scanning and further proceed to connect to the device
                                        bluetoothManagerImpl.stopLeDeviceScan();
                                        bluetoothManagerImpl.connect(device.getAddress());
                                    } else {
                                        Log.e(TAG, "Missing required permission to read Bluetooth device name!");
                                    }
                                });
                            }
                        } else {
                            updateAppState(AppState.notStarted);
                            enableUwbDialog();
                        }
                    } else {
                        updateAppState(AppState.notStarted);
                        enableLocationDialog();
                    }
                } else {
                    updateAppState(AppState.notStarted);
                    enableBluetoothDialog();
                }
            } else {
                updateAppState(AppState.notStarted);
                missingRequiredTechnologiesDialog();
            }
        } else {
            updateAppState(AppState.notStarted);
            requestPermissions();
        }
    }

    public void processUwbRangingConfigurationData(byte[] data) {
        byte messageId = data[0];

        if (messageId == MessageId.uwbDeviceConfigurationData.getValue()) {
            byte[] trimmedData = Utils.trimLeadingBytes(data, 1);
            configureUwbRangingSession(trimmedData);
        } else if (messageId == MessageId.uwbDidStart.getValue()) {
            uwbRangingSessionStarted();
        } else if (messageId == MessageId.uwbDidStop.getValue()) {
            uwbRangingSessionStopped();
        } else {
            throw new IllegalArgumentException("Unexpected value");
        }
    }

    public void startUwbRangingConfiguration() {
        bluetoothManagerImpl.transmit(new byte[]{MessageId.initialize.getValue()});
    }

    public void transmitUwbPhoneConfigData(UwbPhoneConfigData uwbPhoneConfigData) {
        bluetoothManagerImpl.transmit(Utils.concat(
                new byte[]{MessageId.uwbPhoneConfigurationData.getValue()},
                uwbPhoneConfigData.toByteArray()));
    }

    public void transmitUwbRangingStop() {
        bluetoothManagerImpl.transmit(new byte[]{MessageId.stop.getValue()});
    }

    public void configureUwbRangingSession(byte[] data) {
        Log.d(TAG, "UWB Configure UwbDeviceConfigData: " + Utils.byteArrayToHexString(data));
        updateAppState(AppState.uwbConfiguring);

        final UwbDeviceConfigData uwbDeviceConfigData = UwbDeviceConfigData.fromByteArray(data);
        uwbManagerImpl.startRanging(uwbDeviceConfigData, new UwbManagerImpl.UwbRangingListener() {
            @Override
            public void onRangingStarted(UwbPhoneConfigData uwbPhoneConfigData) {
                transmitUwbPhoneConfigData(uwbPhoneConfigData);
            }

            @Override
            public void onRangingResult(RangingResult rangingResult) {
                displayRangingResult(rangingResult);
            }

            @Override
            public void onRangingError(Throwable error) {
                displayRangingError(error);
            }

            @Override
            public void onRangingComplete() {
                // Do nothing
            }
        });
    }

    private void uwbRangingSessionStarted() {
        updateAppState(AppState.uwbStarted);
        updateRangingPartner(remoteDeviceName);
    }

    public void uwbRangingSessionStopped() {
        updateAppState(AppState.uwbStopped);
    }

    public void stopRanging() {
        uwbManagerImpl.stopRanging();
    }

    private void displayRangingResult(RangingResult rangingResult) {
        // Update UI
        RangingResult.RangingResultPosition rangingResultPosition = (RangingResult.RangingResultPosition) rangingResult;
        if (rangingResultPosition.getPosition().getDistance() != null) {
            float distance = rangingResultPosition.getPosition().getDistance().getValue();
            Log.d(TAG, "Position distance: " + distance);
            updateRangingDistanceInfo(distance);
        } else {
            Log.e(TAG, "Unexpected rangingResult value, distance is null!");
        }
        if (rangingResultPosition.getPosition().getAzimuth() != null) {
            float aoaAzimuth = rangingResultPosition.getPosition().getAzimuth().getValue();
            Log.d(TAG, "Position AoA Azimuth: " + aoaAzimuth);
            updateRangingAoaInfo(aoaAzimuth);
        }
    }

    private void displayRangingError(Throwable error) {
        Log.e(TAG, "Ranging error: " + error.getMessage());
        error.printStackTrace();
    }

    public void updateAppState(AppState state) {
        switch (state) {
            case notStarted:
                runOnUiThread(() -> {
                    bleState.setText(getResources().getString(R.string.ble_not_started));
                    uwbState.setText(getResources().getString(R.string.uwb_not_started));
                });

                break;

            case bleScanning:
                runOnUiThread(() -> {
                    bleState.setText(getResources().getString(R.string.ble_scanning));
                    uwbState.setText(getResources().getString(R.string.uwb_not_started));
                });

                break;

            case bleConnected:
                runOnUiThread(() -> {
                    bleState.setText(getResources().getString(R.string.ble_connected));
                    uwbState.setText(getResources().getString(R.string.uwb_not_started));
                });

                break;

            case uwbConfiguring:
                runOnUiThread(() -> {
                    bleState.setText(getResources().getString(R.string.ble_connected));
                    uwbState.setText(getResources().getString(R.string.uwb_configuring));
                });

                break;

            case uwbStarted:
                runOnUiThread(() -> {
                    bleState.setText(getResources().getString(R.string.ble_connected));
                    uwbState.setText(getResources().getString(R.string.uwb_ranging));
                });

                break;

            case uwbStopped:
                runOnUiThread(() -> {
                    bleState.setText(getResources().getString(R.string.ble_connected));
                    uwbState.setText(getResources().getString(R.string.uwb_stopped));
                });

                break;

            default:
                runOnUiThread(() -> {
                    bleState.setText(getResources().getString(R.string.ble_unknown));
                    uwbState.setText(getResources().getString(R.string.uwb_unknown));
                });

                break;
        }
    }

    public void updateRangingDistanceInfo(float distance) {
        if(distance>1)
            runOnUiThread(() -> uwbDistanceInfo.setText(getResources().getString(R.string.uwb_distance_value, distance)));
        else
            runOnUiThread(() -> uwbDistanceInfo.setText(getResources().getString(R.string.uwb_distance_value_cm, distance*100)));

    }

    public void updateRangingAoaInfo(float aoa) {
        runOnUiThread(() -> {
            uwbAoaInfo.setText(getResources().getString(R.string.uwb_aoa_value, aoa));
            uwbAoaArrow.setRotation((float) aoa);
        });
    }


    public void updateRangingPartner(String partner) {
        runOnUiThread(() -> uwbRangingDevice.setText(getResources().getString(R.string.uwb_ranging_device_value, partner)));
    }

    public void resetRangingInfo() {
        runOnUiThread(() -> {
            uwbDistanceInfo.setText(getResources().getString(R.string.uwb_distance_not_started));
            uwbAoaInfo.setText(getResources().getString(R.string.uwb_aoa_not_started));
            uwbAoaArrow.setRotation(0);
            uwbRangingDevice.setText(getResources().getString(R.string.uwb_ranging_device_not_started));
        });
    }

    public void missingRequiredTechnologiesDialog() {
        Utils.showDialog(MainActivity.this,
                getResources().getString(R.string.app_name),
                getResources().getString(R.string.missing_techs),
                getResources().getString(R.string.dialog_accept),
                (dialog, which) -> {
                    // Nothing to do
                });
    }

    public void enableBluetoothDialog() {
        Utils.showDialog(MainActivity.this,
                getResources().getString(R.string.app_name),
                getResources().getString(R.string.request_enable_bluetooth),
                getResources().getString(R.string.dialog_cancel),
                (dialog, which) -> {
                    // Nothing to do
                },
                getResources().getString(R.string.dialog_accept),
                (dialog, which) -> startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)));
    }

    public void enableLocationDialog() {
        Utils.showDialog(MainActivity.this,
                getResources().getString(R.string.app_name),
                getResources().getString(R.string.request_enable_location),
                getResources().getString(R.string.dialog_cancel),
                (dialog, which) -> {
                    // Nothing to do
                },
                getResources().getString(R.string.dialog_accept),
                (dialog, which) -> startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)));
    }

    public void enableUwbDialog() {
        Utils.showDialog(MainActivity.this,
                getResources().getString(R.string.app_name),
                getResources().getString(R.string.request_enable_uwb),
                getResources().getString(R.string.dialog_cancel),
                (dialog, which) -> {
                    // Nothing to do
                },
                getResources().getString(R.string.dialog_accept),
                (dialog, which) -> startActivity(new Intent(Settings.ACTION_SETTINGS)));
    }

    private boolean checkPermissions() {
        return (checkSelfPermission(Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED)
                && (checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED)
                && (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED)
                && (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED)
                && (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                && (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                && (checkSelfPermission(Manifest.permission.UWB_RANGING) == PackageManager.PERMISSION_GRANTED);
    }

    private void requestPermissions() {
        String[] permissionsList = new String[]{
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.UWB_RANGING,
        };

        requestPermissions(permissionsList, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0) {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    boolean showRationale = shouldShowRequestPermissionRationale(permissions[0]);
                    if (!showRationale) {
                        // user denied flagging NEVER ASK AGAIN please enable this permission from device setting
                        // again the permission and directing to the app setting}
                        Utils.showDialog(MainActivity.this,
                                getString(R.string.app_name),
                                getResources().getString(R.string.denied_with_never_ask_again),
                                getString(R.string.dialog_ok),
                                (dialog, which) -> {
                                    Intent intent = new Intent();
                                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                                    intent.setData(uri);
                                    startActivity(intent);
                                });
                    } else {
                        Utils.showDialog(MainActivity.this,
                                getString(R.string.app_name),
                                getString(R.string.permission_alert),
                                getString(R.string.dialog_ok),
                                (dialog, which) -> finish());
                    }
                } else {
                    initializeBleUwb();
                }
            }
        }
    }

    @Override
    public void onConnect(String remoteDeviceName) {
        Toast.makeText(MainActivity.this, "Bluetooth connected!", Toast.LENGTH_LONG).show();
        this.remoteDeviceName = remoteDeviceName;
        updateAppState(AppState.bleConnected);

        // Let's proceed with the UWB session configuration
        startUwbRangingConfiguration();
    }

    @Override
    public void onDisconnect() {
        Toast.makeText(MainActivity.this, "Bluetooth disconnected!", Toast.LENGTH_LONG).show();
        this.remoteDeviceName = null;

        // Close UWB Session if this is ongoing
        uwbManagerImpl.close();

        // Let's restart the demo
        resetRangingInfo();
        initializeBleUwb();
    }

    @Override
    public void onDataReceived(byte[] data) {

        // Process the data received
        processUwbRangingConfigurationData(data);
    }
}