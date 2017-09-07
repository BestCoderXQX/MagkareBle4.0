package com.maiji.magkareble40;

import android.widget.ImageView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import java.util.ArrayList;

/**
* @author xqx
* @email djlxqx@163.com
* blog:http://www.cnblogs.com/xqxacm/
* createAt 2017/9/6
* description:  扫描得到的蓝牙设备列表适配器
*/

public class ScanDeviceAdapter extends BaseQuickAdapter<String , BaseViewHolder> {

    public ScanDeviceAdapter(ArrayList<String> datas) {
        super(R.layout.item_device, datas);
    }

    @Override
    protected void convert(BaseViewHolder helper, String item) {
        helper.setText(R.id.txtMac,item);

    }
}