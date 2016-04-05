package com.google.example.credentialsbasic;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import com.google.android.gms.auth.api.credentials.IdentityProviders;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;

/**
 * <b>Mock</b> server class to demonstrate how to use the Google APIs Client Library for Java
 * to verify an ID token obtained from a Credential.
 */
public class MockServer extends IntentService {

    public static final String TAG = "MockServer";
    public static final String EXTRA_IDTOKEN = "id_token";
    public static final int STATUS_FINISHED = 0;
    public static final int STATUS_ERROR = 1;

    private static String WEB_SERVER_CLIENT_ID = "961906352730-f0pis2s0b127ftca4l42g84o1o17lt7b.apps.googleusercontent.com";

    private static final String PACKAGE_NAME = "com.google.example.credentialsbasic";
    private static final String SHA512_HASH = "njgkSXbLpqCSqwnyGYqqMjZWPALc7FcFFq_Crq2vdzUbznXEGQIMcyt4_RLBB-K4qg0tKN0h6dwQq17gaBzyKA==";

    private static final HttpTransport transport = new NetHttpTransport();
    private static final JsonFactory jsonFactory = new JacksonFactory();

    // Verifier that checks that the token has the proper issuer and audience
    private static GoogleIdTokenVerifier verifier =
            new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                    .setIssuer(IdentityProviders.GOOGLE)
                    .setAudience(Arrays.asList(getAudienceString(SHA512_HASH, PACKAGE_NAME), WEB_SERVER_CLIENT_ID))
            .build();

    public MockServer() {
        super(TAG);
    }

    /**
     * Verify an ID token and log the email address and verification status.
     * @param idTokenString ID Token from a Credential.
     */
    private static String verifyIdToken(String idTokenString) {
        // Print the audience to the logs
        logTokenAudience(idTokenString);

        try {
            // Verify ID Token
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                Log.w(TAG, "ID Token Verification Failed, check the README for instructions.");
                return "{\"err\":\"Verification failed\"}";
            }

            // Extract email address and verification
            GoogleIdToken.Payload payload = idToken.getPayload();
            Log.d(TAG, "IdToken:" + payload.toPrettyString());
            Log.d(TAG, "IdToken:Email:" + payload.getEmail());
            Log.d(TAG, "IdToken:EmailVerified:" + payload.getEmailVerified());
            Log.d(TAG, "IdToken:JSON:" + payload.toPrettyString());
            return payload.toPrettyString();
        } catch (GeneralSecurityException e) {
            Log.e(TAG, "verifyIdToken:GeneralSecurityException", e);
            return "{\"err\":\"GeneralSecurityException\"}";
        } catch (IOException e) {
            Log.e(TAG, "verifyIdToken:IOException", e);
            return "{\"err\":\"IOException\"}";
        }
    }

    /**
     * Print the audience of an unverified token string to the logs.
     * @param idTokenString the ID Token string.
     */
    public static void logTokenAudience(String idTokenString) {
        try {
            GoogleIdToken idToken = GoogleIdToken.parse(jsonFactory, idTokenString);
            Log.d(TAG, "IDToken pretty:" + idToken.getPayload().toPrettyString());
            Log.d(TAG, "IDToken Audience:" + idToken.getPayload().getAudience());
        } catch (IOException e) {
            Log.e(TAG, "IDToken Audience: Could not parse ID Token", e);
        }
    }

    /**
     * Determine the audience of the ID token based on the sha512 hash and the package name.
     * @param sha512Hash sha512 hash of the application, see README for instructions.
     * @param packageName package name of the application,
     */
    private static String getAudienceString(String sha512Hash, String packageName) {
        String fmtString = "android://%s@%s";
        return String.format(fmtString, sha512Hash, packageName);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String res = null;
        String idToken = intent.getStringExtra(EXTRA_IDTOKEN);
        Bundle bundle = new Bundle();
        final ResultReceiver receiver = intent.getParcelableExtra("receiver");
        if (idToken != null) {
            Log.d(TAG, "Processing ID Token:" + idToken);
            res = verifyIdToken(idToken);

        } else {
            res = "{\"err\":\"SystemError\"}";
        }

        if (receiver != null) {
            bundle.putString("result", res);
            receiver.send(STATUS_FINISHED, bundle);
        }
    }
}
