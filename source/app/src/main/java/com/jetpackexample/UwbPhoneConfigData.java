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

public class UwbPhoneConfigData implements Serializable {
    short specVerMajor;
    short specVerMinor;
    int sessionId;
    byte preambleId;
    byte channel;
    byte profileId;
    byte deviceRangingRole;
    byte[] phoneMacAddress;

    public UwbPhoneConfigData() {

    }

    public UwbPhoneConfigData(short specVerMajor, short specVerMinor, int sessionId, byte preambleId, byte channel, byte profileId, byte deviceRangingRole, byte[] phoneMacAddress) {
        this.specVerMajor = specVerMajor;
        this.specVerMinor = specVerMinor;
        this.sessionId = sessionId;
        this.preambleId = preambleId;
        this.channel = channel;
        this.profileId = profileId;
        this.deviceRangingRole = deviceRangingRole;
        this.phoneMacAddress = phoneMacAddress;
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

    public int getSessionId() {
        return sessionId;
    }

    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
    }

    public byte getPreambleId() {
        return preambleId;
    }

    public void setPreambleId(byte preambleId) {
        this.preambleId = preambleId;
    }

    public byte getChannel() {
        return channel;
    }

    public void setChannel(byte channel) {
        this.channel = channel;
    }

    public byte getProfileId() {
        return profileId;
    }

    public void setProfileId(byte profileId) {
        this.profileId = profileId;
    }

    public byte getDeviceRangingRole() {
        return deviceRangingRole;
    }

    public void setDeviceRangingRole(byte deviceRangingRole) {
        this.deviceRangingRole = deviceRangingRole;
    }

    public byte[] getPhoneMacAddress() {
        return phoneMacAddress;
    }

    public void setPhoneMacAddress(byte[] phoneMacAddress) {
        this.phoneMacAddress = phoneMacAddress;
    }

    public byte[] toByteArray() {
        byte[] response = null;
        response = Utils.concat(response, Utils.shortToByteArray(this.specVerMajor));
        response = Utils.concat(response, Utils.shortToByteArray(this.specVerMinor));
        response = Utils.concat(response, Utils.intToByteArray(this.sessionId));
        response = Utils.concat(response, Utils.byteToByteArray(this.preambleId));
        response = Utils.concat(response, Utils.byteToByteArray(this.channel));
        response = Utils.concat(response, Utils.byteToByteArray(this.profileId));
        response = Utils.concat(response, Utils.byteToByteArray(this.deviceRangingRole));
        response = Utils.concat(response, this.phoneMacAddress);

        return response;
    }

    public static UwbPhoneConfigData fromByteArray(byte[] data) {
        UwbPhoneConfigData uwbPhoneConfigData = new UwbPhoneConfigData();
        uwbPhoneConfigData.setSpecVerMajor(Utils.byteArrayToShort(Utils.extract(data, 2, 0)));
        uwbPhoneConfigData.setSpecVerMinor(Utils.byteArrayToShort(Utils.extract(data, 2, 2)));
        uwbPhoneConfigData.setSessionId(Utils.byteArrayToShort(Utils.extract(data, 4, 4)));
        uwbPhoneConfigData.setPreambleId(Utils.byteArrayToByte(Utils.extract(data, 1, 8)));
        uwbPhoneConfigData.setChannel(Utils.byteArrayToByte(Utils.extract(data, 1, 9)));
        uwbPhoneConfigData.setProfileId(Utils.byteArrayToByte(Utils.extract(data, 1, 10)));
        uwbPhoneConfigData.setDeviceRangingRole(Utils.byteArrayToByte(Utils.extract(data, 1, 11)));
        uwbPhoneConfigData.setPhoneMacAddress(Utils.extract(data, 2, 12));

        return uwbPhoneConfigData;
    }
}
