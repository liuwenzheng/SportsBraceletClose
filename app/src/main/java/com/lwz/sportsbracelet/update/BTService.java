package com.lwz.sportsbracelet.update;

import android.app.Service;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

public class BTService extends Service implements LeScanCallback {
    private boolean mIsStartScan = false;
    private long scanPeriod = 5000;
    private static final int GATT_ERROR_TIMEOUT = 133;

    public static Handler mHandler;
    // private ArrayList<BleDevice> mDevices;
    public BluetoothGatt mBluetoothGatt;
    private BluetoothGattCallback mGattCallback;

    @Override
    public void onCreate() {
        mHandler = new Handler(getApplication().getMainLooper());
        LogModule.d("创建BTService...onCreate");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogModule.d("启动BTService...onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    private IBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        LogModule.d("绑定BTService...onBind");
        return mBinder;
    }

    /**
     * 搜索手环
     */
    public void scanDevice(int scanPeriod) {
        if (!mIsStartScan) {
            mIsStartScan = true;
            this.scanPeriod = scanPeriod * 1000;
            BTModule.scanDevice(this);
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mIsStartScan) {
                        LogModule.i(BTService.this.scanPeriod / 1000 + "s后停止扫描");
                        stopLeScan();
                    }
                }
            }, this.scanPeriod);
        } else {
            LogModule.i("正在扫描中...");
        }

    }

    public void stopLeScan() {
        BTModule.mBluetoothAdapter.stopLeScan(BTService.this);
        mIsStartScan = false;
        Intent intent = new Intent(BTConstants.ACTION_BLE_DEVICES_DATA_END);
        // intent.putExtra("devices", mDevices);
        sendBroadcast(intent);
    }

    /**
     * 连接手环
     */
    public void connectBle(String address) {
        final BluetoothDevice device = BTModule.mBluetoothAdapter
                .getRemoteDevice(address);
        if (device == null) {
            return;
        } else {
            mGattCallback = new BluetoothGattCallback() {
                // private int count;

                public void onConnectionStateChange(BluetoothGatt gatt,
                                                    int status, int newState) {
                    super.onConnectionStateChange(gatt, status, newState);
                    LogModule.d("onConnectionStateChange...status:" + status
                            + "...newState:" + newState);
                    switch (newState) {
                        case BluetoothProfile.STATE_CONNECTED:
                            if (status == GATT_ERROR_TIMEOUT) {
                                disConnectBle();
                                Intent intent = new Intent(
                                        BTConstants.ACTION_CONN_STATUS_TIMEOUT);
                                sendBroadcast(intent);
                            } else {
                                if (mBluetoothGatt == null) {
                                    disConnectBle();
                                    Intent intent = new Intent(
                                            BTConstants.ACTION_CONN_STATUS_TIMEOUT);
                                    sendBroadcast(intent);
                                    return;
                                }
                                mBluetoothGatt.discoverServices();
                            }
                            break;
                        case BluetoothProfile.STATE_DISCONNECTED:
                            disConnectBle();
                            Intent intent = new Intent(
                                    BTConstants.ACTION_CONN_STATUS_DISCONNECTED);
                            sendBroadcast(intent);
                            break;
                    }
                }

                ;

                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    super.onServicesDiscovered(gatt, status);
                    LogModule.d("onServicesDiscovered...status:" + status);
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        BTModule.setCharacteristicNotify(mBluetoothGatt);
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Intent intent = new Intent(
                                        BTConstants.ACTION_DISCOVER_SUCCESS);
                                sendBroadcast(intent);
                            }
                        }, 1000);
                    } else {
                        Intent intent = new Intent(
                                BTConstants.ACTION_DISCOVER_FAILURE);
                        sendBroadcast(intent);
                    }
                }

                ;

                public void onCharacteristicRead(BluetoothGatt gatt,
                                                 BluetoothGattCharacteristic characteristic, int status) {
                    super.onCharacteristicRead(gatt, characteristic, status);
                    LogModule.d("onCharacteristicRead...");
                }

                ;

                public void onCharacteristicWrite(BluetoothGatt gatt,
                                                  BluetoothGattCharacteristic characteristic, int status) {
                    super.onCharacteristicWrite(gatt, characteristic, status);
                    LogModule.d("onCharacteristicWrite...");
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        LogModule.d("onCharacteristicWrite...success");
                    } else {
                        LogModule.d("onCharacteristicWrite...failure");
                    }
                }

                ;

                public void onCharacteristicChanged(BluetoothGatt gatt,
                                                    BluetoothGattCharacteristic characteristic) {
                    super.onCharacteristicChanged(gatt, characteristic);
                    LogModule.d("onCharacteristicChanged...");
                    byte[] data = characteristic.getValue();
                    String[] formatDatas = Utils.formatData(data,
                            characteristic);
                    int header = Integer.valueOf(Utils
                            .decodeToString(formatDatas[0]));
                    if (header == BTConstants.HEADER_BACK_ACK) {
                        int ack = Integer.valueOf(Utils
                                .decodeToString(formatDatas[1]));
                        Intent intent = new Intent(BTConstants.ACTION_ACK);
                        intent.putExtra(BTConstants.EXTRA_KEY_ACK_VALUE, ack);
                        BTService.this.sendBroadcast(intent);
                        return;
                    }
                }
            };
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBluetoothGatt = device.connectGatt(BTService.this, false,
                            mGattCallback);
                }
            }, 1000);

        }
    }

    /**
     * 断开手环
     */
    public void disConnectBle() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
            BTModule.mNotifyCharacteristic = null;
        }
    }

    @Override
    public void onLeScan(final BluetoothDevice device, int rssi,
                         byte[] scanRecord) {
        if (device != null) {
            if (Utils.isEmpty(device.getName())) {
                return;
            }
            LogModule.i(device.getName());

            Device bleDevice = new Device();
            bleDevice.name = device.getName();
            bleDevice.address = device.getAddress();
            bleDevice.rssi = rssi;
            bleDevice.status = Device.STATUS_CONN_FALSE;
            bleDevice.scanRecord = scanRecord;
            Intent intent = new Intent(BTConstants.ACTION_BLE_DEVICES_DATA);
            intent.putExtra("device", bleDevice);
            sendBroadcast(intent);
            // mDevices.add(bleDevice);
        }
    }

    /**
     * 休眠模式
     */
    public void synSleep() {
        BTModule.setSleep(mBluetoothGatt);
    }

    /**
     * 是否连接手环
     *
     * @return
     */
    public boolean isConnDevice() {
        BluetoothManager bluetoothManager = (BluetoothManager) getApplicationContext()
                .getSystemService(Context.BLUETOOTH_SERVICE);
        int connState = bluetoothManager
                .getConnectionState(BTModule.mBluetoothAdapter
                                .getRemoteDevice(SPUtiles.getStringValue(
                                        BTConstants.SP_KEY_DEVICE_ADDRESS, null)),
                        BluetoothProfile.GATT);
        if (connState == BluetoothProfile.STATE_CONNECTED) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        LogModule.d("解绑BTService...onUnbind");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        LogModule.d("销毁BTService...onDestroy");
        disConnectBle();
        super.onDestroy();
    }

    public class LocalBinder extends Binder {
        public BTService getService() {
            return BTService.this;
        }
    }
}
