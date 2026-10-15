package com.example.weatherapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

/** This screen is built entirely in Java code (no XML layout). */
public class RegisterActivity extends AppCompatActivity {
    private TextInputLayout tilName, tilEmail, tilPass, tilConfirm;
    private MaterialButton btn;
    private ProgressBar progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(24), dp(64), dp(24), dp(24));
        scroll.addView(col, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(this);
        title.setText("Create account");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        col.addView(title);

        tilName = addField(col, "Display name", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS, false);
        tilEmail = addField(col, "Email", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, false);
        tilPass = addField(col, "Password (min 6 chars)", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD, true);
        tilConfirm = addField(col, "Confirm password", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD, true);

        btn = new MaterialButton(this);
        btn.setText("Register");
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(56));
        blp.topMargin = dp(24);
        col.addView(btn, blp);
        btn.setOnClickListener(v -> register());

        progress = new ProgressBar(this);
        progress.setVisibility(View.GONE);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        plp.gravity = Gravity.CENTER_HORIZONTAL;
        plp.topMargin = dp(12);
        col.addView(progress, plp);

        TextView back = new TextView(this);
        back.setText("Already have an account? Log in");
        back.setTextColor(getColor(R.color.primary));
        back.setPadding(dp(16), dp(16), dp(16), dp(16));
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        col.addView(back);

        setContentView(scroll);
    }

    private TextInputLayout addField(LinearLayout parent, String hint, int inputType, boolean password) {
        TextInputLayout til = new TextInputLayout(this);
        til.setHint(hint);
        if (password) til.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
        TextInputEditText et = new TextInputEditText(til.getContext());
        et.setInputType(inputType);
        et.setSingleLine(true);
        til.addView(et, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(12);
        parent.addView(til, lp);
        return til;
    }

    private String text(TextInputLayout til) {
        return til.getEditText() == null ? "" : til.getEditText().getText().toString().trim();
    }

    private void register() {
        String name = text(tilName), email = text(tilEmail), pass = text(tilPass), confirm = text(tilConfirm);
        tilName.setError(null); tilEmail.setError(null); tilPass.setError(null); tilConfirm.setError(null);
        if (name.isEmpty()) { tilName.setError("Required"); return; }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { tilEmail.setError("Enter a valid email"); return; }
        if (pass.length() < 6) { tilPass.setError("At least 6 characters"); return; }
        if (!pass.equals(confirm)) { tilConfirm.setError("Passwords do not match"); return; }

        setBusy(true);
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().getUser() != null) {
                FirebaseUser u = task.getResult().getUser();
                u.updateProfile(new UserProfileChangeRequest.Builder().setDisplayName(name).build())
                        .addOnCompleteListener(t2 -> {
                            u.sendEmailVerification();
                            Intent i = new Intent(this, MainActivity.class);
                            i.putExtra("welcome", true);
                            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(i);
                        });
            } else {
                setBusy(false);
                String msg = task.getException() != null ? task.getException().getMessage() : "Registration failed";
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setBusy(boolean busy) {
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        btn.setEnabled(!busy);
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
