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
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialRequest;
import com.google.android.gms.auth.api.credentials.CredentialRequestResult;
import com.google.android.gms.auth.api.credentials.IdentityProviders;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SignInActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    @BindView(R.id.editText_username)
    EditText editText_username;
    @BindView(R.id.editText_password)
    EditText editText_password;
    @BindView(R.id.button_sign_in)
    Button button_sign_in;

    private String TAG = SignInActivity.class.getSimpleName();
    private GoogleApiClient googleApiClient;
    private static final int RC_READ = 11;
    private static boolean mIsResolving = false;
    private static final int RC_SAVE = 10;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        ButterKnife.bind(this);

        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .enableAutoManage(this, 0, this)
                .addApi(Auth.CREDENTIALS_API)
                .build();

        button_sign_in.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (validateUsername(editText_username.getText().toString().trim())) {
                    Credential credential = new Credential.Builder(editText_username.getText().toString().trim())
                            .setName(editText_username.getText().toString().trim())
                            .setPassword(editText_password.getText().toString().trim())
                            .build();

                    saveCredentials(credential);
                } else {
                    Toast.makeText(SignInActivity.this, "Credentials are invalid. Username or password are " + "incorrect.", Toast.LENGTH_SHORT).show();
                }

            }
        });

    }

    private boolean validateUsername(String username) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(SignInActivity.this);
        return (!"".equals(sharedPref.getString(username, ""))) ? true : false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_READ) {
            if (resultCode == RESULT_OK) {
                Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);
                onCredentialSuccess(credential);
            } else {
                Log.d(TAG, "Request failed");
            }
        } else if (requestCode == RC_SAVE) {
            Log.d(TAG, "Result code: " + resultCode);
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "Credential Save: OK");
            } else {
                Log.e(TAG, "Credential Save Failed");
            }
            startMainActivity();
        }
        mIsResolving = false;
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected");

        CredentialRequest credentialRequest = new CredentialRequest.Builder()
                .setPasswordLoginSupported(true)
                .setAccountTypes(IdentityProviders.GOOGLE, IdentityProviders.FACEBOOK, IdentityProviders.PAYPAL)
                .build();

        Auth.CredentialsApi.request(googleApiClient, credentialRequest).setResultCallback(new ResultCallback<CredentialRequestResult>() {
            @Override
            public void onResult(@NonNull CredentialRequestResult credentialRequestResult) {
                Status status = credentialRequestResult.getStatus();
                if (status.isSuccess()) {
                    onCredentialSuccess(credentialRequestResult.getCredential());
                } else if (status.getStatusCode() == CommonStatusCodes.RESOLUTION_REQUIRED) {
                    resolveResult(status, RC_READ);
                } else if (status.getStatusCode() == CommonStatusCodes.SIGN_IN_REQUIRED) {
                    Log.d(TAG, "Sign in required");
                } else {
                    Log.w(TAG, "Unrecognized status code: " + status.getStatusCode());
                }

            }
        });
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "onConnectionSuspended: " + cause);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed: " + connectionResult);
    }

    private void onCredentialSuccess(Credential credential) {

        if (credential.getAccountType() == null) {
            if (validateUsername(credential.getName())) {
                startMainActivity();
            } else {
                Toast.makeText(SignInActivity.this, "Credentials are invalid. Username or password are " + "incorrect.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveCredentials(Credential credential) {
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

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void resolveResult(Status status, int requestCode) {
        if (mIsResolving) {
            Log.w(TAG, "resolveResult: already resolving.");
            return;
        }

        Log.d(TAG, "Resolving: " + status);
        if (status.hasResolution()) {
            Log.d(TAG, "STATUS: RESOLVING");
            try {
                status.startResolutionForResult(this, requestCode);
                mIsResolving = true;
            } catch (IntentSender.SendIntentException e) {
                Log.e(TAG, "STATUS: Failed to send resolution.", e);
            }
        } else {
            if (status.getStatusCode() == Status.RESULT_SUCCESS.getStatusCode()) {
                startMainActivity();
            } else {
                Log.e(TAG, "STATUS: FAIL");
                Toast.makeText(this, status.getStatusMessage(), Toast.LENGTH_LONG).show();
            }

        }
    }

}
