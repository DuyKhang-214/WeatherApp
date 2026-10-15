package com.example.weatherapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.auth.UserProfileChangeRequest;
import java.text.DateFormat;
import java.util.Date;

public class ProfileFragment extends Fragment {
    private TextView tvAvatar, tvName, tvEmail, tvVerified, tvProvider, tvCreated, tvLastLogin;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        return inf.inflate(R.layout.fragment_profile, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        tvAvatar = v.findViewById(R.id.tvAvatar);
        tvName = v.findViewById(R.id.tvName);
        tvEmail = v.findViewById(R.id.tvEmail);
        tvVerified = v.findViewById(R.id.tvVerified);
        tvProvider = v.findViewById(R.id.tvProvider);
        tvCreated = v.findViewById(R.id.tvCreated);
        tvLastLogin = v.findViewById(R.id.tvLastLogin);

        v.findViewById(R.id.btnEditName).setOnClickListener(x -> editName());
        v.findViewById(R.id.btnVerify).setOnClickListener(x -> sendVerification());
        v.findViewById(R.id.btnLogout).setOnClickListener(x -> {
            if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).confirmLogout();
        });
        bind();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) bind();
    }

    private void bind() {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null || tvName == null) return;
        String name = u.getDisplayName() == null || u.getDisplayName().isEmpty() ? "No name set" : u.getDisplayName();
        String email = u.getEmail() == null ? "" : u.getEmail();
        tvName.setText(name);
        tvEmail.setText(email);
        String initial = !email.isEmpty() ? email.substring(0, 1) : "?";
        if (u.getDisplayName() != null && !u.getDisplayName().isEmpty()) initial = u.getDisplayName().substring(0, 1);
        tvAvatar.setText(initial.toUpperCase());

        tvVerified.setText("Email verified: " + (u.isEmailVerified() ? "Yes" : "No"));
        String provider = "Email / Password";
        for (UserInfo info : u.getProviderData()) {
            if (!"firebase".equals(info.getProviderId())) { provider = info.getProviderId(); break; }
        }
        tvProvider.setText("Sign-in method: " + provider);
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
        if (u.getMetadata() != null) {
            tvCreated.setText("Member since: " + df.format(new Date(u.getMetadata().getCreationTimestamp())));
            tvLastLogin.setText("Last sign-in: " + df.format(new Date(u.getMetadata().getLastSignInTimestamp())));
        }
    }

    private void editName() {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) return;
        TextInputEditText et = new TextInputEditText(requireContext());
        et.setText(u.getDisplayName());
        et.setSingleLine(true);
        int pad = Math.round(20 * getResources().getDisplayMetrics().density);
        android.widget.FrameLayout box = new android.widget.FrameLayout(requireContext());
        box.setPadding(pad, pad / 2, pad, 0);
        box.addView(et);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Display name")
                .setView(box)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (d, w) -> {
                    String n = et.getText() == null ? "" : et.getText().toString().trim();
                    if (n.isEmpty()) return;
                    u.updateProfile(new UserProfileChangeRequest.Builder().setDisplayName(n).build())
                            .addOnCompleteListener(t -> {
                                if (!isAdded()) return;
                                toast(t.isSuccessful() ? "Name updated" : "Update failed");
                                bind();
                            });
                })
                .show();
    }

    private void sendVerification() {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) return;
        if (u.isEmailVerified()) { toast("Email already verified"); return; }
        u.sendEmailVerification().addOnCompleteListener(t -> {
            if (isAdded()) toast(t.isSuccessful() ? "Verification email sent" : "Could not send email");
        });
    }

    private void toast(String m) { Toast.makeText(requireContext(), m, Toast.LENGTH_SHORT).show(); }
}
