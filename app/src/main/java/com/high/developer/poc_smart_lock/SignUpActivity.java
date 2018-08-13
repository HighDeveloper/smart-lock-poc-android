package com.high.developer.poc_smart_lock;

import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SignUpActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    @BindView(R.id.editText_username)
    EditText editText_username;
    @BindView(R.id.editText_password)
    EditText editText_password;
    @BindView(R.id.button_sign_up)
    Button button_sign_up;
    @BindView(R.id.textView_sign_in)
    TextView textView_sign_in;

    private String TAG = SignUpActivity.class.getSimpleName();
    private GoogleApiClient googleApiClient;
    private static final int RC_SAVE = 10;
    private static boolean mIsResolving = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        ButterKnife.bind(this);

        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .enableAutoManage(this, 0, this)
                .addApi(Auth.CREDENTIALS_API)
                .build();

        button_sign_up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!"".equals(editText_username.getText().toString().trim()) && !"".equals(editText_password.getText().toString().trim())) {

                    Credential credential = new Credential.Builder(editText_username.getText().toString().trim())
                            .setName(editText_username.getText().toString().trim())
                            .setPassword(editText_password.getText().toString().trim())
                            .build();

                    saveCredentials(credential);

                } else {
                    Toast.makeText(SignUpActivity.this, "Empty username or password!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        textView_sign_in.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SignUpActivity.this, SignInActivity.class);
                startActivity(intent);
            }
        });
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SAVE) {
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "Credential Save: OK");
                startMainActivity();
            } else {
                Log.e(TAG, "Credential Save: Failed");
            }
        }
        mIsResolving = false;
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected");
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "onConnectionSuspended: " + cause);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed: " + connectionResult);
    }


    private void saveCredentials(Credential credential) {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(credential.getName(), credential.getPassword());
        editor.apply();

        Auth.CredentialsApi.save(googleApiClient, credential).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {
                if (status.isSuccess()) {
                    Log.d(TAG, "Credential saved");
                    startMainActivity();
                } else {
                    Log.d(TAG, "Attempt to save credential failed " +
                            status.getStatusMessage() + " " +
                            status.getStatusCode());
                    resolveResult(status, RC_SAVE);
                }
            }
        });
    }

    private void resolveResult(Status status, int requestCode) {
        if (mIsResolving) {
            Log.w(TAG, "resolveResult: already resolving.");
            return;
        }
        if (status.hasResolution()) {
            Log.d(TAG, "STATUS: RESOLVING");
            try {
                status.startResolutionForResult(this, requestCode);
                mIsResolving = true;
            } catch (IntentSender.SendIntentException e) {
                Log.e(TAG, "STATUS: Failed to send resolution.", e);
            }
        } else {
            Log.e(TAG, "STATUS: FAIL");
            if (status.getStatusCode() == Status.RESULT_SUCCESS.getStatusCode()) {
                startMainActivity();
            } else {
                Toast.makeText(this, status.getStatusMessage(), Toast.LENGTH_LONG).show();
            }

        }
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }


}
