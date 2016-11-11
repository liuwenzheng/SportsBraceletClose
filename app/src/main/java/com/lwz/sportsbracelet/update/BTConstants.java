package com.lwz.sportsbracelet.update;

public class BTConstants {
	// data time pattern
	public static final String PATTERN_HH_MM = "HH:mm";
	public static final String PATTERN_YYYY_MM_DD = "yyyy-MM-dd";
	public static final String PATTERN_MM_DD = "MM/dd";
	public static final String PATTERN_YYYY_MM_DD_HH_MM = "yyyy-MM-dd HH:mm";
	// action
	/**
	 * 广播action
	 */
	// 搜索到的设备信息数据
	public static final String ACTION_BLE_DEVICES_DATA = "action_ble_devices_data";
	public static final String ACTION_BLE_DEVICES_DATA_END = "action_ble_devices_data_end";
	// 发现状态
	public static final String ACTION_DISCOVER_SUCCESS = "action_discover_success";
	public static final String ACTION_DISCOVER_FAILURE = "action_discover_failure";
	// 断开连接
	public static final String ACTION_CONN_STATUS_DISCONNECTED = "action_conn_status_success";
	// 刷新数据
	public static final String ACTION_REFRESH_DATA = "action_refresh_data";

	// 手环应答
	public static final String ACTION_ACK = "action_ack";
	// 连接超时
	public static final String ACTION_CONN_STATUS_TIMEOUT = "action_conn_status_timeout";

	// sp
	public static final String SP_NAME = "sp_name_sportsbracelet";
	public static final String SP_KEY_DEVICE_ADDRESS = "sp_key_device_address";
	public static final String SP_KEY_DEVICE_NAME = "sp_key_device_NAME";
	// Extra_key
	/**
	 * intent传值key
	 */
	// 设备列表
	public static final String EXTRA_KEY_ACK_VALUE = "extra_key_ack_value";

	/**
	 * 返回数据header
	 */
	// ACK
	public static final int HEADER_BACK_ACK = 150;
	// 手环关机
	public static final byte HEADER_CLOSE = 0x15;
}
