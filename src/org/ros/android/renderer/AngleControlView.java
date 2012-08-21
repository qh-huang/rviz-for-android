package org.ros.android.renderer;

import org.ros.android.renderer.TranslationControlView.OnMouseUpListener;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public class AngleControlView extends View implements android.view.GestureDetector.OnGestureListener {
	
	public static interface OnAngleChangeListener {
		public void angleChange(float newAngle, float delta); 
	}
	
	private static Paint paint;

	private GestureDetector gestureDetector;

	private static Paint createDefaultPaint() {
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setFilterBitmap(true);
		return paint;
	}

	public AngleControlView(Context context) {
		super(context);
		init();
	}

	public AngleControlView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public AngleControlView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	private void init() {
		paint = createDefaultPaint();
		paint.setAntiAlias(true);
		paint.setColor(Color.BLUE);
		paint.setTextSize(40);
		paint.setStrokeWidth(0);
		paint.setStyle(Paint.Style.STROKE);

		gestureDetector = new GestureDetector(getContext(), this);
		gestureDetector.setIsLongpressEnabled(false);
	}

	private static final int KNOB_RADIUS = 150;
	private static final int KNOB_LINEWIDTH = 30;

	private static final int INNER_RADIUS = KNOB_RADIUS - (KNOB_LINEWIDTH / 2) + 3;
	private static final int OUTER_RADIUS = KNOB_RADIUS + (KNOB_LINEWIDTH / 2) - 3;

	private static final int KNOB_WIDTH = (2 * KNOB_RADIUS) + KNOB_LINEWIDTH + 12;

	private static final double TICK_SEPARATION = Math.toRadians(20); // Degrees

	private static final int COLOR_BACKGROUND = Color.GRAY;
	private static final int COLOR_TICKS = Color.DKGRAY;

	private int centerX;
	private int centerY;
	private float angle = 0f;

	private static final OnAngleChangeListener DEFAULT_ANGLE_LISTENER = new OnAngleChangeListener() {
		@Override
		public void angleChange(float newAngle, float delta) {
		}
	};
	
	private static final OnMouseUpListener DEFAULT_ONUP_LISTENER = new OnMouseUpListener() {
		@Override
		public void mouseUp(MotionEvent e) {
		}
	};
	
	private OnAngleChangeListener angleListener = DEFAULT_ANGLE_LISTENER;
	private OnMouseUpListener mouseUpListener = DEFAULT_ONUP_LISTENER;
	
	public void setOnAngleChangeListener(OnAngleChangeListener angleListener) {
		this.angleListener = angleListener;
	}
	
	public void setOnMouseUpListener(OnMouseUpListener listener) {
		mouseUpListener = listener;
	}
	
	@Override
	protected void onMeasure(int widthSpec, int heightSpec) {
		setMeasuredDimension(KNOB_WIDTH, KNOB_WIDTH);

		int height = getMeasuredHeight();
		int width = getMeasuredWidth();
		centerX = width / 2;
		centerY = height / 2;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		// Draw 3D perspective looking piece
		paint.setStrokeWidth(5);
		paint.setColor(Color.DKGRAY);
		canvas.drawCircle(centerX+3, centerY+2, INNER_RADIUS, paint);
		canvas.drawCircle(centerX+3, centerY+2, OUTER_RADIUS, paint);
		
		// Draw ring and border
		paint.setStrokeWidth(KNOB_LINEWIDTH);
		paint.setColor(COLOR_BACKGROUND);
		canvas.drawCircle(centerX, centerY, KNOB_RADIUS, paint);

		paint.setStrokeWidth(1);
		paint.setColor(Color.BLACK);
		canvas.drawCircle(centerX, centerY, KNOB_RADIUS + (KNOB_LINEWIDTH / 2), paint);
		canvas.drawCircle(centerX, centerY, KNOB_RADIUS - (KNOB_LINEWIDTH / 2), paint);
		
		// Rotate to current angle
		canvas.rotate(angle, centerX, centerY);

		// Draw tick marks
		paint.setColor(COLOR_TICKS);
		for(float i = 0; i < 2 * Math.PI; i += TICK_SEPARATION) {
			float cos = FloatMath.cos(i);
			float sin = FloatMath.sin(i);
			canvas.drawLine(centerX + INNER_RADIUS * cos, centerY + INNER_RADIUS * sin, centerX + OUTER_RADIUS * cos, centerY + OUTER_RADIUS * sin, paint);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(event.getAction() == MotionEvent.ACTION_UP)
			mouseUpListener.mouseUp(event);
		
		if(gestureDetector.onTouchEvent(event)) {
			return true;
		} else {
			return false;
		}
	}

	private float dragStartDeg;

	@Override
	public boolean onDown(MotionEvent e) {
		dragStartDeg = toDegrees(e.getX(), e.getY());
		return (dragStartDeg >= 0);
	}

	private float toDegrees(float x, float y) {
		double radius = Math.sqrt(Math.pow(x - centerX, 2) + Math.pow(y - centerY, 2));
		
		if(radius < (KNOB_RADIUS - (1.5*KNOB_LINEWIDTH)))
			return -999;
		
		float degrees = (float) -(Math.toDegrees(Math.atan2(centerY - y, centerX - x)) - 180);

		return degrees;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
	}

	private static final int MAX_ANGLE_DELTA = 25;
	
	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		if(dragStartDeg >= 0) {
			float currentAngle = toDegrees(e2.getX(), e2.getY());

			if(currentAngle == -999)
				return false;
			
			float delta = dragStartDeg - currentAngle;

			delta %= 360.0;

//			if(Math.abs(delta) > MAX_ANGLE_DELTA)
//				delta = 0;

			angle += delta;
			angle %= 360.0;
			dragStartDeg = currentAngle;
			this.invalidate();
			angleListener.angleChange(angle, delta);
		}
		return true;
	}

	@Override
	public void onShowPress(MotionEvent e) {
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		return false;
	}

}
