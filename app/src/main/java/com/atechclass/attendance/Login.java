package com.atechclass.attendance;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.atechclass.attendance.ultis.UserLogin;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

public class Login extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;
    LinearLayout btnLoginGG;
    GoogleSignInClient gsc;
    Language language;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        language = new Language(this);
        language.Language();
        setContentView(R.layout.activity_login);

        mAuth = UserLogin.getAuth();

        setUpLoginGoogle();
    }

    private void setUpLoginGoogle() {
        gsc = UserLogin.getGsc(this, getResources().getString(R.string.default_web_client_id));

        btnLoginGG = findViewById(R.id.img_splash_logo);

        btnLoginGG.setOnClickListener(view -> SignIn());
    }

    private void SignIn() {
        Intent intent = gsc.getSignInIntent();
        startActivityForResult(intent, RC_SIGN_IN);
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account, ProgressDialog dialog) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        dialog.dismiss();
                        startActivity(new Intent(Login.this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(Login.this, getString(R.string.pls_login_again), Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        btnLoginGG.setEnabled(true);
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                btnLoginGG.setEnabled(false);
                firebaseAuthWithGoogle(account, loadDialog());
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private ProgressDialog loadDialog() {
        ProgressDialog dialog = new ProgressDialog(Login.this);
        dialog.setMessage(getString(R.string.check_login));
        dialog.setIndeterminate(true);
        dialog.show();
        return dialog;
    }
}