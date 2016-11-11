package com.lwz.sportsbracelet.update;

import android.text.TextUtils;
import android.util.Log;

/**
 * Log组件
 * 
 * @author wenzheng.liu
 * 
 */
public class LogModule {
	// tag
	public static String TAG = "sportsbraceletInit";

	public static boolean debug = true;

	// v
	public static void v(String msg) {
		v(null, msg, null);
	}

	public static void v(String tag, String msg) {
		v(tag, msg, null);
	}

	public static void v(String tag, String msg, Throwable thr) {
		if (!debug) {
			return;
		}
		if (TextUtils.isEmpty(tag)) {
			tag = TAG;
			Log.v(tag, msg, thr);
		} else {
			Log.v(TAG, msg, thr);
			Log.v(tag, msg, thr);
		}
	}

	// i
	public static void i(String msg) {
		i(null, msg, null);
	}

	public static void i(String tag, String msg) {
		i(tag, msg, null);
	}

	public static void i(String tag, String msg, Throwable thr) {
		if (!debug) {
			return;
		}
		if (TextUtils.isEmpty(tag)) {
			tag = TAG;
			Log.i(tag, msg, thr);
		} else {
			Log.i(TAG, msg, thr);
			Log.i(tag, msg, thr);
		}

	}

	// d
	public static void d(String msg) {
		d(null, msg, null);
	}

	public static void d(String tag, String msg) {
		d(tag, msg, null);
	}

	public static void d(String tag, String msg, Throwable thr) {
		if (!debug) {
			return;
		}
		if (TextUtils.isEmpty(tag)) {
			tag = TAG;
			Log.d(tag, msg, thr);
		} else {
			Log.d(TAG, msg, thr);
			Log.d(tag, msg, thr);
		}

	}

	// w
	public static void w(String msg) {
		w(null, msg, null);
	}

	public static void w(String tag, String msg) {
		w(tag, msg, null);
	}

	public static void w(String tag, String msg, Throwable thr) {
		if (!debug) {
			return;
		}
		if (TextUtils.isEmpty(tag)) {
			tag = TAG;
			Log.w(tag, msg, thr);
		} else {
			Log.w(TAG, msg, thr);
			Log.w(tag, msg, thr);
		}
	}

	// e
	public static void e(String msg) {
		e(null, msg, null);
	}

	public static void e(String tag, String msg) {
		e(tag, msg, null);
	}

	public static void e(String tag, String msg, Throwable thr) {
		if (!debug) {
			return;
		}
		if (TextUtils.isEmpty(tag)) {
			tag = TAG;
			Log.e(tag, msg, thr);
		} else {
			Log.e(TAG, msg, thr);
			Log.e(tag, msg, thr);
		}
	}

}
