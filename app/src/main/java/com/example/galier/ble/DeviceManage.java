package com.example.galier.ble;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.example.galier.ble.command.AppProtocol;
import com.example.galier.ble.command.CommandBean;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.BleDevice;
import cn.com.heaton.blelibrary.ble.L;
import cn.com.heaton.blelibrary.ble.callback.BleConnectCallback;
import cn.com.heaton.blelibrary.ble.callback.BleMtuCallback;
import cn.com.heaton.blelibrary.ble.callback.BleNotiftCallback;
import cn.com.heaton.blelibrary.ble.callback.BleScanCallback;
import cn.com.heaton.blelibrary.ble.callback.BleWriteCallback;
import cn.com.heaton.blelibrary.ble.callback.BleWriteEntityCallback;

public class DeviceManage extends AppCompatActivity {
    public static boolean isConnect;
    private static Toast mToast;
    public static Ble<BleDevice> mBle;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private String TAG = DeviceManage.class.getSimpleName();
    private ListView mListView;
    private Toolbar toolbar;
    private SwipeRefreshLayout mRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble);
        mListView = (ListView) findViewById(R.id.listView);
        mRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.mRefreshLayout);
        mListView.setOnItemClickListener(new ItemClickListener());
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mBle != null && !mBle.isScanning()) {
                            mLeDeviceListAdapter.clear();
                            mLeDeviceListAdapter.addDevices(mBle.getConnetedDevices());
                            mBle.startScan(scanCallback);
                        }
                        mRefreshLayout.setRefreshing(false);
                    }
                }, 5000);

            }
        });
        onInitView();
        initBle();
        Log.e(TAG, "getConnetedDevices2:" + Ble.getInstance().getConnetedDevices());
    }

    public void onInitView() {
        mLeDeviceListAdapter = new LeDeviceListAdapter(this);
        mListView.setAdapter(mLeDeviceListAdapter);
        //1、请求蓝牙相关权限
        requestPermission();
    }

    //请求权限
    public void requestPermission() {
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(DeviceManage.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }
    }

    //初始化蓝牙
    public void initBle() {
        if (mBle != null) {
            if (mBle.getConnetedDevices().size() > 0) {
                List<BleDevice> list = Ble.getInstance().getConnetedDevices();
                Log.e(TAG, "getConnetedDevices1:" + Ble.getInstance().getConnetedDevices());
                for (BleDevice device : list) {
                    mLeDeviceListAdapter.addDevice(device);
                    mLeDeviceListAdapter.notifyDataSetChanged();
                }
            } else if (mBle.getConnetedDevices().size() == 0 && !isConnect) {
//                mBle.destory(getApplicationContext());
//                Log.e(TAG,"destory");//重连后已经初始化了
            }
        } else {
            mBle = Ble.options()
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
//        mBle = Ble.create(getApplicationContext());
        //3、检查蓝牙是否支持及打开
        checkBluetoothStatus();
    }

    //检查蓝牙是否支持及打开
    public void checkBluetoothStatus() {
        // 检查设备是否支持BLE4.0
        if (!mBle.isSupportBle(this)) {
            Toast.makeText(getApplicationContext(), "ble_not_supported", Toast.LENGTH_SHORT).show();
        }
        if (!mBle.isBleEnable()) {
            //4、若未打开，则请求打开蓝牙
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, Ble.REQUEST_ENABLE_BT);
        } else {
            //5、若已打开，则进行扫描
            mBle.startScan(scanCallback);
        }
    }

    /**
     * 发送数据
     */
    public void sendData(View v) {
        List<BleDevice> list = Ble.getInstance().getConnetedDevices();
        Log.e(TAG, "sendData_list:" + String.valueOf(list));
        synchronized (mBle.getLocker()) {
            for (BleDevice device : list) {
                mBle.write(device, "11".getBytes(), bleDeviceBleWriteCallback);
                CommandBean commandBean = new CommandBean();
                AppProtocol.sendCarMoveCommand(device, commandBean.setCarCommand(80, 1));
////                AppProtocol.sendCarMoveCommand(device, commandBean.setOrderCommand(2, 1, null));
////                AppProtocol.sendCarMscCommand(device, commandBean.setMscCommand(C.Command.TF_MUSIC_TYPE, 1, (short) 121));
////                AppProtocol.sendMusicVolume(device, commandBean.setVolumeCommand(C.Command.TF_MUSIC_TYPE, 10));
            }
        }
    }

    public void sendCmd(String msg) {
        List<BleDevice> list = Ble.getInstance().getConnetedDevices();
        synchronized (mBle.getLocker()) {
            for (BleDevice device : list) {
//                mBle.setMTU(device.getBleAddress(),36);
//                mBle.write(device, msg.getBytes(), bleDeviceBleWriteCallback);
                mBle.writeEntity(device, msg.getBytes(), 20, 10, bleDeviceBleWriteEntityCallback);
//            mBle.writeEntity(mBle.getConnetedDevices().get(0),msg.getBytes(),20,500,bleDeviceBleWriteEntityCallback);
            }
        }
    }

    /**
     * writeEntity
     */
    private BleWriteEntityCallback<BleDevice> bleDeviceBleWriteEntityCallback = new BleWriteEntityCallback<BleDevice>() {
        @Override
        public void onWriteSuccess() {
            Log.e(TAG, "onWriteEntitySuccess: ");
        }

        @Override
        public void onWriteFailed() {

        }
    };
    /**
     * write的回调
     */
    public BleWriteCallback<BleDevice> bleDeviceBleWriteCallback = new BleWriteCallback<BleDevice>() {
        @Override
        public void onWriteSuccess(BluetoothGattCharacteristic characteristic) {
            Log.e(TAG, "onWriteSuccess: " + characteristic);
        }

    };

    /**
     * 扫描的回调
     */
    public BleScanCallback<BleDevice> scanCallback = new BleScanCallback<BleDevice>() {
        @Override
        public void onLeScan(final BleDevice device, int rssi, byte[] scanRecord) {
            synchronized (mBle.getLocker()) {
                Log.e(TAG, "device.scanBleAddress: "+device.getBleAddress());
                mLeDeviceListAdapter.addDevice(device);
                mLeDeviceListAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onStop() {
            super.onStop();
            L.e(TAG, "onStop: ");
        }
    };
    /**
     * 连接的回调
     */
    public BleConnectCallback<BleDevice> connectCallback = new BleConnectCallback<BleDevice>() {
        @Override
        public void onConnectionChanged(BleDevice device) {
            Log.e(TAG, "onConnectionChanged: " + device.isConnected()+" "+device.getConnectionState() +" "+ Thread.currentThread().getName());
            if (device.isConnected()) {
                /*连接成功后，设置通知*/
                isConnect = true;
                MainActivity.tvState.setText("已连接");
                mBle.startNotify(device, bleNotiftCallback);
            }
            mLeDeviceListAdapter.notifyDataSetChanged();
        }

        @Override
        public void onConnectException(BleDevice device, int errorCode) {
            super.onConnectException(device, errorCode);
            isConnect = false;
            MainActivity.isFirstOpen = true;
            MainActivity.tvState.setText("未连接");
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
            Toast.makeText(getApplicationContext(), "连接异常:" + "("+introduce+")", Toast.LENGTH_SHORT).show();
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
//            if (device.isConnectting()) {
//                Toast.makeText(getApplicationContext(), "连接成功", Toast.LENGTH_SHORT).show();
//            } else {
//                Toast.makeText(getApplicationContext(), "连接断开", Toast.LENGTH_SHORT).show();
//            }
            Log.e(TAG, "onChanged==uuid:" + uuid.toString());
            Log.e(TAG, "onChanged==address:" + device.getBleAddress());
        }
    };

    /**
     * 设置MTU的回调
     */
    private BleMtuCallback<BleDevice> bleDeviceBleMtuCallback = new BleMtuCallback<BleDevice>() {
        @Override
        public void onMtuChanged(BleDevice device, int mtu, int status) {
            super.onMtuChanged(device, mtu, status);
            Log.e("DeviceManage", "MTU: " + device + " " + mtu + " " + status);
        }
    };

    /**
     * SharedPreferences对一个Key多次存储新的Value（新的代表在内存中重新创建）每次都会覆盖上一个Value,
     * 对于Set对象也不例外，所以当我们通过get方法取得Set对象并向该Set对象添加新的值后再通过Put方法存入SharedPreferences中时
     * 并不能够更新Set内的值，因为该Set对象还是原来的Set对象，没有在内存中产生一个新的Set对象,解决方法就是：
     * keySet = new HashSet (keySet);
     */
    public void saveDeviceHistory(){
        SharedPreferences settings = getSharedPreferences("DeviceInfo", MODE_PRIVATE);
        if (settings.edit() != null) {
            settings.edit().clear();
        }
        if (mBle.getConnetedDevices().size() != 0) {
            Set<String> set = new HashSet<String>();
            if(settings.edit() != null){
                set = settings.getStringSet("Device", set);
                set = new HashSet<>(set);
            }
            List<BleDevice> list = Ble.getInstance().getConnetedDevices();
            for (BleDevice device : list) {
                set.add(device.getBleAddress());
            }
//            set.add(mBle.getConnetedDevices().get(0).getBleName()+","+mBle.getConnetedDevices().get(0).getBleAddress());
//            settings.edit().putString("DeviceAddress", mBle.getConnetedDevices().get(0).getBleAddress()).apply();
            settings.edit().putStringSet("Device",set).apply();
            Log.e(TAG, "onDestroy.getConnetedDevices()" + mBle.getConnetedDevices());
            Log.e(TAG, "onDestroy " + set);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBle.isScanning()) {
            mBle.stopScan();
        }
//        if (mBle != null) {
//            mBle.destory(getApplicationContext());
//        }

        saveDeviceHistory();
    }

    private class ItemClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final BleDevice device = mLeDeviceListAdapter.getDevice(position);
            if (device == null) return;
            if (mBle.isScanning()) {
                mBle.stopScan();
            }
            if (device.isConnected()) {
                mBle.disconnect(device, connectCallback);//断开蓝牙  有回调:不会重置自动重连属性
                isConnect = false;
                MainActivity.isFirstOpen = true;//手动断开重置isFirstOpen
            } else if (!device.isConnectting()) {
                //扫描到设备时   务必用该方式连接(是上层逻辑问题， 否则点击列表  虽然能够连接上，但设备列表的状态不会发生改变)
                mBle.connect(device, connectCallback);
                device.setAutoConnect(true);
//                mBle.addAutoPool(device);
                Log.e(TAG, "getConnetedDevices():" + String.valueOf(mBle.getConnetedDevices()));
                //此方\式只是针对不进行扫描连接（如上，若通过该方式进行扫描列表的连接  列表状态不会发生改变）
//            mBle.connect(device.getBleAddress(), connectCallback);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                List<BleDevice> list = Ble.getInstance().getConnetedDevices();
                for (BleDevice device : list) {
                    mBle.refreshDeviceCache(device.getBleAddress());
                }
                SharedPreferences settings = getSharedPreferences("DeviceInfo", MODE_PRIVATE);
                if (settings.edit() != null) {
                    settings.edit().clear().commit();
                    if(settings.edit().clear().commit()){
                        Toast.makeText(getApplicationContext(),"已清理",Toast.LENGTH_SHORT).show();
                    }
                    Log.e(TAG,"settings.edit().clear().apply(): "+settings.edit().clear().commit());
                }
                break;
            case R.id.menu_send:
                sendData(item.getActionView());
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showToast(String message) {
        if (mToast == null) {
            mToast = Toast.makeText(getApplicationContext(), message,
                    Toast.LENGTH_SHORT);
        } else {
            mToast.setText(message);
        }
        mToast.show();
    }

}
