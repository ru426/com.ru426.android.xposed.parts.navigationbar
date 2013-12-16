package com.ru426.android.xposed.parts.navigationbar;

import com.ru426.android.xposed.parts.navigationbar.util.XUtil;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.MenuItem;
import android.widget.Toast;

public class Settings extends PreferenceActivity {
	public static final String PACKAGE_NAME = Settings.class.getPackage().getName();
	private static Context mContext;
	private static SharedPreferences prefs;
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		mContext = this;
		prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		if(prefs.getBoolean(getString(R.string.ru_use_light_theme_key), false)){
			setTheme(android.R.style.Theme_DeviceDefault_Light);
		}
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings_fragment_base);
	    init();
	    initOption();
	}

	@Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch(item.getItemId()){
		case android.R.id.home:
			finish();
			break;
		}
        return super.onMenuItemSelected(featureId, item);
    }
	
	private static void showHomeButton(){
		if(mContext != null && ((Activity) mContext).getActionBar() != null){
			((Activity) mContext).getActionBar().setHomeButtonEnabled(true);
	        ((Activity) mContext).getActionBar().setDisplayHomeAsUpEnabled(true);
		}		
	}
	
	static void showRestartToast(){
		Toast.makeText(mContext, R.string.ru_restart_message, Toast.LENGTH_SHORT).show();
	}

	@SuppressWarnings("deprecation")
	private void init(){
		CheckBoxPreference isHookNavigationbar = (CheckBoxPreference) findPreference(getString(R.string.is_hook_navigationbar_key));
		findPreference(getString(R.string.settings_navigationbar_key_option_key)).setEnabled(isHookNavigationbar.isChecked());
		findPreference(getString(R.string.settings_custom_navigationbar_key)).setEnabled(isHookNavigationbar.isChecked());
		CheckBoxPreference useModNavigationbarView = (CheckBoxPreference) findPreference(getString(R.string.use_mod_navigationbar_view_key));
        PreferenceScreen portrait = (PreferenceScreen) findPreference(getString(R.string.keybuttonview_align_portrait_key));
        PreferenceScreen landscape = (PreferenceScreen) findPreference(getString(R.string.keybuttonview_align_landscape_key));
        PreferenceScreen optional = (PreferenceScreen) findPreference(getString(R.string.keybuttonview_optional_key));
        portrait.setEnabled(useModNavigationbarView.isChecked());
        landscape.setEnabled(useModNavigationbarView.isChecked());
        optional.setEnabled(useModNavigationbarView.isChecked());
	}
	
	@SuppressWarnings("deprecation")
	private void initOption(){
		showHomeButton();
		setPreferenceChangeListener(getPreferenceScreen());
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	private static void setPreferenceChangeListener(PreferenceScreen preferenceScreen){
		for(int i = 0; i < preferenceScreen.getPreferenceCount(); i++){
			if(preferenceScreen.getPreference(i) instanceof PreferenceCategory){
				for(int j = 0; j < ((PreferenceCategory) preferenceScreen.getPreference(i)).getPreferenceCount(); j++){
					((PreferenceCategory) preferenceScreen.getPreference(i)).getPreference(j).setOnPreferenceChangeListener(onPreferenceChangeListener);
				}
			}else{
				preferenceScreen.getPreference(i).setOnPreferenceChangeListener(onPreferenceChangeListener);				
			}
		}
	}
	
	private static OnPreferenceChangeListener onPreferenceChangeListener = new OnPreferenceChangeListener(){
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			PreferenceManager manager = preference.getPreferenceManager();
			switch(preference.getTitleRes()){
			case R.string.is_hook_navigationbar_title:
				if(!prefs.getBoolean(preference.getKey(), false) && (Boolean) newValue){
					showRestartToast();
				}
				XUtil.writeBoolSettingsToDb(mContext, XUtil.IS_HOOK_NAVIGATIONBAR, (Boolean) newValue);
				manager.findPreference(mContext.getString(R.string.settings_navigationbar_key_option_key)).setEnabled((Boolean) newValue);
				manager.findPreference(mContext.getString(R.string.settings_custom_navigationbar_key)).setEnabled((Boolean) newValue);
				break;
			case R.string.menu_key_always_visible_title:
				XUtil.writeBoolSettingsToDb(mContext, XUtil.MENU_KEY_ALWAYS_VISIBLE, (Boolean) newValue);
				break;
			case R.string.expand_menu_key_long_press_title:
				XUtil.writeBoolSettingsToDb(mContext, XUtil.EXPAND_ON_MENU_KEY_LONG_PRESS, (Boolean) newValue);
				break;
			case R.string.expand_recents_key_long_press_title:
				XUtil.writeBoolSettingsToDb(mContext, XUtil.EXPAND_ON_RECENTS_KEY_LONG_PRESS, (Boolean) newValue);
				break;
			case R.string.expand_search_key_long_press_title:
				XUtil.writeBoolSettingsToDb(mContext, XUtil.EXPAND_ON_SEARCH_KEY_LONG_PRESS, (Boolean) newValue);
				break;
			case R.string.use_mod_navigationbar_view_title:
				XUtil.writeBoolSettingsToDb(mContext, XUtil.USE_MOD_NAVIGATIONBAR, (Boolean) newValue);
				if(!prefs.getBoolean(preference.getKey(), false) && (Boolean) newValue){
					Toast.makeText(mContext, R.string.change_nav_setting_caution, Toast.LENGTH_LONG).show();
				}				
				manager.findPreference(mContext.getString(R.string.keybuttonview_align_portrait_key)).setEnabled((Boolean) newValue);
				manager.findPreference(mContext.getString(R.string.keybuttonview_align_landscape_key)).setEnabled((Boolean) newValue);
				manager.findPreference(mContext.getString(R.string.keybuttonview_optional_key)).setEnabled((Boolean) newValue);
				break;
			default:
				return false;
			}
			return true;
		}		
	};
}
