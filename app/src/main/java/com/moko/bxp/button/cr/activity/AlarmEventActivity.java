package com.moko.bxp.button.cr.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.ConnectStatusEvent;
import com.moko.ble.lib.event.OrderTaskResponseEvent;
import com.moko.ble.lib.task.OrderTask;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.bxp.button.cr.AppConstants;
import com.moko.bxp.button.cr.databinding.ActivityAlarmEventBinding;
import com.moko.lib.bxpui.dialog.LoadingMessageDialog;
import com.moko.lib.bxpui.utils.ToastUtils;
import com.moko.bxp.button.cr.utils.Utils;
import com.moko.support.cr.CRMokoSupport;
import com.moko.support.cr.OrderTaskAssembler;
import com.moko.support.cr.entity.OrderCHAR;
import com.moko.support.cr.entity.ParamsKeyEnum;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

public class AlarmEventActivity extends BaseActivity {


    private ActivityAlarmEventBinding mBind;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBind = ActivityAlarmEventBinding.inflate(getLayoutInflater());
        setContentView(mBind.getRoot());

        EventBus.getDefault().register(this);
        if (!CRMokoSupport.getInstance().isBluetoothOpen()) {
            // 蓝牙未打开，开启蓝牙
            CRMokoSupport.getInstance().enableBluetooth();
        } else {
            showSyncingProgressDialog();
            ArrayList<OrderTask> orderTasks = new ArrayList<>();
            orderTasks.add(OrderTaskAssembler.getSystemTime());
            orderTasks.add(OrderTaskAssembler.getSinglePressEventCount());
            orderTasks.add(OrderTaskAssembler.getDoublePressEventCount());
            orderTasks.add(OrderTaskAssembler.getLongPressEventCount());
            CRMokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
        }
    }


    @Subscribe(threadMode = ThreadMode.POSTING, priority = 200)
    public void onConnectStatusEvent(ConnectStatusEvent event) {
        final String action = event.getAction();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (MokoConstants.ACTION_DISCONNECTED.equals(action)) {
                    // 设备断开，通知页面更新
                    AlarmEventActivity.this.finish();
                }
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 200)
    public void onOrderTaskResponseEvent(OrderTaskResponseEvent event) {
        EventBus.getDefault().cancelEventDelivery(event);
        final String action = event.getAction();
        runOnUiThread(() -> {
            if (MokoConstants.ACTION_ORDER_TIMEOUT.equals(action)) {
            }
            if (MokoConstants.ACTION_ORDER_FINISH.equals(action)) {
                dismissSyncProgressDialog();
            }
            if (MokoConstants.ACTION_ORDER_RESULT.equals(action)) {
                OrderTaskResponse response = event.getResponse();
                OrderCHAR orderCHAR = (OrderCHAR) response.orderCHAR;
                int responseType = response.responseType;
                byte[] value = response.responseValue;
                switch (orderCHAR) {
                    case CHAR_PARAMS:
                        if (value.length > 4) {
                            int header = value[0] & 0xFF;// 0xEB
                            int flag = value[1] & 0xFF;// read or write
                            int cmd = value[2] & 0xFF;
                            if (header != 0xEB)
                                return;
                            ParamsKeyEnum configKeyEnum = ParamsKeyEnum.fromParamKey(cmd);
                            if (configKeyEnum == null) {
                                return;
                            }
                            int length = value[3] & 0xFF;
                            if (flag == 0x01 && length == 0x01) {
                                // write
                                int result = value[4] & 0xFF;
                                switch (configKeyEnum) {
                                    case KEY_SINGLE_PRESS_EVENT_CLEAR:
                                        if (result == 0) {
                                            ToastUtils.showToast(AlarmEventActivity.this, "Opps！Save failed. Please check the input characters and try again.");
                                        } else {
                                            ToastUtils.showToast(AlarmEventActivity.this, "Success！");
                                            mBind.tvSinglePressEventCount.setText("0");
                                            if (CRMokoSupport.getInstance().exportSingleEvents != null) {
                                                CRMokoSupport.getInstance().exportSingleEvents.clear();
                                                CRMokoSupport.getInstance().storeSingleEventString = null;
                                            }
                                        }
                                        break;
                                    case KEY_DOUBLE_PRESS_EVENT_CLEAR:
                                        if (result == 0) {
                                            ToastUtils.showToast(AlarmEventActivity.this, "Opps！Save failed. Please check the input characters and try again.");
                                        } else {
                                            ToastUtils.showToast(AlarmEventActivity.this, "Success！");
                                            mBind.tvDoublePressEventCount.setText("0");
                                            if (CRMokoSupport.getInstance().exportDoubleEvents != null) {
                                                CRMokoSupport.getInstance().exportDoubleEvents.clear();
                                                CRMokoSupport.getInstance().storeDoubleEventString = null;
                                            }
                                        }
                                        break;
                                    case KEY_LONG_PRESS_EVENT_CLEAR:
                                        if (result == 0) {
                                            ToastUtils.showToast(AlarmEventActivity.this, "Opps！Save failed. Please check the input characters and try again.");
                                        } else {
                                            ToastUtils.showToast(AlarmEventActivity.this, "Success！");
                                            mBind.tvLongPressEventCount.setText("0");
                                            if (CRMokoSupport.getInstance().exportLongEvents != null) {
                                                CRMokoSupport.getInstance().exportLongEvents.clear();
                                                CRMokoSupport.getInstance().storeLongEventString = null;
                                            }
                                        }
                                        break;
                                    case KEY_SYSTEM_TIME:
                                        if (result == 0) {
                                            ToastUtils.showToast(AlarmEventActivity.this, "Opps！Save failed. Please check the input characters and try again.");
                                        } else {
                                            ToastUtils.showToast(AlarmEventActivity.this, "Success！");
                                        }
                                        break;
                                }
                            }
                            if (flag == 0x00) {
                                // read
                                switch (configKeyEnum) {
                                    case KEY_SYSTEM_TIME:
                                        if (length == 8) {
                                            byte[] timeBytes = Arrays.copyOfRange(value, 4, 4 + length);
                                            ByteBuffer byteBuffer = ByteBuffer.allocate(Long.SIZE / Byte.SIZE).put(timeBytes, 0, timeBytes.length);
                                            byteBuffer.flip();
                                            long time = byteBuffer.getLong();
                                            Calendar calendar = Utils.getCalenderFromTime(time);
                                            mBind.tvUtcTime.setText(Utils.calendar2strDateGMT(calendar, AppConstants.PATTERN_YYYY_MM_DD_T_HH_MM_SS_Z));
                                        }
                                        break;
                                    case KEY_SINGLE_PRESS_EVENTS:
                                        if (length == 2) {
                                            int count = MokoUtils.toInt(Arrays.copyOfRange(value, 4, 4 + length));
                                            mBind.tvSinglePressEventCount.setText(String.valueOf(count));
                                        }
                                        break;
                                    case KEY_DOUBLE_PRESS_EVENTS:
                                        if (length == 2) {
                                            int count = MokoUtils.toInt(Arrays.copyOfRange(value, 4, 4 + length));
                                            mBind.tvDoublePressEventCount.setText(String.valueOf(count));
                                        }
                                        break;
                                    case KEY_LONG_PRESS_EVENTS:
                                        if (length == 2) {
                                            int count = MokoUtils.toInt(Arrays.copyOfRange(value, 4, 4 + length));
                                            mBind.tvLongPressEventCount.setText(String.valueOf(count));
                                        }
                                        break;

                                }
                            }
                        }
                        break;
                }
            }
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    private LoadingMessageDialog mLoadingMessageDialog;

    public void showSyncingProgressDialog() {
        mLoadingMessageDialog = new LoadingMessageDialog();
        mLoadingMessageDialog.setMessage("Syncing..");
        mLoadingMessageDialog.show(getSupportFragmentManager());
    }

    public void dismissSyncProgressDialog() {
        if (mLoadingMessageDialog != null)
            mLoadingMessageDialog.dismissAllowingStateLoss();
    }


    public void onBack(View view) {
        finish();
    }

    public void onSyncUTCTime(View view) {
        if (isWindowLocked())
            return;
        showSyncingProgressDialog();
        ArrayList<OrderTask> orderTasks = new ArrayList<>();
        orderTasks.add(OrderTaskAssembler.setSystemTime());
        orderTasks.add(OrderTaskAssembler.getSystemTime());
        CRMokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
    }

    public void onClearSinglePressEvent(View view) {
        if (isWindowLocked())
            return;
        showSyncingProgressDialog();
        CRMokoSupport.getInstance().sendOrder(OrderTaskAssembler.setSinglePressEventClear());
    }

    public void onExportSinglePressEvent(View view) {
        if (isWindowLocked())
            return;
        Intent intent = new Intent(this, ExportDataActivity.class);
        intent.putExtra(AppConstants.EXTRA_KEY_SLOT_TYPE, 0);
        startActivity(intent);
    }

    public void onClearDoublePressEvent(View view) {
        if (isWindowLocked())
            return;
        showSyncingProgressDialog();
        CRMokoSupport.getInstance().sendOrder(OrderTaskAssembler.setDoublePressEventClear());
    }

    public void onExportDoublePressEvent(View view) {
        if (isWindowLocked())
            return;
        Intent intent = new Intent(this, ExportDataActivity.class);
        intent.putExtra(AppConstants.EXTRA_KEY_SLOT_TYPE, 1);
        startActivity(intent);
    }


    public void onClearLongPressEvent(View view) {
        if (isWindowLocked())
            return;
        showSyncingProgressDialog();
        CRMokoSupport.getInstance().sendOrder(OrderTaskAssembler.setLongPressEventClear());
    }

    public void onExportLongPressEvent(View view) {
        if (isWindowLocked())
            return;
        Intent intent = new Intent(this, ExportDataActivity.class);
        intent.putExtra(AppConstants.EXTRA_KEY_SLOT_TYPE, 2);
        startActivity(intent);
    }
}
