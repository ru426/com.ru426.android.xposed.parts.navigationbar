package com.ru426.android.xposed.parts.navigationbar.view;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.hardware.input.InputManager;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;

import com.ru426.android.xposed.parts.navigationbar.R;

public class XKeyButtonView extends ImageView {
//	private static final String TAG = "StatusBar.KeyButtonView";
	private Context mContext;
	final float GLOW_MAX_SCALE_FACTOR = 1.8f;
	final float BUTTON_QUIESCENT_ALPHA = 0.70f;

	long mDownTime;
	int mCode;
	int mTouchSlop;
	Drawable mGlowBG;
	int mGlowWidth, mGlowHeight;
	float mGlowAlpha = 0f, mGlowScale = 1f, mDrawingAlpha = 1f;
	boolean mSupportsLongpress = true;
	RectF mRect = new RectF(0f, 0f, 0f, 0f);
	AnimatorSet mPressedAnim;

	Runnable mCheckLongPress = new Runnable() {
		public void run() {
			if (isPressed()) {
				// Slog.d("KeyButtonView", "longpressed: " + this);
				if (mCode != 0) {
					sendEvent(KeyEvent.ACTION_DOWN, KeyEvent.FLAG_LONG_PRESS);
					sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_LONG_CLICKED);
				} else {
					// Just an old-fashioned ImageView
					performLongClick();
				}
			}
		}
	};
	
	public XKeyButtonView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
		mContext = context;
	}

	public XKeyButtonView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs);
		mContext = context;
		
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.XKeyButtonView, defStyle, 0);

		mCode = a.getInteger(R.styleable.XKeyButtonView_keyCode, 0);

		mSupportsLongpress = a.getBoolean(R.styleable.XKeyButtonView_keyRepeat, true);

		mGlowBG = a.getDrawable(R.styleable.XKeyButtonView_glowBackground);
		if (mGlowBG != null) {
			setDrawingAlpha(BUTTON_QUIESCENT_ALPHA);
			mGlowWidth = mGlowBG.getIntrinsicWidth();
			mGlowHeight = mGlowBG.getIntrinsicHeight();
		}

		a.recycle();

		setClickable(true);
		mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if (mGlowBG != null) {
			canvas.save();
			final int w = getWidth();
			final int h = getHeight();
			final float aspect = (float) mGlowWidth / mGlowHeight;
			final int drawW = (int) (h * aspect);
			final int drawH = h;
			final int margin = (drawW - w) / 2;
			canvas.scale(mGlowScale, mGlowScale, w * 0.5f, h * 0.5f);
			mGlowBG.setBounds(-margin, 0, drawW - margin, drawH);
			mGlowBG.setAlpha((int) (mDrawingAlpha * mGlowAlpha * 255));
			mGlowBG.draw(canvas);
			canvas.restore();
			mRect.right = w;
			mRect.bottom = h;
		}
		super.onDraw(canvas);
	}

	public float getDrawingAlpha() {
		if (mGlowBG == null)
			return 0;
		return mDrawingAlpha;
	}

	@SuppressWarnings("deprecation")
	public void setDrawingAlpha(float x) {
		if (mGlowBG == null)
			return;
		// Calling setAlpha(int), which is an ImageView-specific
		// method that's different from setAlpha(float). This sets
		// the alpha on this ImageView's drawable directly
		setAlpha((int) (x * 255));
		mDrawingAlpha = x;
	}

	public float getGlowAlpha() {
		if (mGlowBG == null)
			return 0;
		return mGlowAlpha;
	}

	public void setGlowAlpha(float x) {
		if (mGlowBG == null)
			return;
		mGlowAlpha = x;
		invalidate();
	}

	public float getGlowScale() {
		if (mGlowBG == null)
			return 0;
		return mGlowScale;
	}

	@SuppressWarnings("unused")
	public void setGlowScale(float x) {
		if (mGlowBG == null)
			return;
		mGlowScale = x;
		final float w = getWidth();
		final float h = getHeight();
		if (GLOW_MAX_SCALE_FACTOR <= 1.0f) {
			// this only works if we know the glow will never leave our bounds
//			invalidate();
		} else {
			final float rx = (w * (GLOW_MAX_SCALE_FACTOR - 1.0f)) / 2.0f + 1.0f;
			final float ry = (h * (GLOW_MAX_SCALE_FACTOR - 1.0f)) / 2.0f + 1.0f;
			invalidateGlobalRegion(this,
					new RectF(getLeft() - rx, getTop() - ry, getRight() + rx,
							getBottom() + ry));

			// also invalidate our immediate parent to help avoid situations
			// where nearby glows
			// interfere
			((View) getParent()).invalidate();
		}
	}

	public void setPressed(boolean pressed) {
		if (mGlowBG != null) {
			if (pressed != isPressed()) {
				if (mPressedAnim != null && mPressedAnim.isRunning()) {
					mPressedAnim.cancel();
				}
				final AnimatorSet as = mPressedAnim = new AnimatorSet();
				if (pressed) {
					if (mGlowScale < GLOW_MAX_SCALE_FACTOR)
						mGlowScale = GLOW_MAX_SCALE_FACTOR;
					if (mGlowAlpha < BUTTON_QUIESCENT_ALPHA)
						mGlowAlpha = BUTTON_QUIESCENT_ALPHA;
					setDrawingAlpha(1f);
					as.playTogether(ObjectAnimator.ofFloat(this, "glowAlpha", 1f), ObjectAnimator.ofFloat(this, "glowScale", GLOW_MAX_SCALE_FACTOR));
					as.setDuration(50);
				} else {
					as.playTogether(ObjectAnimator.ofFloat(this, "glowAlpha", 0f), ObjectAnimator.ofFloat(this, "glowScale", 1f), ObjectAnimator.ofFloat(this, "drawingAlpha", BUTTON_QUIESCENT_ALPHA));
					as.setDuration(500);
				}
				as.start();
			}
		}
		super.setPressed(pressed);
	}

	public boolean onTouchEvent(MotionEvent ev) {
		final int action = ev.getAction();
		int x, y;

		switch (action) {
		case MotionEvent.ACTION_DOWN:
			// Slog.d("KeyButtonView", "press");
			mDownTime = SystemClock.uptimeMillis();
			setPressed(true);
			if (mCode != 0) {
				sendEvent(KeyEvent.ACTION_DOWN, 0, mDownTime);
			} else {
				// Provide the same haptic feedback that the system offers for
				// virtual keys.
				performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
			}
			if (mSupportsLongpress) {
				removeCallbacks(mCheckLongPress);
				postDelayed(mCheckLongPress, ViewConfiguration.getLongPressTimeout());
			}
			break;
		case MotionEvent.ACTION_MOVE:
			x = (int) ev.getX();
			y = (int) ev.getY();
			setPressed(x >= -mTouchSlop && x < getWidth() + mTouchSlop && y >= -mTouchSlop && y < getHeight() + mTouchSlop);
			break;
		case MotionEvent.ACTION_CANCEL:
			setPressed(false);
			if (mCode != 0) {
				sendEvent(KeyEvent.ACTION_UP, KeyEvent.FLAG_CANCELED);
			}
			if (mSupportsLongpress) {
				removeCallbacks(mCheckLongPress);
			}
			break;
		case MotionEvent.ACTION_UP:
			final boolean doIt = isPressed();
			setPressed(false);
			if (mCode != 0) {
				if (doIt) {
					sendEvent(KeyEvent.ACTION_UP, 0);
					sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
					playSoundEffect(SoundEffectConstants.CLICK);
				} else {
					sendEvent(KeyEvent.ACTION_UP, KeyEvent.FLAG_CANCELED);
				}
			} else {
				// no key code, just a regular ImageView
				if (doIt) {
					performClick();
				}
			}
			if (mSupportsLongpress) {
				removeCallbacks(mCheckLongPress);
			}
			break;
		}

		return true;
	}

	void sendEvent(int action, int flags) {
		sendEvent(action, flags, SystemClock.uptimeMillis());
	}

	void sendEvent(int action, int flags, long when) {
		final int repeatCount = (flags & KeyEvent.FLAG_LONG_PRESS) != 0 ? 1 : 0;
		final KeyEvent ev = new KeyEvent(mDownTime, when, action, mCode,
				repeatCount, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, flags
						| KeyEvent.FLAG_FROM_SYSTEM
						| KeyEvent.FLAG_VIRTUAL_HARD_KEY,
				InputDevice.SOURCE_KEYBOARD);
		InputManager inputManager = (InputManager) mContext.getSystemService(Context.INPUT_SERVICE);
//		InputManager.getInstance().injectInputEvent(ev, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
		Object params[] = new Object[2];
        params[0] = ev;
        params[1] = Integer.valueOf(0);
        try {
			Method method = inputManager.getClass().getMethod("injectInputEvent", new Class[]{ InputEvent.class, int.class });
			method.invoke(inputManager, params);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	public static void invalidateGlobalRegion(View view, RectF childBounds) {
		// childBounds.offset(view.getTranslationX(), view.getTranslationY());
//		if (DEBUG_INVALIDATE)
//			Log.v(TAG, "-------------");
		while (view.getParent() != null && view.getParent() instanceof View) {
			view = (View) view.getParent();
			view.getMatrix().mapRect(childBounds);
			view.invalidate((int) Math.floor(childBounds.left),
					(int) Math.floor(childBounds.top),
					(int) Math.ceil(childBounds.right),
					(int) Math.ceil(childBounds.bottom));
//			if (DEBUG_INVALIDATE) {
//				Log.v(TAG,
//						"INVALIDATE(" + (int) Math.floor(childBounds.left)
//								+ "," + (int) Math.floor(childBounds.top) + ","
//								+ (int) Math.ceil(childBounds.right) + ","
//								+ (int) Math.ceil(childBounds.bottom));
//			}
		}
	}
}