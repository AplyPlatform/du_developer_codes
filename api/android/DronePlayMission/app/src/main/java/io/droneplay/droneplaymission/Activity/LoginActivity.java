package io.droneplay.droneplaymission.Activity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import org.json.JSONException;
import org.json.JSONObject;

import io.droneplay.droneplaymission.utils.HelperUtils;
import io.droneplay.droneplaymission.R;


public class LoginActivity extends AppCompatActivity {

    private CallbackManager callbackManager;

    private static ProgressDialog spinner = null;

    private int RC_SIGN_IN = 1001; //GOOGLE LOGIN
    private int RC_LOG_IN = 1002; //FACEBOOK LOGIN

    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        UIInit();
    }


    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first

        AccessToken token = AccessToken.getCurrentAccessToken();
        if (token != null) {
            getEmail("facebook", token.getToken());
            return;
        }

    }

    private void showLoader() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                spinner.show();
            }
        });
    }

    private void hideLoader() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                spinner.hide();
            }
        });
    }

    private void UIInit() {
        callbackManager = CallbackManager.Factory.create();

        LoginButton loginButton = findViewById(R.id.login_button);
        loginButton.setReadPermissions("email");
        // Callback registration
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                getEmail("facebook", loginResult.getAccessToken().getToken());
            }

            @Override
            public void onCancel() {
                // App code
            }

            @Override
            public void onError(FacebookException exception) {
                // App code
            }
        });

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        SignInButton siginButton = findViewById(R.id.sign_in_button);
        siginButton.setSize(SignInButton.SIZE_STANDARD);
        // Callback registration
        siginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                LoginActivity.this.startActivityForResult(signInIntent, RC_SIGN_IN);
            }
        });

        spinner = new ProgressDialog(this);
        spinner.setCancelable(false);

    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            getEmail("google", account.getIdToken());
        } catch (ApiException e) {
            showToast("Login failed - " + e.getMessage());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
        else {
            callbackManager.onActivityResult(requestCode, resultCode, data);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void getEmail(String serviceKind, String token) {
        showLoader();
        HelperUtils.getInstance().getDronePlayToken(token, serviceKind, requestLoginHandler);
    }

    @SuppressLint("HandlerLeak")
    private final Handler requestLoginHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {

            hideLoader();

            switch (message.what) {
                case R.id.req_succeeded:

                    String resultContent = (String) message.obj;
                    try {
                        JSONObject json = new JSONObject(resultContent);
                        String result = (String) json.get("result");
                        if ("success".equalsIgnoreCase(result)) {
                            String dronePlayToken = (String) json.get("token");
                            String emailid = (String) json.get("emailid");
                            HelperUtils.getInstance().dronePlayToken = dronePlayToken;
                            HelperUtils.getInstance().clientid = emailid;
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        }
                        else {
                            String fid = (String) json.get("fid");
                            HelperUtils.getInstance().fid = fid;
                            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                            startActivity(intent);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        showToast("Failed - "  + e.getMessage());
                    }

                    return;
                case R.id.req_failed:
                    showToast("Login Failed");
                    break;
            }
        }
    };




    private void showToast(final String toastMsg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_LONG).show();

            }
        });
    }


}
