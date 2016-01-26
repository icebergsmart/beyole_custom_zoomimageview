package com.beyole.view;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;

public class ZoomImageView extends ImageView implements OnScaleGestureListener, OnTouchListener, OnGlobalLayoutListener {

	private static final String TAG = ZoomImageView.class.getSimpleName();
	public static final float SCALE_MAX = 4.0f;
	private static final float SCALE_MID = 2.0f;
	// 初始化时的缩放比例，如果图片宽度或者高度大于屏幕则该值小于1
	private float initScale = 1.0f;
	// 用于存放矩阵的9个值
	private final float[] matrixValues = new float[9];
	// 是否为第一次显示图片
	private boolean once = true;

	private float mLastX, mLastY;
	private boolean isCanDrag;
	private int lastPointCount;
	// 触发移动事件的最短距离
	private int mTouchSlop;
	private boolean isCheckTopAndBottom = true;
	private boolean isCheckLeftAndRight = true;
	// 缩放的手势检测
	private ScaleGestureDetector mScaleGestureDetector = null;
	private final Matrix mScaleMatrix = new Matrix();

	// 图片最初的Matrix
	private Matrix mFinalScaleMatrix = new Matrix();

	// 双击检测
	private GestureDetector mGestureDetector;
	private boolean isAutoScale;

	public ZoomImageView(Context context) {
		this(context, null);
	}

	public ZoomImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// 设置ImageView为Matrix，只有设置才能进行矩阵变换
		super.setScaleType(ScaleType.MATRIX);
		// 初始化手势检测器
		mScaleGestureDetector = new ScaleGestureDetector(context, this);
		mGestureDetector = new GestureDetector(context, new SimpleOnGestureListener() {
			@Override
			public boolean onDoubleTap(MotionEvent e) {
				if (isAutoScale == true)
					return true;
				float x = e.getX();
				float y = e.getY();
				if (getScale() < SCALE_MID) {
					ZoomImageView.this.postDelayed(new AutoScaleRunnable(SCALE_MID, x, y), 16);
					isAutoScale = true;
				} else {
					ZoomImageView.this.postDelayed(new AutoScaleRunnable(initScale, x, y), 16);
					isAutoScale = true;
				}
				return true;
			}
		});
		// 设置移动的最小距离
		mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
		// 设置触摸监听器，事件交由手势检测器处理
		this.setOnTouchListener(this);
	}

	@Override
	public void onGlobalLayout() {
		if (once) {
			Drawable drawable = getDrawable();
			if (drawable == null) {
				return;
			}
			int width = getWidth();
			int height = getHeight();
			// 拿到图片的宽度和高度
			int dw = drawable.getIntrinsicWidth();
			int dh = drawable.getIntrinsicHeight();
			float scale = 1.0f;
			// 如果图片的高度和宽度大于屏幕，则缩放至屏幕的宽或者高
			if (dw > width && dh <= height) {
				scale = width * 1.0f / dw;
			}
			if (dh > height && dw <= width) {
				scale = height * 1.0f / dh;
			}
			// 如果宽度和高度都大于屏幕，则让其按比例适应屏幕大小
			if (dh > height && dw > width) {
				scale = Math.min(width * 1.0f / dw, height * 1.0f / dh);
			}
			initScale = scale;
			// 屏幕移动至屏幕中心，注意这里是平移的距离，而不是平移目的地的坐标
			mScaleMatrix.postTranslate((width - dw) / 2, (height - dh) / 2);
			// 缩放中心点为容器中心点坐标
			mScaleMatrix.postScale(scale, scale, getWidth() / 2, getHeight() / 2);
			setImageMatrix(mScaleMatrix);
			once = false;
		}
	}

	/**
	 * 触摸事件交由ScaleGestureDetector处理
	 */
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (mGestureDetector.onTouchEvent(event)) {
			return true;
		}
		// 将事件交由ScaleGestureDector处理
		mScaleGestureDetector.onTouchEvent(event);
		float x = 0, y = 0;
		// 拿到触摸点的个数
		final int pointCount = event.getPointerCount();
		// 得到多个触摸点的x和y的均值
		for (int i = 0; i < pointCount; i++) {
			x += event.getX(i);
			y += event.getY(i);
		}
		x = x / pointCount;
		y = y / pointCount;
		// 每当触摸点发生变化时，重置mLastX,mLastY
		if (pointCount != lastPointCount) {
			isCanDrag = false;
			mLastX = x;
			mLastY = y;
		}
		lastPointCount = pointCount;
		RectF rectF = getMatrixRectF();
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			if (rectF.width() > getWidth() || rectF.height() > getHeight()) {
				// 不允许父控件拦截事件
				getParent().requestDisallowInterceptTouchEvent(true);
			}
			break;
		case MotionEvent.ACTION_MOVE:
			if (rectF.width() > getWidth() || rectF.height() > getHeight()) {
				// 不允许父控件拦截事件
				getParent().requestDisallowInterceptTouchEvent(true);
			}
			float dx = x - mLastX;
			float dy = y - mLastY;
			if (!isCanDrag) {
				isCanDrag = isCanDrag(dx, dy);
			}
			if (isCanDrag) {
				if (getDrawable() != null) {
					if (getMatrixRectF().left == 0 && dx > 0) {
						getParent().requestDisallowInterceptTouchEvent(false);
						ZoomImageView.this.postDelayed(new AutoScaleRunnable(initScale, x, y), 16);
					}
					if (getMatrixRectF().right == getWidth() && dx < 0) {
						ZoomImageView.this.postDelayed(new AutoScaleRunnable(initScale, x, y), 16);
						getParent().requestDisallowInterceptTouchEvent(false);
					}
					isCheckLeftAndRight = isCheckTopAndBottom = true;
					// 如果宽度小于屏幕宽度，则禁止左右移动
					if (rectF.width() < getWidth()) {
						dx = 0;
						isCheckLeftAndRight = false;
					}
					// 如果高度小于屏幕高度，则禁止上下移动
					if (rectF.height() < getHeight()) {
						dy = 0;
						isCheckTopAndBottom = false;
					}
					mScaleMatrix.postTranslate(dx, dy);
					checkMatrixBounds();
					setImageMatrix(mScaleMatrix);
				}
			}
			// 随时更新
			mLastX = x;
			mLastY = y;
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			lastPointCount = 0;
			break;

		}
		return true;
	}

	/**
	 * 移动边界判断，主要判断宽度或者高度大于屏幕的
	 */
	private void checkMatrixBounds() {
		RectF rectF = getMatrixRectF();
		float deltaX = 0, deltaY = 0;
		final float viewWidth = getWidth();
		final float viewHeight = getHeight();
		// 判断移动或缩放后，图片是否超出屏幕边界
		if (rectF.top > 0 && isCheckTopAndBottom) {
			deltaY = -rectF.top;
		}
		if (rectF.bottom < viewHeight && isCheckTopAndBottom) {
			deltaY = viewHeight - rectF.bottom;
		}
		if (rectF.left > 0 && isCheckLeftAndRight) {
			deltaX = -rectF.left;
		}
		if (rectF.right < viewWidth && isCheckLeftAndRight) {
			deltaX = viewWidth - rectF.right;
		}
		mScaleMatrix.postTranslate(deltaX, deltaY);
	}

	@Override
	public boolean onScale(ScaleGestureDetector detector) {
		float scale = getScale();
		// ScaleGestureDetector会根据手指的分开和合拢计算出一个缩放因子
		// mScaleFactor = getCurrentSpan() / getPreviousSpan(); 缩小时，距离越大，值越小
		float scaleFactor = detector.getScaleFactor();
		if (getDrawable() == null) {
			return true;
		}
		// 缩放的范围控制
		if ((scale < SCALE_MAX && scaleFactor > 1.0f) || (scale > initScale && scaleFactor < 1.0f)) {
			// 最大值最小值的判断
			if (scaleFactor * scale < initScale) {
				scaleFactor = initScale / scale;
			}
			if (scaleFactor * scale > SCALE_MAX) {
				scaleFactor = SCALE_MAX / scale;
			}
			// 设置缩放比例
			// mScaleMatrix.postScale(scaleFactor, scaleFactor, getWidth() / 2,
			// getHeight() / 2);
			// 更改缩放中心
			// <--随意设置缩放点会导致图片的会与边界出现白点-->
			mScaleMatrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
			checkBoundsAndCenterWhenScale();
			setImageMatrix(mScaleMatrix);
		}
		return true;
	}

	@Override
	public boolean onScaleBegin(ScaleGestureDetector detector) {
		return true;
	}

	@Override
	public void onScaleEnd(ScaleGestureDetector detector) {

	}

	/**
	 * 获得当前的缩放比率
	 * 
	 * @return
	 */
	public final float getScale() {
		mScaleMatrix.getValues(matrixValues);
		return matrixValues[Matrix.MSCALE_X];
	}

	/**
	 * 该方法运行在onResume生命周期之后
	 */
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		getViewTreeObserver().addOnGlobalLayoutListener(this);
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		getViewTreeObserver().removeGlobalOnLayoutListener(this);
	}

	/**
	 * 缩放时检查边界和图片显示范围的控制
	 */
	private void checkBoundsAndCenterWhenScale() {
		RectF rectF = getMatrixRectF();
		float deltaX = 0;
		float deltaY = 0;
		int width = getWidth();
		int height = getHeight();
		// 如果宽或高大于屏幕则控制范围
		if (rectF.width() >= width) {
			if (rectF.left > 0) {
				deltaX = -rectF.left;
			}
			if (rectF.right < width) {
				deltaX = width - rectF.right;
			}
		}
		if (rectF.height() >= height) {
			if (rectF.top > 0) {
				deltaY = -rectF.top;
			}
			if (rectF.bottom < height) {
				deltaY = height - rectF.bottom;
			}
		}
		// 如果宽或高小于屏幕，则居中
		if (rectF.width() < width) {
			deltaX = width * 0.5f + rectF.right * 0.5f - rectF.right;
		}
		if (rectF.height() < height) {
			deltaY = height * 0.5f + rectF.height() * 0.5f - rectF.bottom;
		}
		mScaleMatrix.postTranslate(deltaX, deltaY);
	}

	/**
	 * 根据当前图片的matrix获取图片的范围
	 * 
	 * @return
	 */
	private RectF getMatrixRectF() {
		Matrix matrix = mScaleMatrix;
		RectF rectF = new RectF();
		Drawable drawable = getDrawable();
		if (null != drawable) {
			// 这样rect.left，rect.right,rect.top,rect.bottom分别就是当前屏幕离你的图片的边界的距离。
			rectF.set(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
			matrix.mapRect(rectF);
		}
		return rectF;
	}

	/**
	 * 是否是拖动行为
	 * 
	 * @param dx
	 * @param dy
	 * @return
	 */
	private boolean isCanDrag(float dx, float dy) {
		return Math.sqrt((dx * dx) + (dy * dy)) >= mTouchSlop;
	}

	private class AutoScaleRunnable implements Runnable {

		static final float BIGGER = 1.07f;
		static final float SMALLER = 0.93f;
		private float mTargetScale;
		private float tmpScale;
		// 缩放中心
		private float x;
		private float y;

		/**
		 * 传入目标缩放值，根据目标值与当前值，判断应该是放大还是缩小
		 * 
		 * @param targetScale
		 * @param x
		 * @param y
		 */
		public AutoScaleRunnable(float targetScale, float x, float y) {
			this.mTargetScale = targetScale;
			this.x = x;
			this.y = y;
			if (getScale() < mTargetScale) {
				tmpScale = BIGGER;
			} else {
				tmpScale = SMALLER;
			}
		}

		@Override
		public void run() {
			// 进行缩放
			mScaleMatrix.postScale(tmpScale, tmpScale, x, y);
			checkBoundsAndCenterWhenScale();
			setImageMatrix(mScaleMatrix);

			final float currentScale = getScale();
			// 如果值在合法范围内，则继续缩放
			if (((tmpScale > 1.0f) && (currentScale < mTargetScale)) || ((tmpScale < 1.0f) && (mTargetScale < currentScale))) {
				ZoomImageView.this.postDelayed(this, 16);
			} else {
				// 设置为目标的缩放比例
				final float deltaScale = mTargetScale / currentScale;
				mScaleMatrix.postScale(deltaScale, deltaScale, x, y);
				checkBoundsAndCenterWhenScale();
				setImageMatrix(mScaleMatrix);
				isAutoScale = false;
			}
		}

	}
}
