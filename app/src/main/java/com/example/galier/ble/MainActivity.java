package com.example.galier.ble;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.BleDevice;
import cn.com.heaton.blelibrary.ble.L;
import cn.com.heaton.blelibrary.ble.callback.BleConnectCallback;
import cn.com.heaton.blelibrary.ble.callback.BleNotiftCallback;
import cn.com.heaton.blelibrary.ble.callback.BleScanCallback;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnTouchListener {
    private ImageButton ibup, ibdown, ibleft, ibright, ibreset, ibsignal, ibok;
    public static TextView tvState, tvAngle, tvSignal;
    private ImageView ivCar;
    public final String[] authBaseArr = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION
    };
    private int choiceComOption = 0;

    private String angleSpeed = "180";
    private String speed = "3";
    private String sendFlag = "0", angleFlag, sendCmd;
    private int angleVal = 0;
    private volatile boolean sendLongPress = false, angleLongPress = false;
    private final static String TAG = MainActivity.class.getSimpleName();
    public static boolean isFirstOpen = true;

    public static final int MIN_CLICK_DELAY_TIME = 500;
    long upTime = 0;
    long downTime = 0;
    private Socket socketclient = null;
    private String userID = "M0";
    public static String[] devicesArray;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 21) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_main);
        initView();
        if (choiceComOption == 0) {
            reConnect();
        }
    }

    /**
     * 增加自动重连
     */
    public void reConnect() {
        SharedPreferences settings = getSharedPreferences("DeviceInfo", MODE_PRIVATE);
        Set<String> set = new HashSet<String>();//HashSet会报set未初始化
        set = settings.getStringSet("Device", set);
        devicesArray = (String[]) set.toArray(new String[set.size()]);//Object[] deviceArray = set.toArray();同上
//        devicesArray = devicesArray[0].split(",");
//        String deviceAddress = devicesArray[0].split(",")[1];
        Log.e(TAG, "DeviceInfo: " + Arrays.toString(devicesArray) + " set.size: " + set.size());
        requestPermission();
        if(set.size()!=0){
            initBle();
            checkBluetoothStatus();
        }
    }

    //请求权限
    public void requestPermission() {
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }
    }
    private void initBle() {
        DeviceManage.mBle = Ble.options()
                .setLogBleExceptions(true)//设置是否输出打印蓝牙日志
                .setThrowBleException(true)//设置是否抛出蓝牙异常
                .setAutoConnect(false)//设置是否自动连接
                .setConnectFailedRetryCount(3)
                .setConnectTimeout(10 * 1000)//设置连接超时时长
                .setScanPeriod(10 * 1000)//设置扫描时长
                .setUuid_service(UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb"))//设置主服务的uuid
                .setUuid_write_cha(UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb"))//设置可写特征的uuid
                .create(getApplicationContext());
    }
    //检查蓝牙是否支持及打开
    public void checkBluetoothStatus() {
        // 检查设备是否支持BLE4.0
        if (!DeviceManage.mBle.isSupportBle(this)) {
            Toast.makeText(getApplicationContext(), "ble_not_supported", Toast.LENGTH_SHORT).show();
        }
        if (!DeviceManage.mBle.isBleEnable()) {
            //4、若未打开，则请求打开蓝牙
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, Ble.REQUEST_ENABLE_BT);
        } else {
            //5、若已打开，则进行扫描
            DeviceManage.mBle.startScan(scanCallback);
        }
    }

    /**
     * 扫描的回调
     */
    public BleScanCallback<BleDevice> scanCallback = new BleScanCallback<BleDevice>() {
        @Override
        public void onLeScan(final BleDevice device, int rssi, byte[] scanRecord) {
            synchronized (DeviceManage.mBle.getLocker()) {
                Log.e(TAG, "device.scanBleAddress: " +device.getBleName() + "(" + device.getBleAddress() + ")");
                for (String address : devicesArray) {
                    if (device.getBleAddress().equals(address)) {
                        DeviceManage.mBle.connect(address, connectCallback);
                        DeviceManage.mBle.stopScan();//连接后即停止扫描
//                        DeviceManage.mBle.write(device,"123".getBytes(),new DeviceManage().bleDeviceBleWriteCallback);
                    }
                }
                Log.e("devicesArray", Arrays.toString(devicesArray));


            }
        }
        @Override
        public void onStop() {
            super.onStop();
            L.e(TAG, " scanStop");
        }
    };
    /**
     * 连接的回调
     */
    private BleConnectCallback<BleDevice> connectCallback = new BleConnectCallback<BleDevice>() {
        @Override
        public void onConnectionChanged(BleDevice device) {
            Log.e(TAG, "onConnectionChanged: " + device.isConnected()+" "+device.getConnectionState() +" "+ Thread.currentThread().getName());
            if (device.isConnected()) {
                DeviceManage.isConnect = true;
                DeviceManage.mBle.startNotify(device, bleNotiftCallback);
                Toast.makeText(getApplicationContext(), "已连接:" + device.getBleName() + "(" + device.getBleAddress() + ")", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onConnectException(BleDevice device, int errorCode) {
            super.onConnectException(device, errorCode);
            DeviceManage.isConnect = false;
            tvState.setText("未连接");
            String introduce="其他原因";
            if (errorCode == 2523) {
                introduce = "Mcu连接断开或者是信号弱等原因断开";
            } else if (errorCode == 2521) {
                introduce = "连接失败";
            } else if (errorCode == 2522) {
                introduce = "状态异常";
            } else if (errorCode == 2510) {
                introduce = "连接超时";
            }
            showSnackBar(ivCar, "连接异常:" + "("+introduce+")");
        }

        @Override
        public void onConnectTimeOut(BleDevice device) {
            super.onConnectTimeOut(device);
            Toast.makeText(getApplicationContext(), "连接超时:" + device.getBleName(), Toast.LENGTH_SHORT).show();
        }
    };
    /**
     * 通知的回调
     */
    private BleNotiftCallback<BleDevice> bleNotiftCallback = new BleNotiftCallback<BleDevice>() {
        @Override
        public void onChanged(BleDevice device, BluetoothGattCharacteristic characteristic) {
            UUID uuid = characteristic.getUuid();
            if (device.isConnectting()) {
                Toast.makeText(getApplicationContext(), "连接成功", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "连接断开", Toast.LENGTH_SHORT).show();
            }
            Log.e(TAG, "onChanged==uuid:" + uuid.toString());
            Log.e(TAG, "onChanged==address:" + device.getBleAddress());
        }

        /**
         *  Set the notification here when the service finds a callback       setNotify
         * @param gatt
         */
        @Override
        public void onNotifySuccess(BluetoothGatt gatt) {
            super.onNotifySuccess(gatt);
            onResume();//
            Log.e(TAG,"2be successful and can send data");
        }
    };

    /**
     * 旋转后重写
     *
     * @param newConfig
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.activity_main);
        initView();
        if (!DeviceManage.isConnect) {
            tvState.setText("未连接");
        } else {
            tvState.setText("已连接");
        }
    }

    private void initView() {
        ibup = (ImageButton) findViewById(R.id.imageButton_up);
        ibdown = (ImageButton) findViewById(R.id.imageButton_down);
        ibleft = (ImageButton) findViewById(R.id.imageButton_left);
        ibright = (ImageButton) findViewById(R.id.imageButton_right);
        ibreset = (ImageButton) findViewById(R.id.imageButton_reset);
        ibsignal = (ImageButton) findViewById(R.id.imageButton_signal);
        ibok = (ImageButton) findViewById(R.id.imageButton_ok);
        ivCar = (ImageView) findViewById(R.id.imageView_car);

        tvState = (TextView) findViewById(R.id.tv_state);
        tvAngle = (TextView) findViewById(R.id.tv_angle);
        tvSignal = (TextView) findViewById(R.id.textView_signal);

        ibreset.setOnClickListener(this);
        ibsignal.setOnClickListener(this);
        ibok.setOnClickListener(this);
        ivCar.setOnClickListener(this);
        tvSignal.setOnClickListener(this);
        tvSignal.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (choiceComOption == 0) {
                    startActivity(new Intent(MainActivity.this, DeviceManage.class));
                } else {
                    showSnackBar(tvSignal, "开发中");
                }
                return false;
            }
        });

        ibup.setOnTouchListener(this);
        ibdown.setOnTouchListener(this);
        ibleft.setOnTouchListener(this);
        ibright.setOnTouchListener(this);
        final FloatingActionButton action = (FloatingActionButton) findViewById(R.id.fab_bluetooth);
        action.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, DeviceManage.class));
            }
        });
    }

    protected String setSendCmd() {

        switch (sendFlag) {
            case "0":
                sendCmd = "0000000";
                break;
            case "1":
                if (angleVal == 0)
                    sendCmd = "1000;0;" + speed;
                else if (angleVal < 0)
                    sendCmd = "2;D;" + angleSpeed + ";" + Math.abs(angleVal)
                            + ";" + speed;
                else if (angleVal > 0)
                    sendCmd = "3000;D;" + angleSpeed + ";" + Math.abs(angleVal)
                            + ";" + speed;
                break;
            case "2":
                if (angleVal == 0)
                    sendCmd = "40000;0;" + speed;
                else if (angleVal < 0)
                    sendCmd = "2;R;" + angleSpeed + ";" + Math.abs(angleVal)
                            + ";" + speed;
                else if (angleVal > 0)
                    sendCmd = "3000;R;" + angleSpeed + ";" + Math.abs(angleVal)
                            + ";" + speed;
                break;
            case "3":
                sendCmd = "500000";
                break;
            case "4":
                sendCmd = "1111111111";
                break;
            default:
                sendCmd = "";
                break;
        }
//        if (sendCmd.equals("")) {
//            return;
//        }
        String socketData = "YH CM 0 :" + userID + "|3|" + sendCmd
                + " 0\\r\\n";
        return socketData;
    }

    @Override
    protected void onResume() {
        super.onResume();
        sendLongPress = false;
        angleLongPress = false;
        if (!DeviceManage.isConnect) {
            tvState.setText("未连接");
            Log.e(TAG, "onResume：setText(\"未连接\")");
        } else if (isFirstOpen) {
            showSnackBar(ibdown, "蓝牙已连接,进入遥控模式");
            tvState.setText("已连接");
            isFirstOpen = false;
            sendFlag = "0";
            new DeviceManage().sendCmd(setSendCmd());
            Log.e(TAG, "onResume：setText(\"已连接\")");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sendLongPress = false;
        angleLongPress = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sendLongPress = false;
        angleLongPress = false;
//        SharedPreferences settings = getSharedPreferences("DeviceAddress", MODE_PRIVATE);
////            if(settings.edit()!=null){
////                settings.edit().clear();
////            }
//        if (mBle.getConnetedDevices().size() != 0) {
//            settings.edit().putString("DeviceAddress", mBle.getConnetedDevices().get(0).getBleAddress()).apply();
//            Log.e(TAG, "onDestroy " + mBle.getConnetedDevices().get(0).getBleAddress());
//        }
        if (DeviceManage.mBle != null) {
            DeviceManage.mBle.destory(getApplicationContext());
            Log.e(TAG, "MainActivity onDestroy");
        }
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.imageButton_up:
                break;
            case R.id.imageButton_down:
                break;
            case R.id.imageButton_left:
                break;
            case R.id.imageButton_right:
                break;
            case R.id.imageButton_reset:
                angleVal = 0;
                tvAngle.setText(String.valueOf(angleVal));
                ivCar.animate().rotation(0);
                break;
            case R.id.imageButton_ok:
                if (choiceComOption == 0 && !DeviceManage.isConnect) {
                    showSnackBar(tvSignal, "蓝牙未连接");
                    break;
                }
                sendFlag = "3";
                new Thread() {
                    @Override
                    public void run() {
                        super.run();
                        connectServerWithTCPSoc();
                    }
                }.start();
                break;
            case R.id.imageButton_signal:
//                startActivity(new Intent(MainActivity.this, DeviceManage.class));
                break;
            case R.id.imageView_car:
                angleVal = 0;
                tvAngle.setText(String.valueOf(angleVal));
                ivCar.animate().rotation(0);
                break;
            case R.id.textView_signal:
                if (choiceComOption == 0) {
                    choiceComOption = 1;
                    tvSignal.setText("WIFI");
                    new Thread() {
                        @Override
                        public void run() {
                            super.run();
                            try {
                                if (socketclient != null) {
                                    socketclient.close();
                                }
                                if (DeviceManage.mBle != null) {
                                    DeviceManage.mBle.destory(getApplicationContext());
                                    Log.e(TAG, "MainActivity mBle onDestroy");
                                }
                                socketclient = new Socket("10.151.232.250", 8989);
                                handler.sendEmptyMessage(101);
                                sendFlag = "0";
                                connectServerWithTCPSoc();
                            } catch (IOException e) {
                                handler.sendEmptyMessage(102);
                                showSnackBar(tvSignal, e.toString());
                                e.printStackTrace();
                            }
                        }
                    }.start();

                } else if (choiceComOption == 1) {
                    choiceComOption = 2;
                    tvSignal.setText("4G");
                    new Thread() {
                        @Override
                        public void run() {
                            super.run();
                            try {
                                if (socketclient != null) {
                                    socketclient.close();
                                }
                                if (DeviceManage.mBle != null) {
                                    DeviceManage.mBle.destory(getApplicationContext());
                                    Log.e(TAG, "MainActivity mBle onDestroy");
                                }
                                socketclient = new Socket("10.151.232.250", 8989);
                                handler.sendEmptyMessage(101);
                                sendFlag = "0";
                                connectServerWithTCPSoc();
                            } catch (IOException e) {
                                handler.sendEmptyMessage(102);
                                showSnackBar(tvSignal, e.toString());
                                e.printStackTrace();
                            }
                        }
                    }.start();
                } else if (choiceComOption == 2) {
                    choiceComOption = 0;
                    tvSignal.setText("BT");
                    reConnect();
//                    if (DeviceManage.isConnect) {
//                        tvState.setText("已连接");
//                    } else {
//                        tvState.setText("未连接");
//                    }
                    try {
                        if (socketclient != null) {
                            socketclient.close();
                        }
                    } catch (IOException e) {
                        showSnackBar(tvSignal, e.toString());
                        e.printStackTrace();
                    }
                }
                break;
            default:
                break;
        }
    }

    /**
     * 屏蔽快速点击的touch
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {

        switch (v.getId()) {
            case R.id.imageButton_up:
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    downTime = Calendar.getInstance().getTimeInMillis();
                }
                if (downTime - upTime > MIN_CLICK_DELAY_TIME) {
//                    upTime = Calendar.getInstance().getTimeInMillis();
                    if (choiceComOption == 0 && !DeviceManage.isConnect && event.getAction() == MotionEvent.ACTION_DOWN) {
                        showSnackBar(tvSignal, "蓝牙未连接");
                        return false;
                    }
                    sendFlag = "1";
                    LongTouchSendCmd(event);
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    upTime = Calendar.getInstance().getTimeInMillis();
                }
                break;
            case R.id.imageButton_down:
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    downTime = Calendar.getInstance().getTimeInMillis();
                }
                if (downTime - upTime > MIN_CLICK_DELAY_TIME) {
                    if (choiceComOption == 0 && !DeviceManage.isConnect && event.getAction() == MotionEvent.ACTION_DOWN) {
                        showSnackBar(tvSignal, "蓝牙未连接");
                        return false;
                    }
                    sendFlag = "2";
                    LongTouchSendCmd(event);
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    upTime = Calendar.getInstance().getTimeInMillis();
                }
                break;
            case R.id.imageButton_left:
                angleFlag = "0";
                LongTouchChgAngle(event);
                break;
            case R.id.imageButton_right:
                angleFlag = "1";
                LongTouchChgAngle(event);
                break;
        }
        return false;
    }

    private void LongTouchSendCmd(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                sendLongPress = true;
                new Thread() {
                    public void run() {
                        super.run();
                        Looper.prepare();
                        while (true) {
                            if (sendLongPress == true) {
                                try {
                                    connectServerWithTCPSoc();
                                    Thread.sleep(500);//
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                sendFlag = "4";
                                connectServerWithTCPSoc();
                                break;
                            }
                        }
                        Looper.loop();
                    }
                }.start();
                break;
            }
            case MotionEvent.ACTION_UP: {
                sendLongPress = false;
            }
        }
    }

    protected void connectServerWithTCPSoc() {
        try {
            // 4G
            if (choiceComOption == 2) {
//                socketclient = new Socket("61.191.217.247", 8899);
//                socketclient = new Socket("10.151.232.250", 8989);
//                String socketData = "YH CM 0 :" + userID + "|3|" + setSendCmd()
//                        + " 0\\r\\n";
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(socketclient.getOutputStream()));
                writer.write(setSendCmd().replace("\n", " ") + "\n");
                writer.flush();
//                socketclient.close();
            }
            // WIFI
            else if (choiceComOption == 1) {
//                socketclient = new Socket("192.168.0.104", 16);
//                socketclient = new Socket("10.151.232.250", 8989);
//                String socketData = "YH CM 0 :" + userID + "|3|" + setSendCmd()
//                        + " 0\\r\\n";
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(socketclient.getOutputStream()));
                writer.write(setSendCmd());
                writer.flush();
//                socketclient.close();
            }
            //BT
            else if (choiceComOption == 0) {
                new DeviceManage().sendCmd(setSendCmd());
            }
        } catch (UnknownHostException e) {
            showSnackBar(tvSignal, "UnknownHost:" + e.toString());
            e.printStackTrace();
        } catch (IOException e) {
            showSnackBar(tvSignal, "error:" + e.toString());
            e.printStackTrace();
        }
    }

    private void LongTouchChgAngle(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                angleLongPress = true;
                new Thread() {
                    public void run() {
                        super.run();
                        Looper.prepare();
                        while (true) {
                            if (angleLongPress == true) {
                                try {
                                    if (angleFlag == "0" && angleVal > -485) {
                                        if (angleVal == -480 || angleVal == 485) {
                                            angleVal = angleVal - 5;
                                        } else {
                                            angleVal = angleVal - 10;
                                        }
                                    } else if (angleFlag == "1" && angleVal < 485) {
                                        if (angleVal == 480 || angleVal == -485) {
                                            angleVal = angleVal + 5;
                                        } else {
                                            angleVal = angleVal + 10;
                                        }
                                    }
                                    handler.sendEmptyMessage(100);
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                break;
                            }
                        }
                        Looper.loop();
                    }
                }.start();
                break;
            }
            case MotionEvent.ACTION_UP: {
                angleLongPress = false;
            }
        }

    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 100) {
                tvAngle.setText(String.valueOf(angleVal));
//                ivCar.animate().rotation(angleVal / (485 / 45));
                ivCar.setPivotX(ivCar.getWidth() / 2);
                ivCar.setPivotY(ivCar.getHeight() / 2);
                ivCar.setRotation(angleVal / (485 / 45));
            } else if (msg.what == 101) {
                if (socketclient.isConnected()) {
                    tvState.setText("已连接");
                }
            } else if (msg.what == 102) {
                tvState.setText("未连接");
            }
        }
    };


    public byte[] hexStr2byte(String hexStr) {
        StringBuffer stringBuffer = new StringBuffer(hexStr);
        if (hexStr.length() % 2 == 1) {
//            stringBuffer.insert(hexStr.length()-1,"0");
//            hexStr=stringBuffer.toString();
            hexStr += "0";
        }
        hexStr = hexStr.toUpperCase();
        hexStr = hexStr.trim();
        String str = "0123456789ABCDEF";
        char[] hexs = hexStr.toCharArray();
        byte[] bytes = new byte[hexStr.length() / 2];
        int n;

        for (int i = 0; i < bytes.length; i++) {
            n = str.indexOf(hexs[2 * i]) * 16;
            n += str.indexOf(hexs[2 * i + 1]);
            bytes[i] = (byte) (n & 0xff);
        }
        return bytes;
    }

    public byte[] hexToByte(String string) {
        if (string.length() <= 0) {
            return new byte[0];
        } else {
            String var1 = string.replace(" ", "");
            if (var1.length() % 2 == 1) {
                StringBuffer var2 = new StringBuffer(var1);
                var2.insert(var1.length() - 1, '0');
                var1 = var2.toString();
            }

            byte[] var5 = new byte[var1.length() / 2];
            String var3 = "";

            for (int var4 = 0; var4 < var1.length(); var4 += 2) {
                var3 = var1.substring(var4, var4 + 2);
                if (var4 == 0) {
                    var5[var4] = (byte) Integer.parseInt(var3, 16);
                } else {
                    var5[var4 / 2] = (byte) Integer.parseInt(var3, 16);
                }
            }
            return var5;
        }
    }

    @SuppressLint("ResourceAsColor")
    public static void showSnackBar(View view, String msg) {
        Snackbar snackbar = Snackbar.make(view, msg, Snackbar.LENGTH_LONG);
        snackbar.setAction(" ", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        });
        //内容
        TextView tvSnackbarText = (TextView) snackbar.getView().findViewById(R.id.snackbar_text);
        tvSnackbarText.setTextColor(Color.parseColor("#FF152B38"));
        //onClick
        snackbar.getView().setBackgroundColor(Color.parseColor("#ffffff"));
        snackbar.setActionTextColor(Color.parseColor("#FF152B38"));
        snackbar.show();
    }

}
