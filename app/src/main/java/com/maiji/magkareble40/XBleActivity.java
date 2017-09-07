package com.maiji.magkareble40;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.blakequ.bluetooth_manager_lib.BleManager;
import com.blakequ.bluetooth_manager_lib.BleParamsOptions;
import com.blakequ.bluetooth_manager_lib.connect.BluetoothSubScribeData;
import com.blakequ.bluetooth_manager_lib.connect.ConnectConfig;
import com.blakequ.bluetooth_manager_lib.connect.ConnectState;
import com.blakequ.bluetooth_manager_lib.connect.ConnectStateListener;
import com.blakequ.bluetooth_manager_lib.connect.multiple.MultiConnectManager;

import java.util.ArrayList;
import java.util.UUID;

import de.greenrobot.event.EventBus;

/**
* @author xqx
* @email djlxqx@163.com
* blog:http://www.cnblogs.com/xqxacm/
* createAt 2017/9/6
* description:  ble 4.0 多设备连接
*/

public class XBleActivity extends Activity implements View.OnClickListener {

    private Button btnSelectDevice ;  //选择需要绑定的设备
    private Button btnStartConnect ;  //开始连接按钮

    private TextView txtContentMac ; //获取到的数据解析结果显示

    private final int REQUEST_CODE_PERMISSION = 1; // 权限请求码  用于回调

    MultiConnectManager multiConnectManager ;  //多设备连接
    private BluetoothAdapter bluetoothAdapter;   //蓝牙适配器


    private ArrayList<String> connectDeviceMacList ; //需要连接的mac设备集合
    ArrayList<BluetoothGatt> gattArrayList; //设备gatt集合


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_xble);

        initVariables();
        initView();
        initEvent();
        requestWritePermission();
        initConfig();  // 蓝牙初始设置
        EventBus.getDefault().register(this);
    }

    private void initVariables() {
        connectDeviceMacList = new ArrayList<>();
        gattArrayList = new ArrayList<>();

    }

    private void initEvent() {
        btnSelectDevice.setOnClickListener(this);
        btnStartConnect.setOnClickListener(this);
    }

    private void initView() {
        btnSelectDevice = (Button) findViewById(R.id.btnSelectDevice);
        btnStartConnect = (Button) findViewById(R.id.btnStartConnect);
        txtContentMac = (TextView) findViewById(R.id.txtContentMac);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btnSelectDevice:
                // 扫描并选择需要连接的设备
                Intent intent = new Intent();
                intent.setClass(this,SelectDeviceActivity.class);
                startActivityForResult(intent,1);
                break;
            case R.id.btnStartConnect:
                connentBluetooth();
                break;
        }
    }


    /**
     * 连接需要连接的传感器
     * @param
     */
    private void connentBluetooth(){

        String[] objects = connectDeviceMacList.toArray(new String[connectDeviceMacList.size()]);
        multiConnectManager.addDeviceToQueue(objects);
        multiConnectManager.addConnectStateListener(new ConnectStateListener() {
            @Override
            public void onConnectStateChanged(String address, ConnectState state) {
                switch (state){
                    case CONNECTING:
                        Log.i("connectStateX","设备:"+address+"连接状态:"+"正在连接");
                        break;
                    case CONNECTED:
                        Log.i("connectStateX","设备:"+address+"连接状态:"+"成功");
                        break;
                    case NORMAL:
                        Log.i("connectStateX","设备:"+address+"连接状态:"+"失败");
                        break;
                }
            }
        });

        /**
         * 数据回调
         */

        multiConnectManager.setBluetoothGattCallback(new BluetoothGattCallback() {
            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);
                dealCallDatas(gatt , characteristic);
            }
        });

        multiConnectManager.setServiceUUID("0000ffe5-0000-1000-8000-00805f9a34fb");
        multiConnectManager.addBluetoothSubscribeData(
                new BluetoothSubScribeData.Builder().setCharacteristicNotify(UUID.fromString("0000ffe4-0000-1000-8000-00805f9a34fb")).build());

        //还有读写descriptor
        //start descriptor(注意，在使用时当回调onServicesDiscovered成功时会自动调用该方法，所以只需要在连接之前完成1,3步即可)
        for (int i = 0; i < gattArrayList.size(); i++) {
            multiConnectManager.startSubscribe(gattArrayList.get(i));
        }

        multiConnectManager.startConnect();

    }

    /**
     * 处理回调的数据
     * @param gatt
     * @param characteristic
     */

    float[][] floats = new float[7][30];

    private void dealCallDatas(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        int position = connectDeviceMacList.indexOf(gatt.getDevice().getAddress());
        //第一个传感器数据
        byte[] value = characteristic.getValue();
        if (value[0] != 0x55) {
            //开头不是0x55的数据删除
            return;
        }
        switch (value[1]) {
            case 0x61:
                //加速度数据
                floats[position][3] = ((((short) value[3]) << 8) | ((short) value[2] & 0xff)) / 32768.0f * 16;   //x轴
                floats[position][4] = ((((short) value[5]) << 8) | ((short) value[4] & 0xff)) / 32768.0f * 16;   //y轴
                floats[position][5] = ((((short) value[7]) << 8) | ((short) value[6] & 0xff)) / 32768.0f * 16;   //z轴
                //角速度数据
                floats[position][6] = ((((short) value[9]) << 8) | ((short) value[8] & 0xff)) / 32768.0f * 2000;  //x轴
                floats[position][7] = ((((short) value[11]) << 8) | ((short) value[10] & 0xff)) / 32768.0f * 2000;  //x轴
                floats[position][8] = ((((short) value[13]) << 8) | ((short) value[12] & 0xff)) / 32768.0f * 2000;  //x轴
                break;
            case 0x62:
                //四元素
                floats[position][13] = ((((short) value[3]) << 8) | ((short) value[2] & 0xff)) / 32768.0f; // q1
                floats[position][14] = ((((short) value[5]) << 8) | ((short) value[4] & 0xff)) / 32768.0f; // q2
                floats[position][15] = ((((short) value[7]) << 8) | ((short) value[6] & 0xff)) / 32768.0f; // q3
                floats[position][16] = ((((short) value[9]) << 8) | ((short) value[8] & 0xff)) / 32768.0f; // q4
                //电池电压
                floats[position][21] = (float) 1.2 * 4 * (((value[11] << 8) | value[10]) + 1) / 1024;
                //充电状态
                floats[position][22] = value[12];
                //低电压报警
                floats[position][23] = value[14];
                break;
        }
        EventBus.getDefault().post(new RefreshDatas()); // 发送消息，更新UI 显示数据
    }

    /**
     * 对蓝牙的初始化操作
     */
    private void initConfig() {
        multiConnectManager = BleManager.getMultiConnectManager(this);
        // 获取蓝牙适配器

        try {
            // 获取蓝牙适配器
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            //
            if (bluetoothAdapter == null) {
                Toast.makeText(this, "蓝牙不可用", Toast.LENGTH_LONG).show();
                return;
            }

            // 蓝牙没打开的时候打开蓝牙
            if (!bluetoothAdapter.isEnabled())
                bluetoothAdapter.enable();
        }catch (Exception err){};
        BleManager.setBleParamsOptions(new BleParamsOptions.Builder()
                .setBackgroundBetweenScanPeriod(5 * 60 * 1000)
                .setBackgroundScanPeriod(10000)
                .setForegroundBetweenScanPeriod(2000)
                .setForegroundScanPeriod(10000)
                .setDebugMode(BuildConfig.DEBUG)
                .setMaxConnectDeviceNum(7)            //最大可以连接的蓝牙设备个数
                .setReconnectBaseSpaceTime(1000)
                .setReconnectMaxTimes(Integer.MAX_VALUE)
                .setReconnectStrategy(ConnectConfig.RECONNECT_LINE_EXPONENT)
                .setReconnectedLineToExponentTimes(5)
                .setConnectTimeOutTimes(20000)
                .build());
    }

    /**
     * @author xqx
     * @email djlxqx@163.com
     * blog:http://www.cnblogs.com/xqxacm/
     * createAt 2017/8/30
     * description:  权限申请相关，适配6.0+机型 ，蓝牙，文件，位置 权限
     */

    private String[] allPermissionList = {Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE
    };


    /**
     * 遍历出需要获取的权限
     */
    private void requestWritePermission() {
        ArrayList<String> permissionList = new ArrayList<>();
        // 将需要获取的权限加入到集合中  ，根据集合数量判断 需不需要添加
        for (int i = 0; i < allPermissionList.length; i++) {
            if (PackageManager.PERMISSION_DENIED == ContextCompat.checkSelfPermission(this, allPermissionList[i])){
                permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }

        String permissionArray[] = new String[permissionList.size()];
        for (int i = 0; i < permissionList.size(); i++) {
            permissionArray[i] = permissionList.get(i);
        }
        if (permissionList.size() > 0)
            ActivityCompat.requestPermissions(this, permissionArray, REQUEST_CODE_PERMISSION);
    }


    /**
     * 权限申请的回调
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSION){
            if (permissions[0].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    &&grantResults[0] == PackageManager.PERMISSION_GRANTED){
                //用户同意使用write

            }else{
                //用户不同意，自行处理即可
                Toast.makeText(XBleActivity.this,"您取消了权限申请,可能会影响软件的使用,如有问题请退出重试",Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data!=null){
            switch (requestCode){
                case 1:
                    connectDeviceMacList = data.getStringArrayListExtra("data");
                    Log.i("xqxinfo","需要连接的mac"+connectDeviceMacList.toString());
                    //获取设备gatt对象
                    for (int i = 0; i < connectDeviceMacList.size(); i++) {
                        BluetoothGatt gatt = bluetoothAdapter.getRemoteDevice(connectDeviceMacList.get(i)).connectGatt(this, false, new BluetoothGattCallback() {
                        });
                        gattArrayList.add(gatt);
                        Log.i("xqxinfo","添加了"+connectDeviceMacList.get(i));
                    }
                    break;
            }
        }
    }

    public void onEventMainThread(RefreshDatas event) {
        txtContentMac.setText("");
        for (int i = 0; i < connectDeviceMacList.size(); i++) {
            txtContentMac.append("Mac:"+connectDeviceMacList.get(i)+"\n");
            txtContentMac.append("加速度:"+floats[i][3]+"-"+floats[i][4]+"-"+floats[i][5]+"\n");
            txtContentMac.append("角速度:"+floats[i][6]+"-"+floats[i][7]+"-"+floats[i][8]+"\n");
            txtContentMac.append("四元素:"+floats[i][13]+"-"+floats[i][14]+"-"+floats[i][15]+"-"+floats[i][16]+"\n");
            txtContentMac.append("电池电压:"+floats[i][21]+"\n");
            txtContentMac.append("充电状态:"+floats[i][22]+"\n");
            txtContentMac.append("低电压报警:"+floats[i][23]+"\n\n");
        }
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}
