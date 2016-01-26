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
	// ��ʼ��ʱ�����ű��������ͼƬ��Ȼ��߸߶ȴ�����Ļ���ֵС��1
	private float initScale = 1.0f;
	// ���ڴ�ž����9��ֵ
	private final float[] matrixValues = new float[9];
	// �Ƿ�Ϊ��һ����ʾͼƬ
	private boolean once = true;

	private float mLastX, mLastY;
	private boolean isCanDrag;
	private int lastPointCount;
	// �����ƶ��¼�����̾���
	private int mTouchSlop;
	private boolean isCheckTopAndBottom = true;
	private boolean isCheckLeftAndRight = true;
	// ���ŵ����Ƽ��
	private ScaleGestureDetector mScaleGestureDetector = null;
	private final Matrix mScaleMatrix = new Matrix();

	// ͼƬ�����Matrix
	private Matrix mFinalScaleMatrix = new Matrix();

	// ˫�����
	private GestureDetector mGestureDetector;
	private boolean isAutoScale;

	public ZoomImageView(Context context) {
		this(context, null);
	}

	public ZoomImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// ����ImageViewΪMatrix��ֻ�����ò��ܽ��о���任
		super.setScaleType(ScaleType.MATRIX);
		// ��ʼ�����Ƽ����
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
		// �����ƶ�����С����
		mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
		// ���ô������������¼��������Ƽ��������
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
			// �õ�ͼƬ�Ŀ�Ⱥ͸߶�
			int dw = drawable.getIntrinsicWidth();
			int dh = drawable.getIntrinsicHeight();
			float scale = 1.0f;
			// ���ͼƬ�ĸ߶ȺͿ�ȴ�����Ļ������������Ļ�Ŀ���߸�
			if (dw > width && dh <= height) {
				scale = width * 1.0f / dw;
			}
			if (dh > height && dw <= width) {
				scale = height * 1.0f / dh;
			}
			// �����Ⱥ͸߶ȶ�������Ļ�������䰴������Ӧ��Ļ��С
			if (dh > height && dw > width) {
				scale = Math.min(width * 1.0f / dw, height * 1.0f / dh);
			}
			initScale = scale;
			// ��Ļ�ƶ�����Ļ���ģ�ע��������ƽ�Ƶľ��룬������ƽ��Ŀ�ĵص�����
			mScaleMatrix.postTranslate((width - dw) / 2, (height - dh) / 2);
			// �������ĵ�Ϊ�������ĵ�����
			mScaleMatrix.postScale(scale, scale, getWidth() / 2, getHeight() / 2);
			setImageMatrix(mScaleMatrix);
			once = false;
		}
	}

	/**
	 * �����¼�����ScaleGestureDetector����
	 */
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (mGestureDetector.onTouchEvent(event)) {
			return true;
		}
		// ���¼�����ScaleGestureDector����
		mScaleGestureDetector.onTouchEvent(event);
		float x = 0, y = 0;
		// �õ�������ĸ���
		final int pointCount = event.getPointerCount();
		// �õ�����������x��y�ľ�ֵ
		for (int i = 0; i < pointCount; i++) {
			x += event.getX(i);
			y += event.getY(i);
		}
		x = x / pointCount;
		y = y / pointCount;
		// ÿ�������㷢���仯ʱ������mLastX,mLastY
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
				// �������ؼ������¼�
				getParent().requestDisallowInterceptTouchEvent(true);
			}
			break;
		case MotionEvent.ACTION_MOVE:
			if (rectF.width() > getWidth() || rectF.height() > getHeight()) {
				// �������ؼ������¼�
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
					// ������С����Ļ��ȣ����ֹ�����ƶ�
					if (rectF.width() < getWidth()) {
						dx = 0;
						isCheckLeftAndRight = false;
					}
					// ����߶�С����Ļ�߶ȣ����ֹ�����ƶ�
					if (rectF.height() < getHeight()) {
						dy = 0;
						isCheckTopAndBottom = false;
					}
					mScaleMatrix.postTranslate(dx, dy);
					checkMatrixBounds();
					setImageMatrix(mScaleMatrix);
				}
			}
			// ��ʱ����
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
	 * �ƶ��߽��жϣ���Ҫ�жϿ�Ȼ��߸߶ȴ�����Ļ��
	 */
	private void checkMatrixBounds() {
		RectF rectF = getMatrixRectF();
		float deltaX = 0, deltaY = 0;
		final float viewWidth = getWidth();
		final float viewHeight = getHeight();
		// �ж��ƶ������ź�ͼƬ�Ƿ񳬳���Ļ�߽�
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
		// ScaleGestureDetector�������ָ�ķֿ��ͺ�£�����һ����������
		// mScaleFactor = getCurrentSpan() / getPreviousSpan(); ��Сʱ������Խ��ֵԽС
		float scaleFactor = detector.getScaleFactor();
		if (getDrawable() == null) {
			return true;
		}
		// ���ŵķ�Χ����
		if ((scale < SCALE_MAX && scaleFactor > 1.0f) || (scale > initScale && scaleFactor < 1.0f)) {
			// ���ֵ��Сֵ���ж�
			if (scaleFactor * scale < initScale) {
				scaleFactor = initScale / scale;
			}
			if (scaleFactor * scale > SCALE_MAX) {
				scaleFactor = SCALE_MAX / scale;
			}
			// �������ű���
			// mScaleMatrix.postScale(scaleFactor, scaleFactor, getWidth() / 2,
			// getHeight() / 2);
			// ������������
			// <--�����������ŵ�ᵼ��ͼƬ�Ļ���߽���ְ׵�-->
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
	 * ��õ�ǰ�����ű���
	 * 
	 * @return
	 */
	public final float getScale() {
		mScaleMatrix.getValues(matrixValues);
		return matrixValues[Matrix.MSCALE_X];
	}

	/**
	 * �÷���������onResume��������֮��
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
	 * ����ʱ���߽��ͼƬ��ʾ��Χ�Ŀ���
	 */
	private void checkBoundsAndCenterWhenScale() {
		RectF rectF = getMatrixRectF();
		float deltaX = 0;
		float deltaY = 0;
		int width = getWidth();
		int height = getHeight();
		// ������ߴ�����Ļ����Ʒ�Χ
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
		// �������С����Ļ�������
		if (rectF.width() < width) {
			deltaX = width * 0.5f + rectF.right * 0.5f - rectF.right;
		}
		if (rectF.height() < height) {
			deltaY = height * 0.5f + rectF.height() * 0.5f - rectF.bottom;
		}
		mScaleMatrix.postTranslate(deltaX, deltaY);
	}

	/**
	 * ���ݵ�ǰͼƬ��matrix��ȡͼƬ�ķ�Χ
	 * 
	 * @return
	 */
	private RectF getMatrixRectF() {
		Matrix matrix = mScaleMatrix;
		RectF rectF = new RectF();
		Drawable drawable = getDrawable();
		if (null != drawable) {
			// ����rect.left��rect.right,rect.top,rect.bottom�ֱ���ǵ�ǰ��Ļ�����ͼƬ�ı߽�ľ��롣
			rectF.set(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
			matrix.mapRect(rectF);
		}
		return rectF;
	}

	/**
	 * �Ƿ����϶���Ϊ
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
		// ��������
		private float x;
		private float y;

		/**
		 * ����Ŀ������ֵ������Ŀ��ֵ�뵱ǰֵ���ж�Ӧ���ǷŴ�����С
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
			// ��������
			mScaleMatrix.postScale(tmpScale, tmpScale, x, y);
			checkBoundsAndCenterWhenScale();
			setImageMatrix(mScaleMatrix);

			final float currentScale = getScale();
			// ���ֵ�ںϷ���Χ�ڣ����������
			if (((tmpScale > 1.0f) && (currentScale < mTargetScale)) || ((tmpScale < 1.0f) && (mTargetScale < currentScale))) {
				ZoomImageView.this.postDelayed(this, 16);
			} else {
				// ����ΪĿ������ű���
				final float deltaScale = mTargetScale / currentScale;
				mScaleMatrix.postScale(deltaScale, deltaScale, x, y);
				checkBoundsAndCenterWhenScale();
				setImageMatrix(mScaleMatrix);
				isAutoScale = false;
			}
		}

	}
}
