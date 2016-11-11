package com.lwz.sportsbracelet.update;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class InitActivity extends Activity implements OnClickListener {
    private static final int PERMISSION_REQUEST_CODE = 1;

    @InjectView(R.id.btn_close)
    Button btnClose;
    @InjectView(R.id.btn_refresh)
    Button btnRefresh;
    @InjectView(R.id.et_scan_period)
    EditText etScanPeriod;
    @InjectView(R.id.lv_devices)
    ListView lvDevices;
    @InjectView(R.id.et_filter_name)
    EditText etFilterName;
    @InjectView(R.id.et_over_time)
    EditText etOverTime;
    @InjectView(R.id.et_filter_rssi)
    EditText etFilterRssi;
    @InjectView(R.id.et_filter_version)
    EditText et_filter_version;
    private DeviceAdapter mAdapter;
    private ArrayList<Device> devices;
    private BTService mBtService;
    private ProgressDialog mDialog;
    private HashMap<String, Device> devicesMaps;
    private long mOverTime;
    private int mFilterRssi;
    private String mConnDeviceAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                                Manifest.permission.ACCESS_COARSE_LOCATION}
                        , PERMISSION_REQUEST_CODE);
                return;
            }
        }
        initContentView();
    }

    private void initContentView() {
        setContentView(R.layout.init);
        ButterKnife.inject(this);
        SPUtiles.getInstance(this);
        // 启动蓝牙服务
        // startService(new Intent(this, BTService.class));
        // 初始化蓝牙适配器
        BluetoothManager bluetoothManager = (BluetoothManager) getApplicationContext()
                .getSystemService(Context.BLUETOOTH_SERVICE);
        BTModule.mBluetoothAdapter = bluetoothManager.getAdapter();
        devices = new ArrayList<>();
        devicesMaps = new HashMap<>();
        mAdapter = new DeviceAdapter(this, devices);
        lvDevices.setAdapter(mAdapter);
        // 注册广播接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction(BTConstants.ACTION_BLE_DEVICES_DATA);
        filter.addAction(BTConstants.ACTION_BLE_DEVICES_DATA_END);
        filter.addAction(BTConstants.ACTION_CONN_STATUS_TIMEOUT);
        filter.addAction(BTConstants.ACTION_CONN_STATUS_DISCONNECTED);
        filter.addAction(BTConstants.ACTION_DISCOVER_SUCCESS);
        filter.addAction(BTConstants.ACTION_DISCOVER_FAILURE);
        filter.addAction(BTConstants.ACTION_REFRESH_DATA);
        filter.addAction(BTConstants.ACTION_ACK);
        registerReceiver(mReceiver, filter);
        bindService(new Intent(this, BTService.class), mServiceConnection,
                BIND_AUTO_CREATE);
        btnClose.setEnabled(false);
        etOverTime.setText(SPUtiles.getStringValue("overTime", "10"));
        etFilterName.setText(SPUtiles.getStringValue("filterName", ""));
        etScanPeriod.setText(SPUtiles.getStringValue("scanPeriod", "5"));
        etFilterRssi.setText(SPUtiles.getStringValue("filterRssi", "-100"));
        et_filter_version.setText(SPUtiles.getStringValue("filterVersion", "1.0.0"));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE: {
                for (int i = 0; i < grantResults.length; i++) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        ToastUtils.showToast(InitActivity.this, "This app needs these permissions!");
                        InitActivity.this.finish();
                        return;
                    }
                }
                initContentView();
            }
        }
    }

    @Override
    protected void onDestroy() {
        // 注销广播接收器
        unregisterReceiver(mReceiver);
        mBtService.disConnectBle();
        unbindService(mServiceConnection);
        mBtService = null;
        // stopService(new Intent(this, BTService.class));
        super.onDestroy();
    }

    /**
     * 同步数据
     */
    private void synData() {
        // 5.0偶尔会出现获取不到数据的情况，这时候延迟发送命令，解决问题
        BTService.mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mBtService.synSleep();
            }
        }, 200);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                if (BTConstants.ACTION_BLE_DEVICES_DATA.equals(intent
                        .getAction())) {
                    Device bleDevice = intent.getParcelableExtra("device");
                    String filterName = etFilterName.getText().toString();
                    if (!TextUtils.isEmpty(filterName)
                            && !bleDevice.name.equals(filterName)) {
                        return;
                    }
                    if (Integer.valueOf(bleDevice.rssi) <= mFilterRssi) {
                        return;
                    }
                    if (!TextUtils.isEmpty(et_filter_version.getText().toString())) {
                        byte[] scanRecord = bleDevice.scanRecord;
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < scanRecord.length; i++) {
                            sb.append(Utils.byte2HexString(scanRecord[i]));
                            if (i < scanRecord.length - 1) {
                                sb.append(" ");
                            }
                            if (i % 15 == 1)
                                sb.append("\n");
                        }
                        LogModule.i(sb.toString());
                        int index = 0;
                        for (int i = 0; i < scanRecord.length; i++) {
                            if ("0a".equals(Utils.byte2HexString(scanRecord[i]))
                                    && "ff".equals(Utils.byte2HexString(scanRecord[i + 1]))) {
                                index = i + 8;
                                break;
                            }
                        }
                        if (index == 0) {
                            return;
                        }
                        LogModule.i(index + "");
                        LogModule.i(et_filter_version.getText().toString() + "");
                        String[] s = et_filter_version.getText().toString().split("\\.");
                        if (s.length != 3) {
                            return;
                        }
                        String s1 = Utils.decodeToHex(s[0]);
                        String s2 = Utils.decodeToHex(s[1]);
                        String s3 = Utils.decodeToHex(s[2]);
                        if (s1.length() == 1) {
                            s1 = "0" + s1;
                        }
                        if (s2.length() == 1) {
                            s2 = "0" + s2;
                        }
                        if (s3.length() == 1) {
                            s3 = "0" + s3;
                        }
                        LogModule.i("filter version : " + s1 + "." + s2 + "." + s3);
                        if (s1.equals(Utils.byte2HexString(scanRecord[index]))
                                && s2.equals(Utils.byte2HexString(scanRecord[index + 1]))
                                && s3.equals(Utils.byte2HexString(scanRecord[index + 2]))) {
                            if (!devicesMaps.containsKey(bleDevice.address)) {
                                devicesMaps.put(bleDevice.address, bleDevice);
                                devices.add(bleDevice);
                            } else {
                                return;
                            }
                            Collections.sort(devices);
                            mAdapter.setDevices(devices);
                            mAdapter.notifyDataSetChanged();
                            if (devices.size() >= 30) {
                                mBtService.stopLeScan();
                            }
                        }
                    } else {
                        if (!devicesMaps.containsKey(bleDevice.address)) {
                            devicesMaps.put(bleDevice.address, bleDevice);
                            devices.add(bleDevice);
                        } else {
                            return;
                        }
                        Collections.sort(devices);
                        mAdapter.setDevices(devices);
                        mAdapter.notifyDataSetChanged();
                        if (devices.size() >= 30) {
                            mBtService.stopLeScan();
                        }
                    }
                }
                if (BTConstants.ACTION_BLE_DEVICES_DATA_END.equals(intent
                        .getAction())) {
                    mAdapter.notifyDataSetChanged();
                    if (mDialog != null) {
                        mDialog.dismiss();
                    }
                    if (devices.isEmpty()) {
                        btnRefresh.setEnabled(true);
                        btnClose.setEnabled(false);
                        return;
                    }
                    btnRefresh.setEnabled(true);
                    btnClose.setEnabled(true);
                }
                if (BTConstants.ACTION_CONN_STATUS_TIMEOUT.equals(intent
                        .getAction())
                        || BTConstants.ACTION_CONN_STATUS_DISCONNECTED
                        .equals(intent.getAction())
                        || BTConstants.ACTION_DISCOVER_FAILURE.equals(intent
                        .getAction())) {
                    if (devices.isEmpty())
                        return;
                    Device device = devices.get(0);
                    if (devicesMaps.containsKey(device.address)) {
                        removeDevice();
                        mAdapter.notifyDataSetChanged();
                        LogModule.i("配对失败...");
                        ToastUtils.showToast(InitActivity.this, "配对失败");
                        if (mDialog != null)
                            mDialog.dismiss();
                        // 关闭手环并删除
                        connDevice();
                    }
                }
                if (BTConstants.ACTION_DISCOVER_SUCCESS.equals(intent
                        .getAction())) {
                    if (devices.isEmpty())
                        return;
                    Device device = devices.get(0);
                    if (devicesMaps.containsKey(device.address)) {
                        mAdapter.notifyDataSetChanged();
                        // 连接超时
                        LogModule.i("配对成功...");
                        ToastUtils.showToast(InitActivity.this, "配对成功");
                        mConnDeviceAddress = device.address;
                        if (mDialog != null)
                            mDialog.dismiss();
                        // 关闭手环
                        closeDevice(device);
                        device.status = Device.STATUS_CLOSE_ING;
                        mAdapter.notifyDataSetChanged();
                        synData();
                    }
                }
                if (BTConstants.ACTION_ACK.equals(intent.getAction())) {
                    int ack = intent.getIntExtra(
                            BTConstants.EXTRA_KEY_ACK_VALUE, 0);
                    if (ack == 0) {
                        return;
                    }
                    if (ack == 0x22) {
                        if (devices.isEmpty())
                            return;
                        Device device = devices.get(0);
                        if (devicesMaps.containsKey(device.address)) {
                            removeDevice();
                            mAdapter.notifyDataSetChanged();
                            LogModule.i("发送命令成功...");
                            ToastUtils.showToast(InitActivity.this, "关闭成功");
                            if (mDialog != null)
                                mDialog.dismiss();
                            // 关闭手环并删除
                            connDevice();
                        }
                    }
                }
            }

        }
    };
    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LogModule.d("连接服务onServiceConnected...");
            mBtService = ((BTService.LocalBinder) service).getService();
            if (mBtService.mBluetoothGatt != null) {
                mBtService.disConnectBle();
            }
            // 开启蓝牙
            if (!BTModule.isBluetoothOpen()) {
                BTModule.openBluetooth(InitActivity.this);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            LogModule.d("断开服务onServiceDisconnected...");
            mBtService = null;
        }
    };

    @OnClick({R.id.btn_close, R.id.btn_refresh})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_close:
                btnClose.setEnabled(false);
                String overTime = etOverTime.getText().toString();
                String filterName = etFilterName.getText().toString();
                SPUtiles.setStringValue("overTime", overTime);
                SPUtiles.setStringValue("filterName", filterName);
                mOverTime = TextUtils.isEmpty(overTime) ? 2 * 1000 : Integer.parseInt(overTime) * 1000;
                connDevice();
                break;
            case R.id.btn_refresh:
                mDialog = ProgressDialog.show(InitActivity.this, null,
                        getString(R.string.scan_device), false, false);
                devicesMaps.clear();
                devices.clear();
                mAdapter.notifyDataSetChanged();
                // 扫描设备s
                mBtService.scanDevice(Integer.parseInt(etScanPeriod.getText().toString()));
                btnRefresh.setEnabled(false);
                SPUtiles.setStringValue("scanPeriod", etScanPeriod.getText().toString());
                String filterRssi = etFilterRssi.getText().toString();
                SPUtiles.setStringValue("filterRssi", filterRssi);
                String filterVersion = et_filter_version.getText().toString();
                SPUtiles.setStringValue("filterVersion", filterVersion);
                mFilterRssi = TextUtils.isEmpty(filterRssi) ? -1000 : Integer.parseInt(filterRssi);
                break;
        }
    }

    private void connDevice() {
        mBtService.disConnectBle();
        if (devices.isEmpty())
            return;
        mDialog = ProgressDialog.show(InitActivity.this, null,
                getString(R.string.match_device), false, false);
        final Device device = devices.get(0);
        mBtService.connectBle(device.address);
        device.status = Device.STATUS_CONN_ING;
        mAdapter.notifyDataSetChanged();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (devices.isEmpty())
                            return;
                        if (devicesMaps.containsKey(device.address) && !device.address.equals(mConnDeviceAddress)) {
                            removeDevice();
                            mAdapter.notifyDataSetChanged();
                            // 连接超时
                            LogModule.i("连接超时...");
                            ToastUtils.showToast(InitActivity.this, "连接超时");
                            if (mDialog != null)
                                mDialog.dismiss();
                            // 关闭手环并删除
                            connDevice();
                        }
                    }
                });
            }
        }, mOverTime);
    }

    private void closeDevice(final Device device) {
        mDialog = ProgressDialog.show(InitActivity.this, null,
                getString(R.string.close_device), false, false);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (devices.isEmpty())
                            return;
                        if (devicesMaps.containsKey(device.address)) {
                            removeDevice();
                            mAdapter.notifyDataSetChanged();
                            // 连接超时
                            LogModule.i("关闭超时...");
                            ToastUtils.showToast(InitActivity.this, "关闭超时");
                            if (mDialog != null)
                                mDialog.dismiss();
                            // 关闭手环并删除
                            connDevice();
                        }
                    }
                });
            }
        }, mOverTime);
    }


    private synchronized void removeDevice() {
        Device device = devices.get(0);
        devices.remove(device);
        devicesMaps.remove(device.address);
    }
}
