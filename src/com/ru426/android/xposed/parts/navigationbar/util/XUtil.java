package com.ru426.android.xposed.parts.navigationbar.util;

import java.util.Map;
import java.util.Set;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.view.View;

import com.ru426.android.xposed.parts.navigationbar.Settings;

public class XUtil {
	public static final String STATE_CHANGE_IS_HOOK_NAVIGATION_BAR = XUtil.class.getPackage().getName() + ".intent.action.IS_HOOK_NAVIGATION_BAR";
	public static final String STATE_CHANGE_USE_MOD_NAVIGATION_BAR = XUtil.class.getPackage().getName() + ".intent.action.USE_MOD_NAVIGATION_BAR";
	public static final String STATE_CHANGE_MENU_KEY_ALWAYS_VISIBLE = XUtil.class.getPackage().getName() + ".intent.action.MENU_KEY_ALWAYS_VISIBLE";
	public static final String STATE_CHANGE_EXPAND_ON_KEY_LONG_PRESS = XUtil.class.getPackage().getName() + ".intent.action.EXPAND_ON_KEY_LONG_PRESS";
	public static final String OPTIONAL_BUTTON_LONG_CLICKED = XUtil.class.getPackage().getName() + ".intent.action.OPTIONAL_BUTTON_LONG_CLICKED";
	public static final String OPTIONAL_BUTTON_CLICKED = XUtil.class.getPackage().getName() + ".intent.action.OPTIONAL_BUTTON_CLICKED";
	
	public static final String IS_HOOK_NAVIGATIONBAR = "is_hook_navigationbar";
	public static final String USE_MOD_NAVIGATIONBAR = "use_mod_navigationbar";
	public static final String MENU_KEY_ALWAYS_VISIBLE = "menu_key_always_visible";
	public static final String EXPAND_ON_MENU_KEY_LONG_PRESS = "expand_on_menu_key_long_press";
	public static final String EXPAND_ON_RECENTS_KEY_LONG_PRESS = "expand_on_recents_key_long_press";
	public static final String EXPAND_ON_SEARCH_KEY_LONG_PRESS = "expand_on_search_key_long_press";
	
	public static final Uri DB_URI = Uri.parse("content://" + Settings.class.getPackage().getName() + ".dbprovider");
	public static int getIntDB(String name, Context context) {
		int result = 0;
		if(context == null) return result;
		try{
			Cursor cursor = context.getContentResolver().query(DB_URI, new String[]{"value"}, "name=?", new String[]{name}, null);
			if(cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()){
				result = cursor.getInt(cursor.getColumnIndex("value"));
			}
			if(cursor != null) cursor.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		return result;
	}
	
	public static void putIntDB(String name, int value, Context context) {
		try {
			if(context.getContentResolver() != null){
				ContentValues values = new ContentValues();
				values.put("name", name);
				values.put("value", value);
				context.getContentResolver().insert(DB_URI, values);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	public static void writeBoolSettingsToDb(Context context, String name, boolean isChecked) {
		int i = isChecked ? 1 : 0;
		putIntDB(name, i, context);
		sendBroadcastIntent(context, name);
	}
	
	private static void sendBroadcastIntent(Context context, String name){
		if(name == null || name.length() == 0) return;
		Intent intent = new Intent();
		if(name.equals(XUtil.MENU_KEY_ALWAYS_VISIBLE)){
			intent.setAction(XUtil.STATE_CHANGE_MENU_KEY_ALWAYS_VISIBLE);
		} else if (name.equals(XUtil.EXPAND_ON_MENU_KEY_LONG_PRESS)
				| name.equals(XUtil.EXPAND_ON_RECENTS_KEY_LONG_PRESS)
				| name.equals(XUtil.EXPAND_ON_SEARCH_KEY_LONG_PRESS)) {
			intent.setAction(XUtil.STATE_CHANGE_EXPAND_ON_KEY_LONG_PRESS);
		} else if (name.equals(XUtil.USE_MOD_NAVIGATIONBAR)) {
			intent.setAction(XUtil.STATE_CHANGE_USE_MOD_NAVIGATION_BAR);
		} else if(name.equals(XUtil.IS_HOOK_NAVIGATIONBAR)){
			intent.setAction(XUtil.STATE_CHANGE_IS_HOOK_NAVIGATION_BAR);
		}
		context.sendBroadcast(intent);
	}
	
	public static Context createModContext(Context context) {
		Context mContext = null;
		try {
			mContext = context.createPackageContext(Settings.PACKAGE_NAME, 3);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return mContext;
	}
	
	public static View makeCustomKeyButtonView(Context context, int layout){
		return View.inflate(createModContext(context), layout, null);
	}
	
	public static void copyPreferences(SharedPreferences source, SharedPreferences target, String sourceKey){
		Map<String, ?> pluginPrefAll = source.getAll();
		if(sourceKey == null || sourceKey.length() == 0){
			Object obj = pluginPrefAll.get(sourceKey);
			copyPreferenceCore(target, sourceKey, obj);
		}else{
			for(String key : pluginPrefAll.keySet()){
				Object obj = pluginPrefAll.get(key);
				copyPreferenceCore(target, key, obj);
			}
		}
	}
	
	private static void copyPreferenceCore(SharedPreferences target, String key, Object obj){
		try{
			boolean value = (Boolean) obj;
			target.edit().putBoolean(key, value).commit();
		}catch(ClassCastException e){
			try{
				int value = (Integer) obj;
				target.edit().putInt(key, value).commit();
			}catch(ClassCastException e1){
				try{
					long value = (Long) obj;
					target.edit().putLong(key, value).commit();
				}catch(ClassCastException e2){
					try{
						float value = (Float) obj;
						target.edit().putFloat(key, value).commit();
					}catch(ClassCastException e3){											
						try{
							String value = (String) obj;
							target.edit().putString(key, value).commit();
						}catch(ClassCastException e4){
							try{
								@SuppressWarnings("unchecked")
								Set<String> value = (Set<String>) obj;
								target.edit().putStringSet(key, value).commit();
							}catch(ClassCastException e5){
							}
						}
					}
				}
			}
		}
	}
	
	public static String getCodeName(int code){
		String result = "";
		switch(code){
		case 0:
			result = "UNKNOWN";
			break;
		case 3:
			result = "HOME";
			break;
		case 4:
			result = "BACK";
			break;
		case 26:
			result = "POWER";
			break;
		case 82:
			result = "MENU";
			break;
		case 84:
			result = "SEARCH";
			break;
		}
		return result;
	}
	
	public static String getKeyById(Context context, int viewId){
		Resources res = context.getResources();
		String packageName = context.getPackageName();
		String key = "";
		if(viewId == res.getIdentifier("menu", "id", packageName)){
			key = "Menu";
		}
		if(viewId == res.getIdentifier("back", "id", packageName)){
			key = "Back";
		}
		if(viewId == res.getIdentifier("home", "id", packageName)){
			key = "Home";
		}
		if(viewId == res.getIdentifier("search", "id", packageName)){
			key = "Search";
		}
		if(viewId == res.getIdentifier("recent_apps", "id", packageName)){
			key = "RecentApps";
		}
		if(viewId == res.getIdentifier("optional", "id", packageName)){
			key = "Optional";
		}
		if(viewId == res.getIdentifier("menu2", "id", packageName)){
			key = "Menu2";
		}
		if(viewId == res.getIdentifier("power", "id", packageName)){
			key = "Power";
		}
		if(viewId == res.getIdentifier("spacer1", "id", packageName)){
			key = "Spacer1";
		}
		if(viewId == res.getIdentifier("spacer2", "id", packageName)){
			key = "Spacer2";
		}
		if(viewId == -1){
			key = "DefaultSpacer";
		}	
		return key;
	}
}
