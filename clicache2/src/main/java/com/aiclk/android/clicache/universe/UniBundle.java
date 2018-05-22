package com.aiclk.android.clicache.universe;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public abstract class UniBundle {

	private static final String SHRPREF_PREFIX = "com.iclicash.advlib";
	private static SharedPreferences shrPref = null;
	private static SharedPreferences.Editor shrPrefEditor = null;
	private static final Object ublock = new Object();
	private static final Object mblock = new Object();
	private static Map<String, JSONObject> memMap = new HashMap<>();


	public static boolean isPrepared(
			//
			// public static boolean isPrepared(): Determine if UniBundle had prepared
			//
			// Return value: void
	) {
		return ((shrPref != null) && (shrPrefEditor != null));
	}

	public static void prepare(
			//
			// public static void prepare(): perform prepare of UniBundle
			//
			Context context    // The context object
			// Return value: void
	) {
		synchronized (ublock) {
			shrPref = context.getSharedPreferences(SHRPREF_PREFIX, 0);
			shrPrefEditor = shrPref.edit();
		}
	}

	public static void putValue(
			//
			// public static void putContext(): Put a value into Unibundle (with separate context)
			//
			String ctxName,    // The context name
			String key,        // The key
			Object value    // The value
			// Return value: void
	) {
		if ((!isPrepared()) || ctxName == null) {
			return;
		}
		synchronized (mblock) {
			try {
				JSONObject jobj = memMap.get(ctxName);
				if (jobj == null) {
					jobj = new JSONObject(shrPref.getString(ctxName, "{}"));
				}
				jobj.put(key, value);
				memMap.put(ctxName, jobj);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}

	public static void putString(String key, String value) {
		shrPrefEditor.putString(key,value);
		shrPrefEditor.commit();
	}

	public static String getString(String key, String defaultValue) {
		return shrPref.getString(key,defaultValue);
	}

	public static void remove(String key) {
		shrPrefEditor.remove(key);
		shrPrefEditor.commit();
	}

	public static Object takeValue(
			//
			// public static Object takeValue(): Get a value from unibundle's separate context
			//
			String ctxName,    // The context name
			String key        // The key
			// Return value: Object: The value (could be String, Integer, Long, JSONArray or you can put another JSONObject in here)
	) {
		if ((!isPrepared()) || ctxName == null) {
			return null;
		}
		JSONObject jobj = memMap.get(ctxName);
		if (jobj == null) {
			// Find in sharepreferences ...
			try {
				jobj = new JSONObject(shrPref.getString(ctxName, "{}"));
			} catch (JSONException e) {
				e.printStackTrace();
			}
			if (jobj.length() <= 0) {
				return null;
			} else {
				synchronized (mblock) {
					memMap.put(ctxName, jobj);
				}
			}
		}
		try {
			return jobj.get(key);
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static void sync(
			//
			// public static void sync(): Commit any changes back to disk(sharepreferences)
			//
			// Return value: void
	) {
		synchronized (ublock) {
			for (String key : memMap.keySet()) {
				__unatomicsync(key);
			}
			shrPrefEditor.commit();
		}
	}

	public static void sync(
			//
			// public static void sync(): Commit changes on one context back to disk(sharepreferences)
			//
			String ctxName    // The context name
			// Return value: void
	) {
		synchronized (ublock) {
			__unatomicsync(ctxName);
			shrPrefEditor.commit();
		}
	}

	public static void memflush() {
		synchronized (mblock) {
			sync();
			memMap.clear();
		}
	}

	private static void __unatomicsync(String ctxName) {
		JSONObject jobj = memMap.get(ctxName);
		if (jobj != null) {
			shrPrefEditor.putString(ctxName, jobj.toString());
		}
	}
}

