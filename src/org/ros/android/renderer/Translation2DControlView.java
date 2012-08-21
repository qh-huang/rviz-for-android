package org.ros.android.renderer;

import org.ros.android.renderer.TranslationControlView.OnMouseUpListener;
import org.ros.android.renderer.TranslationControlView.OnMoveListener;

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

public class Translation2DControlView extends View implements OnGestureListener {

	private Paint paint;

	private GestureDetector gestureDetector;

	private static final OnMoveListener DEFAULT_LISTENER = new OnMoveListener() {
		@Override
		public void onMove(float X, float Y) {
		}
	};

	private static final OnMouseUpListener DEFAULT_ONUP_LISTENER = new OnMouseUpListener() {
		@Override
		public void mouseUp(MotionEvent e) {
		}
	};

	private OnMouseUpListener mouseUpListener = DEFAULT_ONUP_LISTENER;
	private OnMoveListener moveListener = DEFAULT_LISTENER;

	public Translation2DControlView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public Translation2DControlView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public Translation2DControlView(Context context) {
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
		paint.setStrokeWidth(1);
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

	private static final int DIMENSION = 85;

	private static final float CENTER_X = DIMENSION / 2f;
	private static final float CENTER_Y = DIMENSION / 2f;

	private static final float ARROW_MIDDLE_EDGEGAP = 0.4f;
	private static final float ARROW_HEAD_HEIGHT = 0.25f;
	private static final float ARROW_HEAD_WIDTH = 0.3f;
	private static final float[] vertices = { ARROW_HEAD_WIDTH * DIMENSION, ARROW_HEAD_HEIGHT * DIMENSION, 0.5f * DIMENSION, 0f, (1 - ARROW_HEAD_WIDTH) * DIMENSION, ARROW_HEAD_HEIGHT * DIMENSION, (1 - ARROW_MIDDLE_EDGEGAP) * DIMENSION, ARROW_HEAD_HEIGHT * DIMENSION, (1 - ARROW_MIDDLE_EDGEGAP) * DIMENSION, (1 - ARROW_HEAD_HEIGHT) * DIMENSION, (1 - ARROW_HEAD_WIDTH) * DIMENSION, (1 - ARROW_HEAD_HEIGHT) * DIMENSION, 0.5f * DIMENSION, DIMENSION, ARROW_HEAD_WIDTH * DIMENSION, (1 - ARROW_HEAD_HEIGHT) * DIMENSION, ARROW_MIDDLE_EDGEGAP * DIMENSION, (1 - ARROW_HEAD_HEIGHT) * DIMENSION, ARROW_MIDDLE_EDGEGAP * DIMENSION, ARROW_HEAD_HEIGHT * DIMENSION, };

	private static final Path path = new Path();
	static {
		path.moveTo(vertices[0], vertices[1]);

		for(int i = 2; i < vertices.length; i += 2)
			path.lineTo(vertices[i], vertices[i + 1]);

		path.lineTo(vertices[0], vertices[1]);
	}

	@Override
	protected void onMeasure(int widthSpec, int heightSpec) {
		setMeasuredDimension(DIMENSION + 1, DIMENSION + 1);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		paint.setStyle(Paint.Style.FILL);
		paint.setColor(Color.GRAY);
		canvas.drawPath(path, paint);
		paint.setStyle(Paint.Style.STROKE);
		paint.setColor(Color.BLACK);
		canvas.drawPath(path, paint);

		canvas.rotate(90, CENTER_X, CENTER_Y);

		paint.setStyle(Paint.Style.FILL);
		paint.setColor(Color.GRAY);
		canvas.drawPath(path, paint);
		paint.setStyle(Paint.Style.STROKE);
		paint.setColor(Color.BLACK);
		canvas.drawPath(path, paint);
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

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(event.getAction() == MotionEvent.ACTION_UP)
			mouseUpListener.mouseUp(event);

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
