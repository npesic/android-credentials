package com.google.example.credentialsbasic;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialPickerConfig;
import com.google.android.gms.auth.api.credentials.CredentialsApi;
import com.google.android.gms.auth.api.credentials.HintRequest;
import com.google.android.gms.auth.api.credentials.IdToken;
import com.google.android.gms.auth.api.credentials.IdentityProviders;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResolvingResultCallbacks;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import java.util.logging.Level;

/**
 * Created by npesic on 2/29/16.
 */
public class UafActivity extends AppCompatActivity {

    private String WEB_SERVER_CLIENT_ID = "961906352730-f0pis2s0b127ftca4l42g84o1o17lt7b.apps.googleusercontent.com";

    private GoogleApiClient mCredentialsClient;
    private FragmentActivity mAttachedActivity;
    private CredentialsApi mCredentialsApi;
    private static String TAG = "UafActivity";

    private GoogleApiClient.OnConnectionFailedListener connectionFailedListener;
    private GoogleApiClient.ConnectionCallbacks connectionCallbacks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        connectionFailedListener = new GoogleApiClient.OnConnectionFailedListener() {
            @Override
            public void onConnectionFailed(ConnectionResult connectionResult) {
                Log.d(TAG, "onConnectionFailed " + connectionResult);
            }
        };
        connectionCallbacks = new GoogleApiClient.ConnectionCallbacks() {
            @Override
            public void onConnected(Bundle bundle) {
                Log.d(TAG, "onConnected");
            }

            @Override
            public void onConnectionSuspended(int i) {
                Log.d(TAG, "onConnectionSuspended:" + i);
                mCredentialsClient.reconnect();
            }
        };

        initClient(Preferences.getSettingsParam("email"));

//        mCredentialsClient = new GoogleApiClient.Builder(this)
//                .addConnectionCallbacks(connectionCallbacks)
//                .addOnConnectionFailedListener(connectionFailedListener)
//                .addApi(Auth.CREDENTIALS_API)
//                .build();
    }

    private void initClient (String emailAddress){
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder()
                .setAccountName(emailAddress)
                .requestIdToken(WEB_SERVER_CLIENT_ID)
                .build();

        mCredentialsClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(connectionCallbacks)
                .enableAutoManage(this, connectionFailedListener)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        //getIdToken();
        OptionalPendingResult<GoogleSignInResult> pendingResult = Auth.GoogleSignInApi.silentSignIn(mCredentialsClient);
        pendingResult.setResultCallback(new ResultCallback<GoogleSignInResult>() {
            public void onResult(@NonNull GoogleSignInResult result) {
                if (result.isSuccess()) {
                    Log.i(TAG, "ID token: " + result.getSignInAccount().getIdToken());
                    finishWithResult(result.getSignInAccount().getIdToken(), Preferences.getSettingsParam("email"));
                } else {
                    finishWithError(result.getStatus().toString());
                }
            }
        });
    }

    private void getIdToken(){
        HintRequest hintRequest = new HintRequest.Builder()
                .setHintPickerConfig(new CredentialPickerConfig.Builder()
                        .setShowCancelButton(true)
                        .build())
                .setEmailAddressIdentifierSupported(true)
                .setAccountTypes(IdentityProviders.GOOGLE)
                .build();

        PendingIntent intent =
                Auth.CredentialsApi.getHintPickerIntent(mCredentialsClient, hintRequest);
        try {
            startIntentSenderForResult(intent.getIntentSender(), 1, null, 0, 0, 0);
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "Could not start hint picker Intent", e);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult:" + requestCode + ":" + resultCode + ":" + data);

        switch (requestCode) {
            case 1:
                if (resultCode == RESULT_OK) {
                    Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);
                    if (!credential.getIdTokens().isEmpty()) {
                        IdToken idToken = credential.getIdTokens().get(0);
                        finishWithResult(idToken.getIdToken(), credential.getId());
                    } else {
                        finishWithError("ID Token was empty");
                    }
                } else {
                    finishWithError("Result code:"+resultCode);
                }
                break;
            default:
                finishWithError("Request code:"+requestCode);
        }
    }

    private void finishWithResult(String idToken, String id){
        Bundle data = new Bundle();

        data.putString("idToken", idToken);
        data.putString("id", id);
        data.putString("err", "");
        Intent intent = new Intent();
        intent.putExtras(data);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void finishWithError(String err){
        Bundle data = new Bundle();

        data.putString("idToken", "");
        data.putString("id", "");
        data.putString("err", err);
        Intent intent = new Intent();
        intent.putExtras(data);
        setResult(RESULT_OK, intent);
        finish();
    }
}
