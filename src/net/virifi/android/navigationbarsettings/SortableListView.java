package net.virifi.android.navigationbarsettings;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ImageView;
import android.widget.ListView;


public class SortableListView extends ListView implements OnItemLongClickListener {
    private static final int SCROLL_SPEED_FAST = 25;
    private static final int SCROLL_SPEED_SLOW = 8;
    private static final Bitmap.Config DRAG_BITMAP_CONFIG = Bitmap.Config.ARGB_8888;
    
    private boolean mSortable = false;
    private boolean mDragging = false;
    private DragListener mDragListener = new SimpleDragListener();
    private int mBitmapBackgroundColor = Color.argb(128, 0xFF, 0xFF, 0xFF);
    private Bitmap mDragBitmap = null;
    private ImageView mDragImageView = null;
    private WindowManager.LayoutParams mLayoutParams = null;
   // private MotionEvent mActionDownEvent;
    private int mPositionFrom = -1;
    private float mActionDownEventX;
    private float mActionDownEventY;
    
    private int mContentViewTop;
    
    private void getContentViewTop(Context context) {
    	 Rect rect= new Rect();
    	 Window window= ((Activity)context).getWindow();
    	 window.getDecorView().getWindowVisibleDisplayFrame(rect);
    	//int statusBarHeight= rect.top;
    	mContentViewTop= window.findViewById(Window.ID_ANDROID_CONTENT).getTop();
    	//int titleBarHeight= contentViewTop - statusBarHeight;
    }
    
    public SortableListView(Context context) {
        super(context);
        setOnItemLongClickListener(this);
        getContentViewTop(context);
    }
    
    public SortableListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnItemLongClickListener(this);
        getContentViewTop(context);
    }
    
    public SortableListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setOnItemLongClickListener(this);
        getContentViewTop(context);
    }
    
    public void setDragListener(DragListener listener) {
        mDragListener = listener;
    }
    
    public void setSortable(boolean sortable) {
        this.mSortable = sortable;
    }
    
    @Override
    public void setBackgroundColor(int color) {
        mBitmapBackgroundColor = color;
    }
    
    public boolean getSortable() {
        return mSortable;
    }
    
    private int actionDownCoordToPosition() {
    	getContentViewTop(getContext());
       int tmp =  pointToPosition((int)this.mActionDownEventX, (int) this.mActionDownEventY);
       return tmp - this.getFirstVisiblePosition();
    }
    
    private int eventToPosition(MotionEvent event) {
    	getContentViewTop(getContext());
        int tmp =  pointToPosition((int) event.getX(), (int) event.getY() - mContentViewTop);
        return tmp - this.getFirstVisiblePosition();
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mSortable) {
            return super.onTouchEvent(event);
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
            	
                //storeMotionEvent(event);
            	this.mActionDownEventX = event.getX();
            	this.mActionDownEventY = event.getY();
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                if (duringDrag(event)) {
                    return true;
                }
                break;
            }
            case MotionEvent.ACTION_UP: {
                if (stopDrag(event, true)) {
                    return true;
                }
                break;
            }
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_OUTSIDE: {
                if (stopDrag(event, false)) {
                    return true;
                }
                break;
            }
        }
        return super.onTouchEvent(event);
    }
    
    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view,
            int position, long id) {
        return startDrag();
    }
    
//    private void storeMotionEvent(MotionEvent event) {
//        //mActionDownEvent = event;
//    }
    
    private boolean startDrag() {
    	//mPositionFrom = eventToPosition(mActionDownEvent);
    	mPositionFrom = this.actionDownCoordToPosition();
        
    	if (mPositionFrom < 0) {
            return false;
        }
        mDragging = true;
        
        // View, Canvas, WindowManager
        final View view = getChildByIndex(mPositionFrom);
        final Canvas canvas = new Canvas();
        final WindowManager wm = getWindowManager();
        //view.setVisibility(View.INVISIBLE);
        
        mDragBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(),
                DRAG_BITMAP_CONFIG);
        canvas.setBitmap(mDragBitmap);
        view.draw(canvas);
        
        if (mDragImageView != null) {
            wm.removeView(mDragImageView);
        }
        
        //if (mLayoutParams == null) {
            initLayoutParams();
        //}
        
        //updateLayoutParams((int)mActionDownEvent.getX(), (int)mActionDownEvent.getY());
        
        mDragImageView = new ImageView(getContext());
        mDragImageView.setBackgroundColor(mBitmapBackgroundColor);
        mDragImageView.setImageBitmap(mDragBitmap);
        wm.addView(mDragImageView, mLayoutParams);
        
        if (mDragListener != null) {
            mPositionFrom = mDragListener.onStartDrag(mPositionFrom);
        }
        //return duringDrag(mActionDownEvent);
        return true;
        
    }
    
    private boolean duringDrag(MotionEvent event) {
    	
        if (!mDragging || mDragImageView == null) {
            return false;
        }
        final int x = (int) event.getX();
        final int y = (int) event.getY();
        final int height = getHeight();
        final int middle = height / 2;
        
        final int speed;
        final int fastBound = height / 9;
        final int slowBound = height / 4;
        if (event.getEventTime() - event.getDownTime() < 500) {
        	speed = 0;
        } else if (y < slowBound) {
            speed = y < fastBound ? -SCROLL_SPEED_FAST : -SCROLL_SPEED_SLOW;
        } else if (y > height - slowBound) {
            speed = y > height - fastBound ? SCROLL_SPEED_FAST
                    : SCROLL_SPEED_SLOW;
        } else {
            speed = 0;
        }
        
        if (speed != 0) {
        int middlePosition = pointToPosition(0, middle);
            if (middlePosition == AdapterView.INVALID_POSITION) {
                middlePosition = pointToPosition(0, middle + getDividerHeight()
                        + 64);
            }
            final View middleView = getChildByIndex(middlePosition);
            if (middleView != null) {
                setSelectionFromTop(middlePosition, middleView.getTop() - speed);
            }
        }
        
        if (mDragImageView.getHeight() < 0) {
            mDragImageView.setVisibility(View.INVISIBLE);
        } else {
            mDragImageView.setVisibility(View.VISIBLE);
        }
        updateLayoutParams(x, y);
        getWindowManager().updateViewLayout(mDragImageView, mLayoutParams);
        if (mDragListener != null) {
            mPositionFrom = mDragListener.onDuringDrag(mPositionFrom,
                    pointToPosition(x, y));
        }
        return true;
    }
    
    private boolean stopDrag(MotionEvent event, boolean isDrop) {
        if (!mDragging) {
            return false;
        }
        if (isDrop && mDragListener != null) {
            mDragListener.onStopDrag(mPositionFrom, eventToPosition(event));
        }
        mDragging = false;
        if (mDragImageView != null) {
            getWindowManager().removeView(mDragImageView);
            mDragImageView = null;
            // mDragBitmap.recycle();
            mDragBitmap = null;
            return true;
        }
        return false;
    }
    
    private View getChildByIndex(int index) {
        return getChildAt(index - getFirstVisiblePosition());
    }
    
    protected WindowManager getWindowManager() {
        return (WindowManager) getContext().getSystemService(
                Context.WINDOW_SERVICE);
    }
    
    protected void initLayoutParams() {
        mLayoutParams = new WindowManager.LayoutParams();
        mLayoutParams.gravity = Gravity.TOP | Gravity.LEFT;
        mLayoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mLayoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        mLayoutParams.format = PixelFormat.TRANSLUCENT;
        mLayoutParams.windowAnimations = 0;
        mLayoutParams.x = getLeft();
        mLayoutParams.y = getTop() + (int)this.mActionDownEventY;
    }
    
    protected void updateLayoutParams(int x, int y) {
       // mLayoutParams.y = getTop() + y - 32;
    	 mLayoutParams.y = getTop() + y;
    }
    
    public interface DragListener {
        public int onStartDrag(int position);
        public int onDuringDrag(int positionFrom, int positionTo);
        
        public boolean onStopDrag(int positionFrom, int positionTo);
    }
    
    public static class SimpleDragListener implements DragListener {
        @Override
        public int onStartDrag(int position) {
            return position;
        }
        
        @Override
        public int onDuringDrag(int positionFrom, int positionTo) {
            return positionFrom;
        }
        

        @Override
        public boolean onStopDrag(int positionFrom, int positionTo) {
            return positionFrom != positionTo && positionFrom >= 0
                    || positionTo >= 0;
        }
    }
}
