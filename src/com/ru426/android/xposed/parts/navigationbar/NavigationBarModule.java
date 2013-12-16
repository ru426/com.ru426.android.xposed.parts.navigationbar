package com.ru426.android.xposed.parts.navigationbar;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;

import android.animation.LayoutTransition;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;

import com.ru426.android.xposed.library.ModuleBase;
import com.ru426.android.xposed.library.util.XModUtil;
import com.ru426.android.xposed.parts.navigationbar.util.XUtil;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class NavigationBarModule extends ModuleBase {
	private static final String TAG = NavigationBarModule.class.getSimpleName();

	private static boolean isHookNavigationKeys = false;
	private static boolean useModNavigationKeysAlign = false;
	private static boolean isMenuKeyAlwaysVisible = false;
	private static boolean isExpandEnableMenu = false;
	private static boolean isExpandEnableRecents = false;
	private static boolean isExpandEnableSearch = false;
	
	private static boolean isPerformLongClick = false;
	
	private static Configuration mConfiguration;
	
	private static LinearLayout mNavigationBarView;
	private static View mCurrentView = null;
	private static XLinkedList<XNavButtonInfo> mPortLinkedList = new XLinkedList<XNavButtonInfo>();
	private static XLinkedList<XNavButtonInfo> mLandLinkedList = new XLinkedList<XNavButtonInfo>();
	private static HashMap<String, Integer> mPortLinkedNumList = new HashMap<String, Integer>();
	private static HashMap<String, Integer> mLandLinkedNumList = new HashMap<String, Integer>();
	
	@Override
	public void init(XSharedPreferences prefs, ClassLoader classLoader, boolean isDebug) {
		super.init(prefs, classLoader, isDebug);
		isHookNavigationKeys = (Boolean) xGetValue(prefs, xGetString(R.string.is_hook_navigationbar_key), false);
		useModNavigationKeysAlign = (Boolean) xGetValue(prefs, xGetString(R.string.use_mod_navigationbar_view_key), false);
		isMenuKeyAlwaysVisible = (Boolean) xGetValue(prefs, xGetString(R.string.menu_key_always_visible_key), false);
		isExpandEnableMenu = (Boolean) xGetValue(prefs, xGetString(R.string.expand_menu_key_long_press_key), false);
		isExpandEnableRecents = (Boolean) xGetValue(prefs, xGetString(R.string.expand_recents_key_long_press_key), false);
		isExpandEnableSearch = (Boolean) xGetValue(prefs, xGetString(R.string.expand_search_key_long_press_key), false);
		
		if(!isHookNavigationKeys) return;
		
		final Class<?> xNavigationBarView = XposedHelpers.findClass("com.android.systemui.statusbar.phone.NavigationBarView", classLoader);
		Object callback[] = new Object[3];
		callback[0] = boolean.class;
		callback[1] = boolean.class;
		callback[2] = new XC_MethodReplacement() {			
			@Override
			protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
				try{
					if(!isHookNavigationKeys){
						XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
						return null;
					}
					xLog(TAG + " : " + "replaceHookedMethod setMenuVisibility");
					setCommonObjects(param);
					
					if(isMenuKeyAlwaysVisible){
						final boolean show = (Boolean) param.args[0]; 
						final boolean force = (Boolean) param.args[1];						
						boolean mShowMenu = (Boolean) XposedHelpers.getObjectField(param.thisObject, "mShowMenu");
						if (!force && mShowMenu == show) return null;						
				        mShowMenu = isMenuKeyAlwaysVisible;
				        XposedHelpers.setObjectField(param.thisObject, "mShowMenu", mShowMenu);
				        if(mContext.getResources().getIdentifier("menu", "id",  mContext.getPackageName()) > 0){
				        	mCurrentView.findViewById(mContext.getResources().getIdentifier("menu", "id",  mContext.getPackageName())).setVisibility(mShowMenu ? View.VISIBLE : View.INVISIBLE);
					        if(getCustomKeyButton("Menu2") != null){
					        	if(getCurrentLinkedList().get("Menu2").getView().getVisibility() == View.VISIBLE){
					        		getCustomKeyButton("Menu2").setVisibility(mShowMenu ? View.VISIBLE : View.INVISIBLE);
					        	}
					        }
				        }
				        return null;
					}				
				} catch (Throwable throwable) {
					XposedBridge.log(throwable);
				}
				XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
				return null;
			}
		};
		xHookMethod(xNavigationBarView, "setMenuVisibility", callback, isHookNavigationKeys);
		
		Object callback2[] = new Object[1];
		callback2[0] = new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				super.afterHookedMethod(param);
				try{
					if(!isHookNavigationKeys){
						return;
					}
					xLog(TAG + " : " + "afterHookedMethod onFinishInflate");
					setCommonObjects(param);
					IntentFilter intentFilter = new IntentFilter();
					intentFilter.addAction(XUtil.STATE_CHANGE_USE_MOD_NAVIGATION_BAR);
					intentFilter.addAction(XUtil.STATE_CHANGE_MENU_KEY_ALWAYS_VISIBLE);
					intentFilter.addAction(XUtil.STATE_CHANGE_EXPAND_ON_KEY_LONG_PRESS);
					xRegisterReceiver(mContext, intentFilter);
					
					if(isHookNavigationKeys){
						setUpNavigationBarKeys(mCurrentView);
					}
				} catch (Throwable throwable) {
					XposedBridge.log(throwable);
				}
			}			
		};
		xHookMethod(xNavigationBarView, "onFinishInflate", callback2, isHookNavigationKeys);
		
		Object callback3[] = new Object[3];
		callback3[0] = int.class;
		callback3[1] = boolean.class;
		callback3[2] = new XC_MethodReplacement() {			
			@Override
			protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
				try{
					if(!isHookNavigationKeys){
						XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
						return null;
					}
					xLog(TAG + " : " + "replaceHookedMethod setDisabledFlags");
					setCommonObjects(param);
					int disabledFlags = (Integer) param.args[0];
					boolean force = (Boolean) param.args[1];
					int mDisabledFlags = (Integer) XposedHelpers.getObjectField(param.thisObject, "mDisabledFlags");
											
					if (!force && mDisabledFlags == disabledFlags) {
						setUpNavigationBarKeys(mCurrentView);
						return null;
					}						
					
				    mDisabledFlags = disabledFlags;

				    int mNavigationIconHints = (Integer) XposedHelpers.getObjectField(param.thisObject, "mNavigationIconHints");
					
					final boolean disableHome = ((disabledFlags & 0x200000) != 0);
					final boolean disableRecent = ((disabledFlags & 0x1000000) != 0);
					final boolean disableBack = ((disabledFlags & 0x400000) != 0) && ((mNavigationIconHints & 0x8) == 0);
					
					final boolean disableOprional = getCurrentLinkedList().get("Optional") != null ? getCurrentLinkedList().get("Optional").getView().getVisibility() == View.GONE : true;
					final boolean disablePower = getCurrentLinkedList().get("Power") != null ? getCurrentLinkedList().get("Power").getView().getVisibility() == View.GONE : true;
					final boolean disableSearch = getCurrentLinkedList().get("Search") != null ? getCurrentLinkedList().get("Search").getView().getVisibility() == View.GONE : true;

					
					boolean flag = disableHome && disableRecent && disableBack;
					XposedHelpers.callMethod(param.thisObject, "setSlippery", flag);

					if (mCurrentView != null) {
						int id = mContext.getResources().getIdentifier("nav_buttons", "id",  mContext.getPackageName());
  						ViewGroup navButtons = (ViewGroup) mCurrentView.findViewById(id);
						LayoutTransition lt = navButtons == null ? null : navButtons.getLayoutTransition();
						if (lt != null) {
							lt.disableTransitionType(LayoutTransition.CHANGE_APPEARING
									| LayoutTransition.CHANGE_DISAPPEARING
									| LayoutTransition.APPEARING
									| LayoutTransition.DISAPPEARING);
						}
					}
					//default
					View navButton = (View) XposedHelpers.callMethod(param.thisObject, "getBackButton");
					if(navButton != null) navButton.setVisibility(disableBack ? View.INVISIBLE : View.VISIBLE);
					navButton = (View) XposedHelpers.callMethod(param.thisObject, "getHomeButton");
					if(navButton != null) navButton.setVisibility(disableHome ? View.INVISIBLE : View.VISIBLE);						
					navButton = (View) XposedHelpers.callMethod(param.thisObject, "getRecentsButton");
					if(navButton != null) navButton.setVisibility(disableRecent ? View.INVISIBLE : View.VISIBLE);
					//mod
					navButton = getCustomKeyButton("Optional");
					if(navButton != null) navButton.setVisibility(disableOprional ? View.GONE : mDisabledFlags != 0 ? View.INVISIBLE : View.VISIBLE);						
					navButton = getCustomKeyButton("Power");
					if(navButton != null) navButton.setVisibility(disablePower ? View.GONE : mDisabledFlags != 0 ? View.INVISIBLE : View.VISIBLE);						
					navButton = getCustomKeyButton("Search");						
					if(navButton != null) navButton.setVisibility(disableSearch ? View.GONE : mDisabledFlags != 0 ? View.INVISIBLE : View.VISIBLE);

			        callSetMenuVisibility();
			        
					if (mConfiguration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
						boolean mVertical = (Boolean) XposedHelpers.getObjectField(param.thisObject, "mVertical");
						if (mVertical) {
							Intent intent = new Intent(XUtil.STATE_CHANGE_USE_MOD_NAVIGATION_BAR);
							if(mContext != null) mContext.sendBroadcast(intent);
						}
					}
					return null;
				} catch (Throwable throwable) {
					XposedBridge.log(throwable);
				}
				XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
				return null;
			}
		};
		xHookMethod(xNavigationBarView, "setDisabledFlags", callback3, useModNavigationKeysAlign);
		
		Class<?> xKeyButtonView = XposedHelpers.findClass("com.android.systemui.statusbar.policy.KeyButtonView", classLoader);
		Object callback4[] = new Object[4];
		callback4[0] = int.class;
		callback4[1] = int.class;
		callback4[2] = long.class;
		callback4[3] = new XC_MethodReplacement() {
			@Override
			protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
				try {
					xLog(TAG + " : " + "replaceHookedMethod sendEvent");
					if(xModuleResources != null){
						int flags = (Integer) param.args[1];
						int mCode = (Integer) XposedHelpers.getObjectField(param.thisObject, "mCode");
						String mCodeName = XUtil.getCodeName(mCode);
						ImageView mKeyButtonView = (ImageView) param.thisObject;						
						mContext = mKeyButtonView.getContext();
						if(flags == KeyEvent.FLAG_LONG_PRESS){
							xLog(TAG + " : FLAG_LONG_PRESS [" + flags +"]");
							xLog(TAG + " : codeName is [" + mCodeName +"]");

							if(mCode == 82 && isExpandEnableMenu){
								XModUtil.expandStatusBar(mContext);
								return null;
							}
							if(mCode == 84 && isExpandEnableSearch){
								XModUtil.expandStatusBar(mContext);
								return null;
							}
						}
					}					
				} catch (Throwable throwable) {
					XposedBridge.log(throwable);
				}
				XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
				return null;
			}
		};
		xHookMethod(xKeyButtonView, "sendEvent", callback4, isHookNavigationKeys);
		
		Object callback5[] = new Object[2];
		callback5[0] = MotionEvent.class;
		callback5[1] = new XC_MethodReplacement() {
			@Override
			protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
				try {
					if(!isHookNavigationKeys){
						XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
						return null;
					}
					xLog(TAG + " : " + "replaceHookedMethod onTouchEvent");
					ImageView mKeyButtonView = (ImageView) param.thisObject;
					mContext = mKeyButtonView.getContext();
					final int action = ((MotionEvent) param.args[0]).getAction();
					switch (action) {
		            case MotionEvent.ACTION_UP:
						if(mKeyButtonView.getId() == R.id.optional){
			            	XposedHelpers.setObjectField(param.thisObject, "mSupportsLongpress", true);
			            	XposedHelpers.callMethod(param.thisObject, "setPressed", false);
							isPerformLongClick = false;
							return true;							
						}
						if(isExpandEnableRecents){
							int recent_apps = mKeyButtonView.getContext().getResources().getIdentifier("recent_apps", "id",  mKeyButtonView.getContext().getPackageName());
							if(mKeyButtonView.getId() == recent_apps){
				            	XposedHelpers.setObjectField(param.thisObject, "mSupportsLongpress", true);
				            	XposedHelpers.callMethod(param.thisObject, "setPressed", false);
								isPerformLongClick = false;
								return true;							
							}
						}
						isPerformLongClick = false;
					}
					XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
				} catch (Throwable throwable) {
					XposedBridge.log(throwable);
				}
				return true;
			}
		};
		xHookMethod(xKeyButtonView, "onTouchEvent", callback5, isHookNavigationKeys);
		
		Object callback6[] = new Object[2];
		callback6[0] = float.class;
		callback6[1] = new XC_MethodReplacement() {
			@Override
			protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
				try {
					if(!isHookNavigationKeys){
						XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
						return null;
					}
					xLog(TAG + " : " + "replaceHookedMethod setGlowScale");
					ImageView KeyButtonView = (ImageView) param.thisObject;
					Drawable mGlowBG = (Drawable) XposedHelpers.getObjectField(param.thisObject, "mGlowBG");
					float GLOW_MAX_SCALE_FACTOR = (Float) XposedHelpers.getObjectField(param.thisObject, "GLOW_MAX_SCALE_FACTOR");
					
					if (mGlowBG == null) return null;
			        final float w = KeyButtonView.getWidth();
			        final float h = KeyButtonView.getHeight();
			        if (GLOW_MAX_SCALE_FACTOR <= 1.0f) {
			        	KeyButtonView.invalidate();
			        } else {
			            final float rx = (w * (GLOW_MAX_SCALE_FACTOR - 1.0f)) / 2.0f + 1.0f;
			            final float ry = (h * (GLOW_MAX_SCALE_FACTOR - 1.0f)) / 2.0f + 1.0f;
						invalidateGlobalRegion(KeyButtonView,
								new RectF(KeyButtonView.getLeft() - rx,
										KeyButtonView.getTop() - ry,
										KeyButtonView.getRight() + rx,
										KeyButtonView.getBottom() + ry));
			            if(KeyButtonView.getParent() != null){
			            	 ((View)KeyButtonView.getParent()).invalidate();
			            }			           
			        }
				} catch (Throwable throwable) {
					XposedBridge.log(throwable);
				}
				return null;
			}
		};
		xHookMethod(xKeyButtonView, "setGlowScale", callback6, isHookNavigationKeys);
	}
	
	private static void setCommonObjects(MethodHookParam param){
		mNavigationBarView = (LinearLayout) param.thisObject;
		mCurrentView = (View) XposedHelpers.getObjectField(param.thisObject, "mCurrentView");
		mContext = mCurrentView.getContext();
		mConfiguration = mContext.getResources().getConfiguration();
	}
	
	private static class XNavButtonInfo{
		private String key;
		private View view;
		private boolean isDefault;
		XNavButtonInfo(String key, View view, int position, boolean isDefault){
			this.key = key;
			this.view = view;
			this.isDefault = isDefault;
			getCurrentLinkedNumList().put(this.key, position);
		}
		public String getKey() {
			return key;
		}
		public View getView() {
			return view;
		}
		public boolean isDefaultKeyButtonView() {
			return isDefault;
		}
	}
	
	@SuppressWarnings("hiding")
	private static class XLinkedList<XNavButtonInfo> extends LinkedList<XNavButtonInfo>{
		private static final long serialVersionUID = 1L;
		public XNavButtonInfo get(String key) {
			if(mContext == null) return null;
			int idx = getCurrentLinkedNumList().get(key) == null ? 0 : getCurrentLinkedNumList().get(key);
			try{
				return get(idx);
			}catch(IndexOutOfBoundsException e){}
			return null;
		}
	}
	
	private static XLinkedList<XNavButtonInfo> getCurrentLinkedList(){
		XLinkedList<XNavButtonInfo> mLinkedList = mPortLinkedList;
		try{
			switch(mConfiguration.orientation){
			case Configuration.ORIENTATION_LANDSCAPE:
				mLinkedList = mLandLinkedList;
				break;
			}
		}catch(Exception e){
			XposedBridge.log(e);
		}
		return mLinkedList;
	}
	
	private static HashMap<String, Integer> getCurrentLinkedNumList(){
		HashMap<String, Integer> mLinkedNumList = mPortLinkedNumList;
		try{
			switch(mConfiguration.orientation){
			case Configuration.ORIENTATION_LANDSCAPE:
				mLinkedNumList = mLandLinkedNumList;
				break;
			}			
		}catch(Exception e){
			XposedBridge.log(e);
		}
		return mLinkedNumList;
	}
	
	private static View getCustomKeyButton(String key){
		if(mContext == null) return null;
		int id = xModuleResources.getIdentifier(key.toLowerCase(Locale.ENGLISH), "id", Settings.PACKAGE_NAME);
		if(id <= 0){
			return null;
		}
		return getCurrentLinkedList().get(key) != null ? getCurrentLinkedList().get(key).getView() : null;
	}
	
	@SuppressLint("WorldReadableFiles")
	@SuppressWarnings("deprecation")
	private static SharedPreferences getSharedPreferences() {
		if(mContext == null) return null;
        Context context = null;
        SharedPreferences pref = null;
        String prefName = "NavigationBarPortraitSettings";
        try {
            context = mContext.createPackageContext(Settings.PACKAGE_NAME, Context.CONTEXT_RESTRICTED);
        } catch (NameNotFoundException e) {
            XposedBridge.log(e);
            return null;
        }
        
        switch(mConfiguration.orientation){
		case Configuration.ORIENTATION_LANDSCAPE:
			prefName = "NavigationBarLandscapeSettings";
			break;
		}
		if(prefName.length() == 0) return null;
		pref = context.getSharedPreferences(prefName, Activity.MODE_WORLD_READABLE | Activity.MODE_MULTI_PROCESS);
        return pref;
    }
	
	@SuppressWarnings("deprecation")
	private static void resetNavigationBarKeys(View view) {
		String mTAG = TAG + " : " + "resetNavigationBarKeys ";
		if(mContext == null) {
			XposedBridge.log(mTAG + "mContext is null");
            return;
		}
		if(view == null) {
			XposedBridge.log(mTAG + "mCurrentView is null");
            return;
		}
		int id = mContext.getResources().getIdentifier("nav_buttons", "id",  mContext.getPackageName());
		if(id <= 0){
			XposedBridge.log(mTAG + "nav_buttons is not found");
			return;
		}
		XLinkedList<XNavButtonInfo> mXLinkedList = getCurrentLinkedList();
        ViewGroup group = (ViewGroup) view.findViewById(id);
        if (mXLinkedList.size() == 0) {
        	XposedBridge.log(mTAG + "mXLinkedList size is 0");
        	return;
        }
        
        group.removeAllViews();

        int landDefSpacerLastIndex = 0;
        for (int i = 0; i < mXLinkedList.size(); i++) {
            View currentView = mXLinkedList.get(i).getView();
            if (currentView == null) continue;
            if(currentView.getParent() != null){
            	if(currentView instanceof LinearLayout){
            		LinearLayout wrapper = (LinearLayout) currentView.getParent();
            		wrapper.removeView(currentView);
            	}
            }
            
			if (mXLinkedList.get(i).isDefaultKeyButtonView()) {
				final int FP = LinearLayout.LayoutParams.FILL_PARENT;
				int widthId = -1;
				int menuWidthId = -1;
				try{
					widthId = mContext.getResources().getIdentifier("navigation_key_width", "dimen", mContext.getPackageName());
					menuWidthId = mContext.getResources().getIdentifier("navigation_menu_key_width", "dimen", mContext.getPackageName());					
				}catch(Exception e){
					XposedBridge.log(e);
				}
				int dpWidth = 80;
				if (widthId > 0) dpWidth = (int) mContext.getResources().getDimension(widthId);
				if (mXLinkedList.get(i).getKey().equals("Menu") | mXLinkedList.get(i).getKey().equals("DefaultSpacer")) {
					dpWidth = 40;
					if (menuWidthId > 0) dpWidth = (int) mContext.getResources().getDimension(menuWidthId);
				}
				if(mXLinkedList.get(i).getKey().equals("DefaultSpacer")){
					if(i == 0){							
						currentView.setLayoutParams(new LinearLayout.LayoutParams(dpWidth, FP, 0));	
					}else{
						currentView.setLayoutParams(new LinearLayout.LayoutParams(FP, FP, 1.0f));
					}
				}else{
					currentView.setLayoutParams(new LinearLayout.LayoutParams(dpWidth, FP, 0));						
				}
				switch(mConfiguration.orientation){
				case Configuration.ORIENTATION_LANDSCAPE:
					if(mXLinkedList.get(i).getKey().equals("DefaultSpacer")){
						currentView.setLayoutParams(new LinearLayout.LayoutParams(FP, FP, 1.0f));
						landDefSpacerLastIndex = i;
					}else{
						currentView.setLayoutParams(new LinearLayout.LayoutParams(FP, dpWidth, 0));
					}
					break;
				}
				group.addView(currentView);
				currentView.setVisibility(View.VISIBLE);	
			} else {
            	currentView.setVisibility(View.GONE);
            	continue;
            }			
        }
        switch(mConfiguration.orientation){
		case Configuration.ORIENTATION_LANDSCAPE:
			int dpWidth = 40;
			group.getChildAt(landDefSpacerLastIndex).setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, dpWidth, 0));
			break;
		}
        callSetMenuVisibility();
	}
	
	private static void setUpNavigationBarKeys(View view) {
		String mTAG = TAG + " : " + "setUpNavigationBarKeys ";
		if(mContext == null) {
			XposedBridge.log(mTAG + "mContext is null");
            return;
		}
		if(view == null) {
			XposedBridge.log(mTAG + "mCurrentView is null");
            return;
		}
		int id = mContext.getResources().getIdentifier("nav_buttons", "id",  mContext.getPackageName());
		if(id == 0){
			XposedBridge.log(mTAG + "nav_buttons is not found");
			return;
		}
        SharedPreferences pref = getSharedPreferences();
        if (pref == null) {
        	XposedBridge.log(mTAG + "pref is null");
            return;
        }
        if (pref.getString("keyname1", null) == null) {
        	XposedBridge.log(mTAG + "keyname1 is null");
        	return;
        }
        
		XLinkedList<XNavButtonInfo> mXLinkedList = getCurrentLinkedList();
        ViewGroup group = (ViewGroup) view.findViewById(id);
        if (mXLinkedList.size() == 0) {
        	//set default key button
        	for(int i = 0; i < group.getChildCount(); i++){
        		int thisId = group.getChildAt(i).getId();
        		mXLinkedList.add(new XNavButtonInfo(XUtil.getKeyById(mContext, thisId), group.getChildAt(i), i, true));
        	}        	
        	//set other key button      
            id = mContext.getResources().getIdentifier("search", "id",  mContext.getPackageName());
            if(id <= 0 || group.findViewById(id) == null){
            	mXLinkedList.add(new XNavButtonInfo("Search", XUtil.makeCustomKeyButtonView(mContext, R.layout.xkeybuttonview_search), mXLinkedList.size(), false));
            }
            id = mContext.getResources().getIdentifier("recent_apps", "id",  mContext.getPackageName());
            if(id <= 0 || group.findViewById(id) == null){
            	mXLinkedList.add(new XNavButtonInfo("RecentApps", XUtil.makeCustomKeyButtonView(mContext, R.layout.xkeybuttonview_recent), mXLinkedList.size(), false));
            }            
            id = mContext.getResources().getIdentifier("optional", "id",  mContext.getPackageName());
            if(id <= 0 || group.findViewById(id) == null){
            	View optionalView = XUtil.makeCustomKeyButtonView(mContext, R.layout.xkeybuttonview_optional);
                optionalView.setOnClickListener(mOnClickListener);
                optionalView.setOnLongClickListener(mOnLongClickListener);
            	mXLinkedList.add(new XNavButtonInfo("Optional", optionalView, mXLinkedList.size(), false));
            }            
            id = mContext.getResources().getIdentifier("menu2", "id",  mContext.getPackageName());
            if(id <= 0 || group.findViewById(id) == null){
            	mXLinkedList.add(new XNavButtonInfo("Menu2", XUtil.makeCustomKeyButtonView(mContext, R.layout.xkeybuttonview_menu2), mXLinkedList.size(), false));
            }            
            id = mContext.getResources().getIdentifier("power", "id",  mContext.getPackageName());
            if(id <= 0 || group.findViewById(id) == null){
            	mXLinkedList.add(new XNavButtonInfo("Power", XUtil.makeCustomKeyButtonView(mContext, R.layout.xkeybuttonview_power), mXLinkedList.size(), false));
            }            
            id = mContext.getResources().getIdentifier("spacer1", "id",  mContext.getPackageName());
            if(id <= 0 || group.findViewById(id) == null){
            	mXLinkedList.add(new XNavButtonInfo("Spacer1", XUtil.makeCustomKeyButtonView(mContext, R.layout.xkeybuttonview_spacer1), mXLinkedList.size(), false));
            }            
            id = mContext.getResources().getIdentifier("spacer2", "id",  mContext.getPackageName());
            if(id <= 0 || group.findViewById(id) == null){
            	mXLinkedList.add(new XNavButtonInfo("Spacer2", XUtil.makeCustomKeyButtonView(mContext, R.layout.xkeybuttonview_spacer2), mXLinkedList.size(), false));
            }
            
            if(getCustomKeyButton("RecentApps") != null){
        		View recentsView = getCustomKeyButton("RecentApps");
                recentsView.setOnLongClickListener(mOnLongClickListener);
        	}
        }

        group.removeAllViews();

        @SuppressWarnings("deprecation")
		final int FP = LinearLayout.LayoutParams.FILL_PARENT;
        int showKeyCount = pref.getInt("showKeyCount", 10);
        int keyCount = 10;
        int showedCount = 0;
        for (int i = 0; i < keyCount; i++) {
            String keyName = pref.getString("keyname" + String.valueOf(i), null);
            if (keyName == null) continue;
            View currentView = getCurrentLinkedList().get(keyName).getView();
            if (currentView == null) continue;
            int dpWidth = pref.getInt("size" + String.valueOf(i), -1);
            if (dpWidth < 0) dpWidth = 40;
            final float scale = mContext.getResources().getDisplayMetrics().density;
            int pxWidth = (int)(dpWidth * scale + 0.5f);
			if(keyName.equals("Back")) ((ImageView)currentView).setScaleType(ScaleType.CENTER_INSIDE);
			currentView.setLayoutParams(new LinearLayout.LayoutParams(pxWidth, FP, 0));			
            switch(mConfiguration.orientation){
    		case Configuration.ORIENTATION_LANDSCAPE:
    			currentView.setLayoutParams(new LinearLayout.LayoutParams(FP, pxWidth, 0));
    			break;
    		}
            
            if(currentView.getParent() != null){
            	if(currentView instanceof LinearLayout){
            		LinearLayout wrapper = (LinearLayout) currentView.getParent();
            		wrapper.removeView(currentView);
            	}
            }
            group.addView(currentView);
            boolean show = pref.getBoolean("show" + String.valueOf(i), true);
            currentView.setVisibility(show ? View.VISIBLE : View.GONE);

            if (show)
                showedCount++;
            
            if (showedCount != showKeyCount && show) {
                View padView = new View(mContext);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(FP, FP, (float)1.0);
                padView.setLayoutParams(layoutParams);
                padView.setVisibility(View.INVISIBLE);
                group.addView(padView);
            }
        }
        callSetMenuVisibility();
    }
	
	private static void callSetMenuVisibility(){		
		if(mNavigationBarView != null){
			XposedHelpers.callMethod(mNavigationBarView, "setMenuVisibility", new Object[]{isMenuKeyAlwaysVisible, true});
		}
	}
	
	private static OnLongClickListener mOnLongClickListener =  new OnLongClickListener() {							
		@Override
		public boolean onLongClick(View v) {
			if(v.getId() == R.id.optional){
				Intent intent = new Intent(XUtil.OPTIONAL_BUTTON_LONG_CLICKED);
				v.getContext().sendBroadcast(intent);	
				isPerformLongClick = true;
				return true;
			}
			int recent_apps = mContext.getResources().getIdentifier("recent_apps", "id",  mContext.getPackageName());
			if(v.getId() == recent_apps){
				XModUtil.expandStatusBar(mContext);
				return true;
			}
			return false;
		}
	};
	
	private static OnClickListener mOnClickListener = new OnClickListener() {		
		@Override
		public void onClick(View v) {
			if(!isPerformLongClick){
				Intent intent = new Intent(XUtil.OPTIONAL_BUTTON_CLICKED);
				v.getContext().sendBroadcast(intent);
        	}
			isPerformLongClick = false;
		}
	};
	
	public static void invalidateGlobalRegion(View view, RectF childBounds) {
		while (view.getParent() != null && view.getParent() instanceof View) {
			view = (View) view.getParent();
			view.getMatrix().mapRect(childBounds);
			view.invalidate((int) Math.floor(childBounds.left), (int) Math.floor(childBounds.top), (int) Math.ceil(childBounds.right), (int) Math.ceil(childBounds.bottom));
		}
	}

	@Override
	protected void xOnReceive(Context context, Intent intent) {
		super.xOnReceive(context, intent);
		xLog(TAG + " : " + "OnReceive " + intent.getAction());
		if (intent.getAction().equals(XUtil.STATE_CHANGE_EXPAND_ON_KEY_LONG_PRESS)) {
			isExpandEnableMenu = XUtil.getIntDB(XUtil.EXPAND_ON_MENU_KEY_LONG_PRESS, mContext) == 1;
			isExpandEnableRecents = XUtil.getIntDB(XUtil.EXPAND_ON_RECENTS_KEY_LONG_PRESS, mContext) == 1;
			isExpandEnableSearch = XUtil.getIntDB(XUtil.EXPAND_ON_SEARCH_KEY_LONG_PRESS, mContext) == 1;
		}else if(intent.getAction().equals(XUtil.STATE_CHANGE_USE_MOD_NAVIGATION_BAR)){
			if(mContext != null){
				useModNavigationKeysAlign = XUtil.getIntDB(XUtil.USE_MOD_NAVIGATIONBAR, mContext) == 1;
				if(useModNavigationKeysAlign){
					setUpNavigationBarKeys(mCurrentView);
				}else{
					resetNavigationBarKeys(mCurrentView);
				}
			}
		}else if(intent.getAction().equals(XUtil.STATE_CHANGE_MENU_KEY_ALWAYS_VISIBLE)){
			if(mContext != null){
				isMenuKeyAlwaysVisible = XUtil.getIntDB(XUtil.MENU_KEY_ALWAYS_VISIBLE, mContext) == 1;
				callSetMenuVisibility();				
			}
		}else if(intent.getAction().equals(XUtil.STATE_CHANGE_IS_HOOK_NAVIGATION_BAR)){
			if(mContext != null){
				isHookNavigationKeys = XUtil.getIntDB(XUtil.IS_HOOK_NAVIGATIONBAR, mContext) == 1;
				if(useModNavigationKeysAlign) resetNavigationBarKeys(mCurrentView);
			}
		}
	}
}
