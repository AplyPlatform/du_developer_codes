package io.droneplay.droneplaymission.Activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.safetynet.SafetyNet;
import com.google.android.gms.safetynet.SafetyNetApi;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONException;
import org.json.JSONObject;

import io.droneplay.droneplaymission.utils.HelperUtils;
import io.droneplay.droneplaymission.R;
import io.droneplay.droneplaymission.utils.ToastUtils;


public class RegisterActivity extends AppCompatActivity {


    private static ProgressDialog spinner = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        UIInit();
    }


    private void UIInit() {

        final EditText phonenumber = findViewById(R.id.phoneNumber);
        final EditText name = findViewById(R.id.nameField);
        final EditText socialid = findViewById(R.id.emailAddress);

        Button registerButton = findViewById(R.id.agreeButton);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (HelperUtils.getInstance().isValidEmail(socialid.getText().toString()) == false) {
                    ToastUtils.setResultToToast("잘못된 이메일 주소입니다.");
                    return;
                }

                if (HelperUtils.getInstance().validCellPhone(phonenumber.getText().toString())== false) {
                    ToastUtils.setResultToToast("잘못된 전화번호 입니다.");
                    return;
                }

                if ("".equalsIgnoreCase(name.getText().toString()) == true) {
                    ToastUtils.setResultToToast("이름을 입력해 주세요.");
                    return;
                }

                processRegister(name.getText().toString(), phonenumber.getText().toString(), socialid.getText().toString());
            }
        });

        spinner = new ProgressDialog(this);
        spinner.setCancelable(false);
    }

    private void processRegister(String name, String phonenumber, String email) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                spinner.show();
            }
        });

        String captChapAPIKey = HelperUtils.getInstance().getMetadata(RegisterActivity.this, "google.recaptcha.SITEKEY");
        SafetyNet.getClient(RegisterActivity.this).verifyWithRecaptcha(captChapAPIKey)
                .addOnSuccessListener((Activity) RegisterActivity.this,
                        new OnSuccessListener<SafetyNetApi.RecaptchaTokenResponse>() {
                            @Override
                            public void onSuccess(SafetyNetApi.RecaptchaTokenResponse response) {
                                // Indicates communication with reCAPTCHA service was
                                // successful.
                                String userResponseToken = response.getTokenResult();
                                if (!userResponseToken.isEmpty()) {
                                    HelperUtils.getInstance().processDronePlayRegister(name, email, phonenumber, userResponseToken, registerHandler);
                                }
                                else {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            spinner.hide();
                                        }
                                    });
                                    ToastUtils.setResultToToast("죄송합니다, 잠시후에 다시 시도해 주세요.");
                                }
                            }
                        })
                .addOnFailureListener((Activity) RegisterActivity.this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                spinner.hide();
                            }
                        });

                        if (e instanceof ApiException) {
                            // An error occurred when communicating with the
                            // reCAPTCHA service. Refer to the status code to
                            // handle the error appropriately.
                            ApiException apiException = (ApiException) e;
                            int statusCode = apiException.getStatusCode();
                            Log.d("DronePlayRegister", "Error: " + CommonStatusCodes
                                    .getStatusCodeString(statusCode));
                        } else {
                            // A different, unknown type of error occurred.
                            Log.d("DronePlayRegister", "Error: " + e.getMessage());
                        }
                    }
                });

    }

    @SuppressLint("HandlerLeak")
    private final Handler registerHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    spinner.hide();
                }
            });

            switch (message.what) {
                case R.id.req_succeeded:

                    String resultContent = (String) message.obj;
                    try {
                        JSONObject json = new JSONObject(resultContent);
                        String result = (String) json.get("result");
                        if ("success".equalsIgnoreCase(result)) {
                            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                            startActivity(intent);
                            finish();
                        }
                        else {
                            String reason = (String) json.get("reason");
                            ToastUtils.setResultToToast(reason);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    try {
                        if(resultContent != null) {

                        }
                        else {

                        }
                    } catch (Exception e) {

                    }

                    return;
                case R.id.req_failed:

                    break;
            }
        }
    };

}
