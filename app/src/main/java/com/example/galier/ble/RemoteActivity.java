package com.example.galier.ble;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import com.example.galier.ble.DefCirLeft.RegionBtnClickListener;
import com.example.galier.ble.DefCirRight.RegionRightClickListener;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class RemoteActivity extends Activity {

	private String userID;
	public static final String MUSERID = "";
	private ImageButton btnBack;
	private Button btnGear, btnCom;
	private DefCirLeft mRegionViewLeft;
	private DefCirRight mRegionViewRight;
	private Toast mToast;
	private String angleSpeed = "180";// ���ٶ�Ĭ�ϡ�180��
	private String speed = "3";// �ٶ�Ĭ�ϡ�5��
	private boolean sendLongPress = false, angleLongPress = false;// �Ƿ񳤰�

	// �Ƕ�ѡ��
	TextView txtAngle;
	private int angleVal = 0;

	// ͨѶģʽ
	private int choiceComOption = 0;

	// SocketͨѶ
	private Socket socketclient = null;// Socket
	private String sendCmd, sendFlag, angleFlag;// ����ָ����ͱ�ʶ���Ƕȱ�ʶ

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_remote);

		// ȫ��ʵ��
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		// ��ȡ�û�ID
//		Intent intent = getIntent();
//		userID = intent.getStringExtra(MUSERID);
		userID = "M";
		// ��ʼ��ayout
		initLayout();

		// ��ʼ��ť
		initButton();

		// ��ʼ��ָ��
		InitCmd();
	}

	// ����UserID
	public static void setRemoteActivity(Context context, String userID) {
		Intent intent = new Intent(context, RemoteActivity.class);
		intent.putExtra(MUSERID, userID);
		context.startActivity(intent);
	}

	// ��ʼ��layout
	private void initLayout() {
		// ����
		btnBack = (ImageButton) findViewById(R.id.BtnBack);
		btnBack.setOnClickListener(btnBackOnClickListener);

		// ��λ
		btnGear = (Button) findViewById(R.id.BtnGear);

		// �Ƕ�
		txtAngle = (TextView) findViewById(R.id.TxtAngle);

		// ͨѶ
		btnCom = (Button) findViewById(R.id.BtnCom);
		btnCom.setOnClickListener(BtnComOnClickListener);
	}

	// ���ذ�ť
	private final OnClickListener btnBackOnClickListener = new OnClickListener() {
		@Override
		public void onClick(final View view) {
			Intent intenMsg = new Intent(RemoteActivity.this,
					MainActivity.class);
			startActivity(intenMsg);
		}
	};

	// ͨѶ�л�
	private final OnClickListener BtnComOnClickListener = new OnClickListener() {
		@Override
		public void onClick(final View view) {
			if (choiceComOption == 0) {
				choiceComOption = 1;
				btnCom.setText("WIFI");
			} else if (choiceComOption == 1) {
				choiceComOption = 2;
				btnCom.setText("BT");
			} else if (choiceComOption == 2) {
				choiceComOption = 0;
				btnCom.setText("4G");
			}
		}
	};

	// ��ʼ����ť
	private void initButton() {
		// �ж��û��Ƿ����
		if (userID.equals("M")) {
			showToast("���ֻ���δע��");
			return;
		}

		/* ��߰�ť */
		mRegionViewLeft = (DefCirLeft) findViewById(R.id.regionleft);
		mRegionViewLeft.setListener(new RegionBtnClickListener() {

			// ֱ��
			@Override
			public void clickTop() {
				sendFlag = "1";// ָ���ʶ
				btnGear.setText("D");
				LongTouchSendCmd(DefCirLeft.btnEvent);
			}

			// ����
			@Override
			public void clickBottom() {
				sendFlag = "2";// ָ���ʶ
				btnGear.setText("R");
				LongTouchSendCmd(DefCirLeft.btnEvent);
			}

			// ȷ��
			@Override
			public void clickCenter() {
				sendFlag = "3";// ָ���ʶ
				btnGear.setText("OK");
				connectServerWithTCPSocket();
			}
		});

		/* �ұ߰�ť */
		mRegionViewRight = (DefCirRight) findViewById(R.id.regionright);
		mRegionViewRight.setListener(new RegionRightClickListener() {

			// ����
			@Override
			public void clickLeft() {
				angleFlag = "0";
				LongTouchChgAngle(DefCirRight.btnEvent);
			}

			// ����
			@Override
			public void clickRight() {
				angleFlag = "1";
				LongTouchChgAngle(DefCirRight.btnEvent);
			}

			// ����
			@Override
			public void clickCenter() {
				angleFlag = "2";
				angleVal = 0;
				txtAngle.setText("0");
			}
		});
	}

	// SocketͨѶ�߳�
	protected void connectServerWithTCPSocket() {
		// �ж��û��Ƿ����
		if (userID == null || userID.equals("M")) {
			userID = "M0";
		}

		// �̷߳���ָ��
		new Thread() {
			@Override
			public void run() {
				connectServerWithTCPSoc();
			}
		}.start();
	}

	// SocketͨѶ
	protected void connectServerWithTCPSoc() {
		try {
			// ��ȡָ��
			switch (sendFlag) {
			// ģʽ
			case "0":
				sendCmd = "0000000";// ����ָ��
				break;
			// ǰ��
			case "1":
				if (angleVal == 0)
					sendCmd = "1000;0;" + speed;// ����ָ��
				else if (angleVal < 0)
					sendCmd = "2;D;" + angleSpeed + ";" + Math.abs(angleVal)
							+ ";" + speed;// ����ָ��
				else if (angleVal > 0)
					sendCmd = "3000;D;" + angleSpeed + ";" + Math.abs(angleVal)
							+ ";" + speed;// ����ָ��
				break;
			// ����
			case "2":
				if (angleVal == 0)
					sendCmd = "40000;0;" + speed;// ����ָ��
				else if (angleVal < 0)
					sendCmd = "2;R;" + angleSpeed + ";" + Math.abs(angleVal)
							+ ";" + speed;// ����ָ��
				else if (angleVal > 0)
					sendCmd = "3000;R;" + angleSpeed + ";" + Math.abs(angleVal)
							+ ";" + speed;// ����ָ��
				break;
			// ȷ��
			case "3":
				sendCmd = "500000";// ����ָ��
				break;
			// ֹͣ
			case "4":
				sendCmd = "1111111111";// ����ͣ��10λ
				break;
			// Ĭ��
			default:
				sendCmd = "";
				break;
			}

			// �ж��Ƿ�Ϊ��
			if (sendCmd.equals("")) {
				return;
			}

			// 4G
			if (choiceComOption == 0) {
				socketclient = new Socket("61.191.217.247", 8899);
				String socketData = "YH CM 0 :" + userID + "|3|" + sendCmd
						+ " 0\\r\\n";
				BufferedWriter writer = new BufferedWriter(
						new OutputStreamWriter(socketclient.getOutputStream()));
				writer.write(socketData.replace("\n", " ") + "\n");
				writer.flush();
				socketclient.close();
			}
			// WIFI
			else if (choiceComOption == 1) {
				socketclient = new Socket("192.168.0.104", 16);
				String socketData = "YH CM 0 :" + userID + "|3|" + sendCmd
						+ " 0\\r\\n";
				BufferedWriter writer = new BufferedWriter(
						new OutputStreamWriter(socketclient.getOutputStream()));
				writer.write(socketData);
				writer.flush();
				socketclient.close();
			}
			// ����
			else if (choiceComOption == 2) {

			}

		} catch (UnknownHostException e) {
			showToast("����ʧ�ܣ���������");
			e.printStackTrace();
		} catch (IOException e) {
			showToast("����ʧ�ܣ���������");
			e.printStackTrace();
		}
	}

	// ��ʼ��ָ��
	private void InitCmd() {
		sendFlag = "0";// ָ���ʶ
		connectServerWithTCPSocket();
	}

	// ��������
	private void LongTouchSendCmd(MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN: {
			sendLongPress = true;
			new Thread() {
				public void run() {
					super.run();
					while (true) {
						if (sendLongPress == true)// ����������������
						{
							try {
								connectServerWithTCPSoc();// ����ָ��
								Thread.sleep(500);// 1�뷢��һ��
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						} else {
							sendFlag = "4";// ָ���ʶ
							connectServerWithTCPSoc();
							break;// û�а��£��˳�ѭ��
						}
					}
				}
			}.start();
			break;
		}
		case MotionEvent.ACTION_UP: {
			sendLongPress = false;
		}
		case MotionEvent.ACTION_MOVE: {
		}
		}

	}
	
	Handler handler = new Handler( ) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            txtAngle.setText(String.valueOf(angleVal));
        }
    };

	// �����Ӽ��Ƕ�
	private void LongTouchChgAngle(MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN: {
			angleLongPress = true;
			new Thread() {
				public void run() {
					super.run();
					while (true) {
						if (angleLongPress == true)
						{
							try {
								if (angleFlag == "0" && angleVal > -485) {
									angleVal = angleVal - 10;
								} else if (angleFlag == "1" && angleVal < 485) {
									angleVal = angleVal + 10;
								}
								handler.sendEmptyMessage(2);
								Thread.sleep(50);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						} else {
							break;
						}
					}
				}
			}.start();
			break;
		}
		case MotionEvent.ACTION_UP: {
			angleLongPress = false;
		}
		case MotionEvent.ACTION_MOVE: {
		}
		}

	}

	// ������Ϣ����
	private void showToast(String message) {
		if (mToast == null) {
			mToast = Toast.makeText(RemoteActivity.this, message,
					Toast.LENGTH_SHORT);
		} else {
			mToast.setText(message);
		}
		mToast.show();
	}

	@Override
	protected void onResume() {
		super.onResume();
		sendLongPress = false;
		angleLongPress = false;
	}

	@Override
	protected void onPause() {
		super.onPause();
		// ��activity����ǰ̨��ֹͣ��ʱ
		sendLongPress = false;
		angleLongPress = false;
	}

	@Override
	protected void onDestroy() {
		// ����ʱ
		sendLongPress = false;
		angleLongPress = false;
		super.onDestroy();
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		switch (ev.getAction()) {
		case MotionEvent.ACTION_DOWN:
			break;
		case MotionEvent.ACTION_UP:
			// ̧��ʱ������ʱ
			sendLongPress = false;
			angleLongPress = false;
			break;
		}
		return super.dispatchTouchEvent(ev);

	}

}
