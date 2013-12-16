package net.virifi.android.navigationbarsettings;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.ru426.android.xposed.parts.navigationbar.R;
import com.ru426.android.xposed.parts.navigationbar.util.XUtil;

public class NavigationBarKeySettingPortrait extends Activity {
	public static final String PREF_KEY = "NavigationBarPortraitSettings"; 
	
	String[] prefs = { "Menu", "Back", "Home", "Search", "RecentApps", "Optional", "Menu2", "Power", "Spacer1", "Spacer2"};
	int[] resids = {R.drawable.ic_sysbar_menu, R.drawable.ic_sysbar_back, R.drawable.ic_sysbar_home, R.drawable.ic_sysbar_search, R.drawable.ic_sysbar_recent, R.drawable.ic_sysbar_menu, R.drawable.ic_sysbar_menu, R.drawable.ic_sysbar_power, 0, 0};
	boolean[] checked = {true, true, true, true, true, true, false, false, false, false};
	int[] spinnerSelected = { 0, 1, 1, 1, 1, 0, 0, 1, 0, 0};
	boolean[] initialized = new boolean[prefs.length];
	
	static final int[] spinnerSize = { 40, 60, 80 };

	int mDraggingPosition = -1;
	SampleAdapter mAdapter;
	SortableListView mListView;
	boolean mIsDragging = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.ru_use_light_theme_key), false)){
			setTheme(android.R.style.Theme_DeviceDefault_Light);
		}
	    super.onCreate(savedInstanceState);
	    for (int i = 0; i < prefs.length; i++) {
	    	initialized[i] = false;
	    }
	    setContentView(R.layout.navigationbarkeysettings_keyalign);
	    mAdapter = new SampleAdapter();
	    mListView = (SortableListView) findViewById(R.id.listView);
	    mListView.setDragListener(new DragListener());
	    mListView.setSortable(true);
	    mListView.setAdapter(mAdapter);
	    loadKeySettings();
	    View container = findViewById(R.id.button_container);
	    Button saveButton = (Button) container.findViewById(R.id.saveButton);
	    saveButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				saveAndNotify();
			}
	    });
	    Button resetButton = (Button) container.findViewById(R.id.resetButton);
	    resetButton.setOnClickListener(new OnClickListener() {
	    	@Override
	    	public void onClick(View v) {
	    		loadKeySettings();
	    		mListView.invalidateViews();
	    	}
	    });
	    showHomeButton();
	}
	
	private void showHomeButton(){
		if(getActionBar() != null){
			getActionBar().setHomeButtonEnabled(true);
	        getActionBar().setDisplayHomeAsUpEnabled(true);
		}		
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
	
	private void saveAndNotify() {
		storeKeySettings();
		Intent intent = new Intent(XUtil.STATE_CHANGE_USE_MOD_NAVIGATION_BAR);
		sendBroadcast(intent);
	}
	
	@SuppressLint("WorldReadableFiles")
	private void loadKeySettings() {
		@SuppressWarnings("deprecation")
		SharedPreferences pref = getSharedPreferences(PREF_KEY, Activity.MODE_WORLD_READABLE | Activity.MODE_MULTI_PROCESS);
		if (pref == null || pref.getString("keyname1", null) == null) {
			return;
		}
		String[] prefsOrig = new String[prefs.length];
		boolean[] checkedOrig = new boolean[checked.length];
		int[] residsOrig = new int[resids.length];
		int[] spinnerSelectedOrig = new int[spinnerSelected.length];
		for (int i = 0; i < prefs.length; i++) {
			prefsOrig[i] = new String(prefs[i]);
			checkedOrig[i] = checked[i];
			residsOrig[i] = resids[i];
			spinnerSelectedOrig[i] = spinnerSelected[i];
		}
		boolean failed = false;
		for (int i = 0; i < prefs.length; i++) {
			prefs[i] = pref.getString("keyname" + String.valueOf(i), null);
			if (prefs[i] == null) {
				failed = true;
				break;
			}
			checked[i] = pref.getBoolean("show" + String.valueOf(i), true);
			resids[i] = getResId(prefs[i]);
			int size = pref.getInt("size" + String.valueOf(i), -1);
			if (size < 0) {
				failed = true;
				break;
			}
			for (int j = 0; j < spinnerSize.length; j++) {
				if (size == spinnerSize[j]) {
					spinnerSelected[i] = j;
					break;
				}
			}
		}
		if (failed) {
			prefs = prefsOrig;
			checked = checkedOrig;
			resids = residsOrig;
			spinnerSelected = spinnerSelectedOrig;
		}
	}
	
	private int getResId(String keyName) {
		if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.ru_use_light_theme_key), false)){
			if ("Menu".equals(keyName)) {
				return R.drawable.ic_sysbar_menu_inverse;
			} else if ("Back".equals(keyName)) {
				return R.drawable.ic_sysbar_back_inverse;
			} else if ("Home".equals(keyName)) {
				return R.drawable.ic_sysbar_home_inverse;
			} else if ("Search".equals(keyName)) {
				return R.drawable.ic_sysbar_search_inverse;
			} else if ("RecentApps".equals(keyName)) {
				return R.drawable.ic_sysbar_recent_inverse;
			} else if ("Optional".equals(keyName)) {
				return R.drawable.ic_sysbar_menu_inverse;	
			} else if ("Menu2".equals(keyName)) {
				return R.drawable.ic_sysbar_menu_inverse;
			} else if ("Power".equals(keyName)) {
				return R.drawable.ic_sysbar_power_inverse;
			} else {
				return 0;
			}
		}else{
			if ("Menu".equals(keyName)) {
				return R.drawable.ic_sysbar_menu;
			} else if ("Back".equals(keyName)) {
				return R.drawable.ic_sysbar_back;
			} else if ("Home".equals(keyName)) {
				return R.drawable.ic_sysbar_home;
			} else if ("Search".equals(keyName)) {
				return R.drawable.ic_sysbar_search;
			} else if ("RecentApps".equals(keyName)) {
				return R.drawable.ic_sysbar_recent;
			} else if ("Optional".equals(keyName)) {
				return R.drawable.ic_sysbar_menu;	
			} else if ("Menu2".equals(keyName)) {
				return R.drawable.ic_sysbar_menu;
			} else if ("Power".equals(keyName)) {
				return R.drawable.ic_sysbar_power;
			} else {
				return 0;
			}
		}		
	}
	
	@SuppressLint("WorldReadableFiles")
	private void storeKeySettings() {
		@SuppressWarnings("deprecation")
		SharedPreferences pref = getSharedPreferences(PREF_KEY, Activity.MODE_WORLD_READABLE | Activity.MODE_MULTI_PROCESS);
		if (pref == null) {
			Toast.makeText(this, getString(R.string.cant_save_message), Toast.LENGTH_LONG).show();
		}
		SharedPreferences.Editor editor = pref.edit();
		if (editor == null) {
			Toast.makeText(this, getString(R.string.cant_save_message), Toast.LENGTH_LONG).show();
		}
		int showKeyCount = 0;
		for (int i = 0; i < prefs.length; i++) {
			editor.putString("keyname" + String.valueOf(i), prefs[i]);
			editor.putBoolean("show" + String.valueOf(i), checked[i]);
			editor.putInt("size" + String.valueOf(i), spinnerSize[spinnerSelected[i]]);
			if (checked[i]) showKeyCount++;
		}
		editor.putInt("showKeyCount", showKeyCount);
		editor.commit();
		Toast.makeText(this, getString(R.string.saved), Toast.LENGTH_SHORT).show();
	}

	class SampleAdapter extends BaseAdapter {
	    @Override
	    public int getCount() {
	        return prefs.length;
	    }
	    
	    @Override
	    public String getItem(int position) {
	        return prefs[position];
	    }
	    
	    @Override
	    public long getItemId(int position) {
	        return position;
	    }
	    
	    @Override
	    public View getView(int position, View convertView, ViewGroup parent) {
	        if (convertView == null) {
	            convertView = getLayoutInflater().inflate(R.layout.navigationbarkeysettings_list_view_item, null);
	        }
	        final TextView view = (TextView) convertView.findViewById(R.id.key_name);
	        final String keyName = prefs[position];
	        view.setText(keyName);
	        view.setVisibility(position == mDraggingPosition ? View.INVISIBLE : View.VISIBLE);
	        final ImageView imgView = (ImageView) convertView.findViewById(R.id.imageView1);
	       
	        imgView.setImageResource(resids[position]);
	        imgView.setVisibility(position == mDraggingPosition ? View.INVISIBLE : View.VISIBLE);
	        final CheckBox checkBox = (CheckBox) convertView.findViewById(R.id.checkBox1);
	        checkBox.setVisibility(position == mDraggingPosition ? View.INVISIBLE : View.VISIBLE);
	        
	        final int p = position;
    		
    		checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView,
						boolean isChecked) {
					if (keyName.equals("Home") && !isChecked) {
						checkBox.setChecked(true);
						Toast.makeText(getApplicationContext(), getString(R.string.cant_disable_message, "Home"), Toast.LENGTH_LONG).show();
						return;
					} else if (keyName.equals("Back") && !isChecked) {
						checkBox.setChecked(true);
						Toast.makeText(getApplicationContext(), getString(R.string.cant_disable_message, "Back"), Toast.LENGTH_LONG).show();
						return;
					}
					checked[p] = isChecked;
					/*
					if (!mIsDragging)
						saveAndNotify();
						*/
						
				}

    		});
    		checkBox.setChecked(checked[position]);
    		
    		boolean isLightTheme = PreferenceManager.getDefaultSharedPreferences(NavigationBarKeySettingPortrait.this).getBoolean(getString(R.string.ru_use_light_theme_key), false);
    		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), isLightTheme ? R.layout.spinner_item_inverse : R.layout.spinner_item);
	        adapter.setDropDownViewResource(isLightTheme ? R.layout.spinner_dropdown_item_inverse : R.layout.spinner_dropdown_item);
	        	        
	        String[] spinnerList = getApplicationContext().getResources().getStringArray(R.array.navbar_settings_spinner_value_list);
	        for(String value : spinnerList){
	        	adapter.add(value);
	        }
	        Spinner spinner = (Spinner) convertView.findViewById(R.id.spinner1);
	        spinner.setAdapter(adapter);
	        spinner.setSelection(spinnerSelected[p]);
	        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
	            @Override
	            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

	            	if (position == spinnerSelected[p]) return;
	            	
	            	spinnerSelected[p] = position;
	            }
	            @Override
	            public void onNothingSelected(AdapterView<?> arg0) {
	            }
	        });
	        spinner.setVisibility(position == mDraggingPosition ? View.INVISIBLE : View.VISIBLE);
	        spinner.setFocusable(false);
    		
	       return convertView;
	    }
	}
    
    
    class DragListener extends SortableListView.SimpleDragListener {
        @Override
        public int onStartDrag(int position) {
            mDraggingPosition = position;
            mListView.invalidateViews();
            mIsDragging = true;
            return position;
        }
        
        @Override
        public int onDuringDrag(int positionFrom, int positionTo) {
            if (positionFrom < 0 || positionTo < 0
                    || positionFrom == positionTo) {
                return positionFrom;
            }
            int i;
            if (positionFrom < positionTo) {
                final int min = positionFrom;
                final int max = positionTo;
                final String data = prefs[min];
                final int resId = resids[min];
                final boolean isChecked = checked[min];
                final int spinnerSel = spinnerSelected[min];
                i = min;
                while (i < max) {
                    prefs[i] = prefs[i+1];
                    resids[i] = resids[i+1];
                    checked[i] = checked[i+1];
                    spinnerSelected[i] = spinnerSelected[i+1];
                    i++;
                }
                prefs[max] = data;
                resids[max] = resId;
                checked[max] = isChecked;
                spinnerSelected[max] = spinnerSel;
            } else if (positionFrom > positionTo) {
                final int min = positionTo;
                final int max = positionFrom;
                final String data = prefs[max];
                final int resId = resids[max];
                final boolean isChecked = checked[max];
                final int spinnerSel = spinnerSelected[max];
                i = max;
                while (i > min) {
                    prefs[i] = prefs[i-1];
                    resids[i] = resids[i-1];
                    checked[i] = checked[i-1];
                    spinnerSelected[i] = spinnerSelected[i-1];
                    i--;
                }
                prefs[min] = data;
                resids[min] = resId;
                checked[min] = isChecked;
                spinnerSelected[min] = spinnerSel;
            }
            mDraggingPosition = positionTo;
            mListView.invalidateViews();
            return positionTo;
        }
        
        @Override
        public boolean onStopDrag(int positionFrom, int positionTo) {
            mDraggingPosition = -1;
            mListView.invalidateViews();
            mIsDragging = false;
            return super.onStopDrag(positionFrom, positionTo);
        }
    }
}
