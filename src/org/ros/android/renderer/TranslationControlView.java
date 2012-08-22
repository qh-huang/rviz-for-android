package org.ros.android.renderer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;

public class TranslationControlView extends View implements OnGestureListener {

	private Paint paint;

	private GestureDetector gestureDetector;

	public interface OnMoveListener {
		public void onMove(float X, float Y);
		public void onMoveStart();
	}
	public interface OnMouseUpListener {
		public void mouseUp(MotionEvent e);
	}

	private static final OnMoveListener DEFAULT_ONMOVE_LISTENER = new OnMoveListener() {
		@Override
		public void onMove(float dX, float dY) {
		}

		@Override
		public void onMoveStart() {
		}
	};
	
	private static final OnMouseUpListener DEFAULT_ONUP_LISTENER = new OnMouseUpListener() {
		@Override
		public void mouseUp(MotionEvent e) {
			// TODO Auto-generated method stub
			
		}
	};
	
	private OnMouseUpListener mouseUpListener = DEFAULT_ONUP_LISTENER;
	private OnMoveListener moveListener = DEFAULT_ONMOVE_LISTENER;

	public TranslationControlView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public TranslationControlView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public TranslationControlView(Context context) {
		super(context);
		init();
	}

	private static Paint createDefaultPaint() {
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setFilterBitmap(true);
		return paint;
	}

	private void init() {
		paint = createDefaultPaint();
		paint.setAntiAlias(true);
		paint.setStrokeWidth(2);
		paint.setStyle(Paint.Style.FILL);
		paint.setColor(Color.GRAY);
		gestureDetector = new GestureDetector(getContext(), this);
		gestureDetector.setIsLongpressEnabled(false);
	}

	public void setOnMoveListener(OnMoveListener listener) {
		moveListener = listener;
	}
	
	public void setOnMouseUpListener(OnMouseUpListener listener) {
		mouseUpListener = listener;
	}

	private static final int BUTTON_WIDTH = 55;
	private static final int BUTTON_HEIGHT = 85;
	private static final int DIMENSION = Math.max(BUTTON_HEIGHT, BUTTON_WIDTH);
	
	private static final float CENTER_X = DIMENSION/2f;
	private static final float CENTER_Y = DIMENSION/2f;

	private static final float ARROW_MIDDLE_EDGEGAP = 0.38f;
	private static final float ARROW_HEAD_HEIGHT = 0.4f;
	private static final float[] vertices = {0f, ARROW_HEAD_HEIGHT*BUTTON_HEIGHT,
											0.5f*BUTTON_WIDTH, 0f,									 
											BUTTON_WIDTH, ARROW_HEAD_HEIGHT*BUTTON_HEIGHT,
											 (1-ARROW_MIDDLE_EDGEGAP)*BUTTON_WIDTH,ARROW_HEAD_HEIGHT*BUTTON_HEIGHT,
											 (1-ARROW_MIDDLE_EDGEGAP)*BUTTON_WIDTH,(1-ARROW_HEAD_HEIGHT)*BUTTON_HEIGHT,
											 BUTTON_WIDTH,(1-ARROW_HEAD_HEIGHT)*BUTTON_HEIGHT,
											 0.5f*BUTTON_WIDTH, BUTTON_HEIGHT,
											 0f, (1-ARROW_HEAD_HEIGHT)*BUTTON_HEIGHT,
											 ARROW_MIDDLE_EDGEGAP*BUTTON_WIDTH, (1-ARROW_HEAD_HEIGHT)*BUTTON_HEIGHT,
											 ARROW_MIDDLE_EDGEGAP*BUTTON_WIDTH, ARROW_HEAD_HEIGHT*BUTTON_HEIGHT,};
	private static final Path path = new Path();
	static {
		path.moveTo(vertices[0], vertices[1]);
		
		for(int i = 2; i < vertices.length; i+=2)
			path.lineTo(vertices[i], vertices[i+1]);
		
		path.lineTo(vertices[0], vertices[1]);
		path.offset((DIMENSION-BUTTON_WIDTH)/2f, (DIMENSION-BUTTON_HEIGHT)/2f);
	}
	
	
	@Override
	protected void onMeasure(int widthSpec, int heightSpec) {
		setMeasuredDimension(DIMENSION, DIMENSION);
	}

	private float angle = 0;
	@Override
	protected void onDraw(Canvas canvas) {
		canvas.rotate(angle,CENTER_X,CENTER_Y);
		paint.setStyle(Paint.Style.FILL);
		paint.setColor(Color.GRAY);
		canvas.drawPath(path, paint);
		paint.setStyle(Paint.Style.STROKE);
		paint.setColor(Color.BLACK);
		canvas.drawPath(path, paint);
	}

	public void setDrawAngle(double angle) {
		this.angle = (float) Math.toDegrees(angle);
		this.invalidate();
	}
	
	@Override
	public boolean onDown(MotionEvent e) {
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		moveListener.onMove(e2.getRawX(), e2.getRawY());
		return true;
	}

	@Override
	public void onShowPress(MotionEvent e) {
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		return false;
	}

	private boolean moving = false;
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(event.getAction() == MotionEvent.ACTION_UP) {
			mouseUpListener.mouseUp(event);
			moving = false;
		} else if(!moving) {
			moving = true;
			moveListener.onMoveStart();
		}	
		
		if(gestureDetector.onTouchEvent(event)) {
			return true;
		} else {
			return super.onTouchEvent(event);
		}
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		return false;
	}
}
