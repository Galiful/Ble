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

public class DefCirLeft extends View {

	private Matrix matrix;

	private Paint mPaint;

	private Paint mClickPaint;

	private RectF mRectFBig;

	private RectF mRectFLittle;

	private Path mPathTop;
	private Path mPathBottom;
	private Path mPathCenter;

	private float mInitSweepAngle = -90;
	private float mBigSweepAngle = 86;// 86
	private float mLittleSweepAngle = 84;// 84

	private float mBigMarginAngle;
	private float mLittleMarginAngle;

	private List<Region> mList;

	private Region mAllRegion;

	private Region mRegionTop;
	private Region mRegionBottom;
	private Region mRegionCenter;

	private int mRadius;

	private static final int TOP = 0;
	private static final int BOTTOM = 1;
	private static final int CENTER = 2;

	private int mClickFlag = -1;

	private int mWidth;

	private int mCurX, mCurY;

	public static MotionEvent btnEvent;// ��ť�¼�

	private RegionBtnClickListener mListener;

	public DefCirLeft(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		initView();
	}

	public DefCirLeft(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView();
	}

	public DefCirLeft(Context context) {
		super(context);
		initView();
	}

	public void setListener(RegionBtnClickListener mListener) {
		this.mListener = mListener;
	}

	private void initView() {
		mPaint = new Paint();
		mPaint.setStyle(Style.FILL);
		mPaint.setAntiAlias(true);
		mPaint.setColor(Color.parseColor("#1fc5dd"));// �����ɫ#16abf9/2e6ac2

		mClickPaint = new Paint(mPaint);
		mClickPaint.setColor(Color.parseColor("#1bc58e"));// ������ɫ

		mPathTop = new Path();
		mPathBottom = new Path();
		mPathCenter = new Path();
		mList = new ArrayList<>();
		mRegionTop = new Region();
		mRegionBottom = new Region();
		mRegionCenter = new Region();

		mBigMarginAngle = 90 - mBigSweepAngle;// 90
		mLittleMarginAngle = 90 - mLittleSweepAngle;// 90
	}

	private void initPath() {
		mList.clear();

		// ��ʼ��top·��
		mPathTop.addArc(mRectFBig, mInitSweepAngle - mBigSweepAngle / 2,
				mBigSweepAngle);
		mPathTop.arcTo(mRectFLittle, mInitSweepAngle + mLittleSweepAngle / 2,
				-mLittleSweepAngle);
		mPathTop.close();

		// ����top������
		mRegionTop.setPath(mPathTop, mAllRegion);
		mList.add(mRegionTop);

		// ��ʼ��bottom·��
		mPathBottom.addArc(mRectFBig, mInitSweepAngle - mBigSweepAngle / 2 + 2
				* (mBigMarginAngle + mBigSweepAngle), mBigSweepAngle);
		mPathBottom.arcTo(mRectFLittle, mInitSweepAngle + mLittleSweepAngle / 2
				+ 2 * (mLittleMarginAngle + mLittleSweepAngle),
				-mLittleSweepAngle);
		mPathBottom.close();

		// ����bottom������
		mRegionBottom.setPath(mPathBottom, mAllRegion);
		mList.add(mRegionBottom);

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
		Bitmap bmp = BitmapFactory.decodeResource(getResources(),
				R.drawable.dir);
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
		matrix = new Matrix();
		matrix.postTranslate(-48, -48);
		canvas.drawBitmap(bmpOK, matrix, mPaint);

		// ��
		canvas.drawPath(mPathTop, mPaint);
		matrix = new Matrix();
		matrix.postRotate(270, bmp.getWidth() / 2, bmp.getHeight() / 2);
		matrix.postTranslate(-bmp.getWidth() / 2, -250);
		canvas.drawBitmap(bmp, matrix, mPaint);

		// ��
		canvas.drawPath(mPathBottom, mPaint);
		matrix = new Matrix();
		matrix.postRotate(90, bmp.getWidth() / 2, bmp.getHeight() / 2);
		matrix.postTranslate(-bmp.getWidth() / 2, 210);
		canvas.drawBitmap(bmp, matrix, mPaint);

		// ����Բ
		Paint paintCir = new Paint();
		paintCir.setColor(Color.parseColor("#1fc5dd"));
		paintCir.setStyle(Style.STROKE);// ���������ǿ���Բ
		paintCir.setStrokeWidth(8);// ���û��ʴ�ϸ
		canvas.drawCircle(0, 0, mWidth / 2 - 2, paintCir);

		switch (mClickFlag) {
		case BOTTOM:
			canvas.drawPath(mPathBottom, mClickPaint);
			break;
		case TOP:
			canvas.drawPath(mPathTop, mClickPaint);
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
		// ��ȥ�Ƴ� ��λ��
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
				case BOTTOM:
					if (mListener != null) {
						mListener.clickBottom();
					}
					break;
				case TOP:
					if (mListener != null) {
						mListener.clickTop();
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
				case BOTTOM:
					if (mListener != null) {
						mListener.clickBottom();
					}
					break;
				case TOP:
					if (mListener != null) {
						mListener.clickTop();
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
				case BOTTOM:
					if (mListener != null) {
						mListener.clickBottom();
					}
					break;
				case TOP:
					if (mListener != null) {
						mListener.clickTop();
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
			Log.i("aaa", "TOP ");
			return TOP;
		case 1:
			Log.i("aaa", "BOTTOM");
			return BOTTOM;
		case 2:
			Log.i("aaa", "CENTER");
			return CENTER;
		default:
			return -1;
		}
	}

	public interface RegionBtnClickListener {
		/**
		 * �ϱ߰�ť�������
		 */
		public void clickTop();

		/**
		 * �±߰�ť�������
		 */
		public void clickBottom();

		/**
		 * �м䰴ť�������
		 */
		public void clickCenter();
	}

}
