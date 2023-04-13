package com.atechclass.attendance.ultis;

import android.content.Context;

import com.atechclass.attendance.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;

public class UserLogin {
    public static GoogleSignInOptions gso;
    public static GoogleSignInClient gsc;
    public static FirebaseAuth mAuth;

    public static GoogleSignInOptions getGSO(String clientID) {
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(clientID)
                .requestEmail()
                .build();
        return gso;
    }

    public static GoogleSignInClient getGsc(Context context, String clientID) {
        gsc = GoogleSignIn.getClient(context, getGSO(clientID));
        return gsc;
    }

    public static FirebaseAuth getAuth() {
        mAuth = FirebaseAuth.getInstance();
        return mAuth;
    }
}
