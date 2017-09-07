package com.maiji.magkareble40;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.blakequ.bluetooth_manager_lib.BleManager;
import com.blakequ.bluetooth_manager_lib.BleParamsOptions;
import com.blakequ.bluetooth_manager_lib.connect.ConnectConfig;
import com.blakequ.bluetooth_manager_lib.scan.BluetoothScanManager;
import com.blakequ.bluetooth_manager_lib.scan.ScanOverListener;
import com.blakequ.bluetooth_manager_lib.scan.bluetoothcompat.ScanCallbackCompat;
import com.blakequ.bluetooth_manager_lib.scan.bluetoothcompat.ScanFilterCompat;
import com.blakequ.bluetooth_manager_lib.scan.bluetoothcompat.ScanResultCompat;
import com.chad.library.adapter.base.BaseQuickAdapter;

import java.util.ArrayList;
import java.util.List;

/**
* @author xqx
* @email djlxqx@163.com
* blog:http://www.cnblogs.com/xqxacm/
* createAt 2017/9/6
* description:  扫描蓝牙设备  选择需要连接的传感器
*/

public class SelectDeviceActivity extends Activity implements View.OnClickListener {

    private Button btnScan;        //开始扫描按钮
    private Button btnStopScan;   //停止扫描按钮
    private Button btnOk;   //选择好了需要连接的mac设备

    BluetoothScanManager scanManager ;


    /* 列表相关 */
    private RecyclerView recyclerView ; //列表
    private ScanDeviceAdapter adapter;
    private ArrayList<String> deviceMacs ; // 数据源 ： 所有扫描到的设备mac地址

    private ArrayList<String> selectDeviceMacs; // 选择的需要连接的设备的mac集合

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_device);

        deviceMacs = new ArrayList<>();
        selectDeviceMacs = new ArrayList<>();
        initView();
        initEvent();
        initBle();


    }

    /**
     * 初始化蓝牙相关配置
     */
    private void initBle() {
        scanManager = BleManager.getScanManager(this);

        scanManager.setScanOverListener(new ScanOverListener() {
            @Override
            public void onScanOver() {
            }
        });


        scanManager.setScanCallbackCompat(new ScanCallbackCompat() {
            @Override
            public void onBatchScanResults(List<ScanResultCompat> results) {
                super.onBatchScanResults(results);
            }

            @Override
            public void onScanFailed(final int errorCode) {
                super.onScanFailed(errorCode);

            }

            @Override
            public void onScanResult(int callbackType, ScanResultCompat result) {
                super.onScanResult(callbackType, result);
                //scan result
                // 只有当前列表中没有该mac地址的时候 添加
                if (!deviceMacs.contains(result.getDevice().getAddress())) {
                    deviceMacs.add(result.getDevice().getAddress());
                    adapter.notifyDataSetChanged();
                }
            }
        });

    }

    private void initEvent() {
        btnScan.setOnClickListener(this);
        btnStopScan.setOnClickListener(this);
        btnOk.setOnClickListener(this);
    }

    private void initView() {
        btnScan = (Button) findViewById(R.id.btnScan);
        btnStopScan = (Button) findViewById(R.id.btnStopScan);
        btnOk = (Button) findViewById(R.id.btnOk);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);

        // 列表相关初始化
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ScanDeviceAdapter(deviceMacs);

        adapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                if (!selectDeviceMacs.contains(deviceMacs.get(position))){
                    //如果改item的mac不在已选中的mac集合中 说明没有选中，添加进已选中mac集合中，状态改为"已选择"
                    selectDeviceMacs.add(deviceMacs.get(position));
                    ((TextView)view.findViewById(R.id.txtState)).setText("已选择");
                }else {
                    selectDeviceMacs.remove(deviceMacs.get(position));
                    ((TextView)view.findViewById(R.id.txtState)).setText("未选择");
                }
            }
        });

        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btnScan:
                //开始 扫描
//                scanManager.startCycleScan(); //不会立即开始，可能会延时
                scanManager.startScanNow(); //立即开始扫描
                break;

            case R.id.btnStopScan:
                // 如果正在扫描中 停止扫描
                if (scanManager.isScanning()) {
                    scanManager.stopCycleScan();
                }
                break;
            case R.id.btnOk:
                Intent intent = new Intent();
                intent.putExtra("data",selectDeviceMacs);                // 设置结果，并进行传送
                this.setResult(1, intent);
                this.finish();
                break;

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 如果正在扫描中 停止扫描
        if (scanManager.isScanning()) {
            scanManager.stopCycleScan();
        }
    }
}
