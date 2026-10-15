package com.example.weatherapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private Button btnLogin;
    private ProgressBar progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) { goMain(); return; }

        setContentView(R.layout.activity_login);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progress = findViewById(R.id.progress);

        btnLogin.setOnClickListener(v -> login());
        // Explicit Intent -> RegisterActivity
        findViewById(R.id.tvRegister).setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
        findViewById(R.id.tvForgot).setOnClickListener(v -> resetPassword());
    }

    private void login() {
        String email = etEmail.getText() == null ? "" : etEmail.getText().toString().trim();
        String pass = etPassword.getText() == null ? "" : etPassword.getText().toString();
        tilEmail.setError(null);
        tilPassword.setError(null);
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { tilEmail.setError("Enter a valid email"); return; }
        if (pass.length() < 6) { tilPassword.setError("At least 6 characters"); return; }

        setBusy(true);
        auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
            setBusy(false);
            if (task.isSuccessful()) {
                goMain();
            } else {
                String msg = task.getException() != null ? task.getException().getMessage() : "Login failed";
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void resetPassword() {
        String email = etEmail.getText() == null ? "" : etEmail.getText().toString().trim();
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Enter your email first");
            return;
        }
        auth.sendPasswordResetEmail(email).addOnCompleteListener(t ->
                Toast.makeText(this, t.isSuccessful() ? "Reset email sent" : "Could not send reset email",
                        Toast.LENGTH_LONG).show());
    }

    private void setBusy(boolean busy) {
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!busy);
    }

    private void goMain() {
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra("welcome", true);
        startActivity(i);
        finish();
    }
}
