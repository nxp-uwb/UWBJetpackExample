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

import com.jetpackexample.utils.Utils;

import java.io.Serializable;

public class UwbDeviceConfigData implements Serializable {
    public short specVerMajor;
    public short specVerMinor;
    public byte[] chipId = new byte[2];
    public byte[] chipFwVersion = new byte[2];
    public byte[] mwVersion = new byte[3];
    public int supportedUwbProfileIds;
    public byte supportedDeviceRangingRoles;
    public byte[] deviceMacAddress;

    public UwbDeviceConfigData() {

    }

    public UwbDeviceConfigData(short specVerMajor, short specVerMinor, byte[] chipId, byte[] chipFwVersion, byte[] mwVersion, int supportedUwbProfileIds, byte supportedDeviceRangingRoles, byte[] deviceMacAddress) {
        this.specVerMajor = specVerMajor;
        this.specVerMinor = specVerMinor;
        this.chipId = chipId;
        this.chipFwVersion = chipFwVersion;
        this.mwVersion = mwVersion;
        this.supportedUwbProfileIds = supportedUwbProfileIds;
        this.supportedDeviceRangingRoles = supportedDeviceRangingRoles;
        this.deviceMacAddress = deviceMacAddress;
    }

    public short getSpecVerMajor() {
        return specVerMajor;
    }

    public void setSpecVerMajor(short specVerMajor) {
        this.specVerMajor = specVerMajor;
    }

    public short getSpecVerMinor() {
        return specVerMinor;
    }

    public void setSpecVerMinor(short specVerMinor) {
        this.specVerMinor = specVerMinor;
    }

    public byte[] getChipId() {
        return chipId;
    }

    public void setChipId(byte[] chipId) {
        this.chipId = chipId;
    }

    public byte[] getChipFwVersion() {
        return chipFwVersion;
    }

    public void setChipFwVersion(byte[] chipFwVersion) {
        this.chipFwVersion = chipFwVersion;
    }

    public byte[] getMwVersion() {
        return mwVersion;
    }

    public void setMwVersion(byte[] mwVersion) {
        this.mwVersion = mwVersion;
    }

    public int getSupportedUwbProfileIds() {
        return supportedUwbProfileIds;
    }

    public void setSupportedUwbProfileIds(int supportedUwbProfileIds) {
        this.supportedUwbProfileIds = supportedUwbProfileIds;
    }

    public byte getSupportedDeviceRangingRoles() {
        return supportedDeviceRangingRoles;
    }

    public void setSupportedDeviceRangingRoles(byte supportedDeviceRangingRoles) {
        this.supportedDeviceRangingRoles = supportedDeviceRangingRoles;
    }

    public byte[] getDeviceMacAddress() {
        return deviceMacAddress;
    }

    public void setDeviceMacAddress(byte[] deviceMacAddress) {
        this.deviceMacAddress = deviceMacAddress;
    }

    public byte[] toByteArray() {
        byte[] response = null;
        response = Utils.concat(response, Utils.shortToByteArray(this.specVerMajor));
        response = Utils.concat(response, Utils.shortToByteArray(this.specVerMinor));
        response = Utils.concat(response, this.chipId);
        response = Utils.concat(response, this.chipFwVersion);
        response = Utils.concat(response, this.mwVersion);
        response = Utils.concat(response, Utils.intToByteArray(this.supportedUwbProfileIds));
        response = Utils.concat(response, Utils.byteToByteArray(this.supportedDeviceRangingRoles));
        response = Utils.concat(response, this.deviceMacAddress);

        return response;
    }

    public static UwbDeviceConfigData fromByteArray(byte[] data) {
        UwbDeviceConfigData uwbDeviceConfigData = new UwbDeviceConfigData();
        uwbDeviceConfigData.setSpecVerMajor(Utils.byteArrayToShort(Utils.extract(data, 2, 0)));
        uwbDeviceConfigData.setSpecVerMinor(Utils.byteArrayToShort(Utils.extract(data, 2, 2)));
        uwbDeviceConfigData.setChipId(Utils.extract(data, 2, 4));
        uwbDeviceConfigData.setChipFwVersion(Utils.extract(data, 2, 6));
        uwbDeviceConfigData.setMwVersion(Utils.extract(data, 3, 8));
        uwbDeviceConfigData.setSupportedUwbProfileIds(Utils.byteArrayToInt(Utils.extract(data, 4, 11)));
        uwbDeviceConfigData.setSupportedDeviceRangingRoles(Utils.byteArrayToByte(Utils.extract(data, 1, 15)));
        uwbDeviceConfigData.setDeviceMacAddress(Utils.extract(data, 2, 16));

        return uwbDeviceConfigData;
    }
}
