/*
 * Copyright (c) 2016 Samsung Electronics Co., Ltd. All rights reserved.
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that
 * the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright notice,
 *       this list of conditions and the following disclaimer in the documentation and/or
 *       other materials provided with the distribution.
 *     * Neither the name of Samsung Electronics Co., Ltd. nor the names of its contributors may be used to endorse or
 *       promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package io.droneplay.droneplaymission.Activity;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import io.droneplay.droneplaymission.R;
import io.droneplay.droneplaymission.SmartWatchManagerService;

public class SmartWatchManagerActivity extends AppCompatActivity {
    private static TextView mTextView;
    private boolean mIsBound = false;
    private static boolean sendButtonClicked;
    private SmartWatchManagerService mConsumerService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smart_watch_man);
        mTextView = findViewById(R.id.tvStatus);
        sendButtonClicked = false;
        // Bind service
        //mIsBound = bindService(new Intent(SmartWatchManagerActivity.this, SmartWatchManagerService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

//    @Override
//    protected void onDestroy() {
//        // Clean up connections
//        if (mIsBound == true && mConsumerService != null) {
//            mConsumerService.clearToast();
//        }
//        // Un-bind service
//        if (mIsBound) {
//            unbindService(mConnection);
//            mIsBound = false;
//        }
//        sendButtonClicked = false;
//        super.onDestroy();
//    }
//
//    public void mOnClick(View v) {
//        switch (v.getId()) {
//            case R.id.buttonFindPeerAgent: {
//                if (mIsBound == true && mConsumerService != null) {
//                    mConsumerService.findPeers();
//                    sendButtonClicked = false;
//                }
//                break;
//            }
//            case R.id.buttonSend: {
//                if (mIsBound == true && sendButtonClicked == false && mConsumerService != null) {
//                    if (mConsumerService.sendData("Hello Message!") != -1) {
//                        sendButtonClicked = true;
//                    }else {
//                        sendButtonClicked = false;
//                    }
//                }
//                break;
//            }
//            default:
//        }
//    }
//
//    private final ServiceConnection mConnection = new ServiceConnection() {
//        @Override
//        public void onServiceConnected(ComponentName className, IBinder service) {
//            mConsumerService = ((SmartWatchManagerService.LocalBinder) service).getService();
//            updateTextView("onServiceConnected");
//        }
//
//        @Override
//        public void onServiceDisconnected(ComponentName className) {
//            mConsumerService = null;
//            mIsBound = false;
//            updateTextView("onServiceDisconnected");
//        }
//    };
//
//    public static void addMessage(String data) {
//        //mMessageAdapter.addMessage(new Message(data));
//        updateTextView("recevied:" + data);
//    }
//
//    public static void updateTextView(final String str) {
//        mTextView.setText(str);
//    }
//
//    public static void updateButtonState(boolean enable) {
//        sendButtonClicked = enable;
//    }
}
