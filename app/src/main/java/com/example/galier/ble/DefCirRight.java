package com.example.galier.ble;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class DefCirRight extends View {

	private Matrix matrix;

	private Paint mPaint;

	private Paint mClickPaint;

	private RectF mRectFBig;

	private RectF mRectFLittle;

	private Path mPathLeft;
	private Path mPathRight;
	private Path mPathCenter;

	private float mInitSweepAngle = 0;
	private float mBigSweepAngle = 86;// 86
	private float mLittleSweepAngle = 84;// 84

	private float mBigMarginAngle;
	private float mLittleMarginAngle;

	private List<Region> mList;

	private Region mAllRegion;

	private Region mRegionRight;
	private Region mRegionLeft;
	private Region mRegionCenter;

	private int mRadius;

	private static final int LEFT = 0;
	private static final int RIGHT = 1;
	private static final int CENTER = 2;

	private int mClickFlag = -1;

	private int mWidth;

	private int mCurX, mCurY;

	public static MotionEvent btnEvent;// ��ť�¼�

	private RegionRightClickListener mListener;

	public DefCirRight(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		initView();
	}

	public DefCirRight(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView();
	}

	public DefCirRight(Context context) {
		super(context);
		initView();
	}

	public void setListener(RegionRightClickListener mListener) {
		this.mListener = mListener;
	}

	private void initView() {
		mPaint = new Paint();
		mPaint.setStyle(Style.FILL);
		mPaint.setAntiAlias(true);
		mPaint.setColor(Color.parseColor("#1fc5dd"));// �����ɫ#16abf9/2e6ac2

		mClickPaint = new Paint(mPaint);
		mClickPaint.setColor(Color.parseColor("#1bc58e"));// ������ɫ

		mPathLeft = new Path();
		mPathRight = new Path();
		mPathCenter = new Path();

		mList = new ArrayList<>();
		mRegionLeft = new Region();
		mRegionRight = new Region();
		mRegionCenter = new Region();

		mBigMarginAngle = 90 - mBigSweepAngle;// 90
		mLittleMarginAngle = 90 - mLittleSweepAngle;// 90
	}

	private void initPath() {
		mList.clear();

		// ��ʼ��icon_right·��
		mPathRight.addArc(mRectFBig, mInitSweepAngle - mBigSweepAngle / 2,
				mBigSweepAngle);
		mPathRight.arcTo(mRectFLittle, mInitSweepAngle + mLittleSweepAngle / 2,
				-mLittleSweepAngle);
		mPathRight.close();

		// ����icon_right������
		mRegionRight.setPath(mPathRight, mAllRegion);
		mList.add(mRegionRight);

		// ��ʼ��icon_left·��
		mPathLeft.addArc(mRectFBig, mInitSweepAngle - mBigSweepAngle / 2 + 2
				* (mBigMarginAngle + mBigSweepAngle), mBigSweepAngle);
		mPathLeft.arcTo(mRectFLittle, mInitSweepAngle + mLittleSweepAngle / 2
				+ 2 * (mLittleMarginAngle + mLittleSweepAngle),
				-mLittleSweepAngle);
		mPathLeft.close();

		// ����icon_left������
		mRegionLeft.setPath(mPathLeft, mAllRegion);
		mList.add(mRegionLeft);

		// ��ʼ��center·��
		mPathCenter.addCircle(0, 0, mRadius, Path.Direction.CW);
		mPathCenter.close();

		// ����center������
		mRegionCenter.setPath(mPathCenter, mAllRegion);
		mList.add(mRegionCenter);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		mWidth = Math.min(getMeasuredWidth(), getMeasuredHeight());
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		// ��ͷͼ��
		Bitmap bmpc = BitmapFactory.decodeResource(getResources(),
				R.drawable.cut);
		Bitmap bmpa = BitmapFactory.decodeResource(getResources(),
				R.drawable.add);
		Bitmap bmpOK = BitmapFactory.decodeResource(getResources(),
				R.drawable.ok);

		// ��Ļ�ߴ�
		DisplayMetrics dm = getResources().getDisplayMetrics();
		int sHeigth = dm.heightPixels;
		int sWidth = dm.widthPixels;

		canvas.save();
		canvas.translate(getMeasuredWidth() / 2, getMeasuredHeight() / 2);

		// ��
		canvas.drawPath(mPathCenter, mPaint);

		// ��
		canvas.drawPath(mPathRight, mPaint);
		matrix = new Matrix();
		matrix.postTranslate(215, -bmpa.getWidth() / 2);
		canvas.drawBitmap(bmpa, matrix, mPaint);

		// ��
		canvas.drawPath(mPathLeft, mPaint);
		matrix = new Matrix();
		matrix.postTranslate(-260, -bmpc.getWidth() / 2);
		canvas.drawBitmap(bmpc, matrix, mPaint);

		// ����Բ
		Paint paintCir = new Paint();
		paintCir.setColor(Color.parseColor("#1fc5dd"));
		paintCir.setStyle(Style.STROKE);// ���������ǿ���Բ
		paintCir.setStrokeWidth(8);// ���û��ʴ�ϸ
		canvas.drawCircle(0, 0, mWidth / 2 - 2, paintCir);

		switch (mClickFlag) {
		case RIGHT:
			canvas.drawPath(mPathRight, mClickPaint);
			break;
		case LEFT:
			canvas.drawPath(mPathLeft, mClickPaint);
			break;
		case CENTER:
			canvas.drawPath(mPathCenter, mClickPaint);
			break;
		}

		canvas.restore();
	}

	// ���û��ߺ��м䰴ť�ߴ�
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);

		mAllRegion = new Region(-mWidth, -mWidth, mWidth, mWidth);

		mRectFBig = new RectF(-mWidth / 2, -mWidth / 2, mWidth / 2, mWidth / 2);// ��ť���

		mRectFLittle = new RectF(-mWidth / 4, -mWidth / 4, mWidth / 4,// ��ť�ڿ�
				mWidth / 4);

		mRadius = mWidth / 5;

		initPath();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// ��ȥ�Ƴ���λ��
		mCurX = (int) event.getX() - getMeasuredWidth() / 2;
		mCurY = (int) event.getY() - getMeasuredHeight() / 2;

		// ��ȡ��ť�¼�
		btnEvent = event;

		switch (event.getAction()) {
		// ����
		case MotionEvent.ACTION_DOWN:
			containRect(mCurX, mCurY);

			if (mClickFlag != -1) {
				switch (mClickFlag) {
				case RIGHT:
					if (mListener != null) {
						mListener.clickRight();
					}
					break;
				case LEFT:
					if (mListener != null) {
						mListener.clickLeft();
					}
					break;
				}
			}

			invalidate();
			break;
		case MotionEvent.ACTION_MOVE:
			if (mClickFlag != -1) {
				containRect(mCurX, mCurY);

				// ����
				switch (mClickFlag) {
				case RIGHT:
					if (mListener != null) {
						mListener.clickRight();
					}
					break;
				case LEFT:
					if (mListener != null) {
						mListener.clickLeft();
					}
					break;
				}
			}
			invalidate();
			break;
		// �ɿ�
		case MotionEvent.ACTION_UP:
			if (mClickFlag != -1) {
				switch (mClickFlag) {
				case RIGHT:
					if (mListener != null) {
						mListener.clickRight();
					}
					break;
				case LEFT:
					if (mListener != null) {
						mListener.clickLeft();
					}
					break;
				case CENTER:
					if (mListener != null) {
						mListener.clickCenter();
					}
					break;
				}

				mClickFlag = -1;
			}

			invalidate();
			break;
		default:
			break;
		}

		return true;
	}

	public void containRect(int x, int y) {
		int index = -1;
		for (int i = 0; i < mList.size(); i++) {
			if (mList.get(i).contains(x, y)) {
				mClickFlag = switchRect(i);
				index = i;
				break;
			}
		}

		if (index == -1) {
			mClickFlag = -1;
		}
	}

	public int switchRect(int i) {
		switch (i) {
		case 0:
			Log.i("aaa", "RIGHT ");
			return RIGHT;
		case 1:
			Log.i("aaa", "LEFT");
			return LEFT;
		case 2:
			Log.i("aaa", "CENTER");
			return CENTER;
		default:
			return -1;
		}
	}

	public interface RegionRightClickListener {
		/**
		 * ��߰�ť�������
		 */
		public void clickLeft();

		/**
		 * �ұ߰�ť�������
		 */
		public void clickRight();

		/**
		 * �м䰴ť�������
		 */
		public void clickCenter();
	}

}
