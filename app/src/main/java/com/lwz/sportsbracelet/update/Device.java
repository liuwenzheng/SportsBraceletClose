package com.lwz.sportsbracelet.update;

import android.os.Parcel;
import android.os.Parcelable;

public class Device implements Parcelable, Comparable<Device> {
    /**
     * 未连接
     */
    public static final int STATUS_CONN_FALSE = 0;
    /**
     * 正在连接
     */
    public static final int STATUS_CONN_ING = 1;
    /**
     * 正在关闭
     */
    public static final int STATUS_CLOSE_ING = 2;
    public String name;
    public String address;
    public int rssi;
    public int status;


    @Override
    public int compareTo(Device another) {
        if (this.rssi > another.rssi) {
            return -1;
        } else if (this.rssi < another.rssi) {
            return 1;
        }
        return 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.name);
        dest.writeString(this.address);
        dest.writeInt(this.rssi);
        dest.writeInt(this.status);
    }

    public Device() {
    }

    protected Device(Parcel in) {
        this.name = in.readString();
        this.address = in.readString();
        this.rssi = in.readInt();
        this.status = in.readInt();
    }

    public static final Creator<Device> CREATOR = new Creator<Device>() {
        @Override
        public Device createFromParcel(Parcel source) {
            return new Device(source);
        }

        @Override
        public Device[] newArray(int size) {
            return new Device[size];
        }
    };
}
