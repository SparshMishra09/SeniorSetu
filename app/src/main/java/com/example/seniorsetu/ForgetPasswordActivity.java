package com.example.seniorsetu;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class ForgetPasswordActivity extends AppCompatActivity {

    private static final String TAG = "ForgetPasswordActivity";

    // UI Components
    private TextInputEditText editTextEmailReset;
    private Button buttonResetPassword;
    private ImageButton tvBackToLogin;

    // Firebase
    private FirebaseAuth mAuth;

    // Loading state
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forget_password);

        // Initialize Firebase
        initializeFirebase();

        // Initialize UI components
        initializeViews();

        // Setup pre-filled email if passed from LoginActivity
        setupPrefilledEmail();

        // Set click listeners
        setClickListeners();

        // Handle back button press
        setupBackButton();

        Log.d(TAG, "ForgetPasswordActivity created successfully");
    }

    private void initializeFirebase() {
        try {
            mAuth = FirebaseAuth.getInstance();
            Log.d(TAG, "Firebase initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase: " + e.getMessage());
            Toast.makeText(this, "Error initializing app. Please try again.", Toast.LENGTH_LONG).show();
        }
    }

    private void initializeViews() {
        try {
            editTextEmailReset = findViewById(R.id.editTextEmailReset);
            buttonResetPassword = findViewById(R.id.buttonResetPassword);
            tvBackToLogin = findViewById(R.id.tvBackToLogin);

            Log.d(TAG, "Views initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage());
            Toast.makeText(this, "Error loading interface. Please restart the app.", Toast.LENGTH_LONG).show();
        }
    }

    private void setupPrefilledEmail() {
        try {
            String prefilledEmail = getIntent().getStringExtra("email");
            if (!TextUtils.isEmpty(prefilledEmail) && editTextEmailReset != null) {
                editTextEmailReset.setText(prefilledEmail);
                Log.d(TAG, "Pre-filled email: " + prefilledEmail);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up pre-filled email: " + e.getMessage());
        }
    }

    private void setClickListeners() {
        // Back button click listener
        if (tvBackToLogin != null) {
            tvBackToLogin.setOnClickListener(v -> navigateBackToLogin());
        }

        // Reset password button click listener
        if (buttonResetPassword != null) {
            buttonResetPassword.setOnClickListener(v -> sendPasswordResetEmail());
        }
    }

    private void setupBackButton() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                navigateBackToLogin();
            }
        });
    }

    private void sendPasswordResetEmail() {
        // Get email input
        String email = getTextFromEditText(editTextEmailReset);

        // Validate email
        if (!validateEmail(email)) {
            return;
        }

        // Prevent multiple requests
        if (isLoading) {
            Toast.makeText(this, "Please wait...", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading state
        setLoadingState(true);

        // Send password reset email
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    setLoadingState(false);

                    if (task.isSuccessful()) {
                        handleResetEmailSent(email);
                    } else {
                        handleResetEmailError(task.getException());
                    }
                })
                .addOnFailureListener(e -> {
                    setLoadingState(false);
                    Log.e(TAG, "Failed to send password reset email: " + e.getMessage());
                    handleResetEmailError(e);
                });
    }

    private String getTextFromEditText(TextInputEditText editText) {
        return editText != null && editText.getText() != null ?
                editText.getText().toString().trim() : "";
    }

    private boolean validateEmail(String email) {
        // Check if email is empty
        if (TextUtils.isEmpty(email)) {
            showFieldError(editTextEmailReset, "Email is required");
            return false;
        }

        // Validate email format
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showFieldError(editTextEmailReset, "Please enter a valid email address");
            return false;
        }

        return true;
    }

    private void showFieldError(TextInputEditText field, String message) {
        if (field != null) {
            field.setError(message);
            field.requestFocus();
        }
    }

    private void handleResetEmailSent(String email) {
        Log.d(TAG, "Password reset email sent successfully to: " + email);

        // Show success message
        Toast.makeText(this,
                "Password reset link sent to " + email + "\nPlease check your email inbox.",
                Toast.LENGTH_LONG).show();

        // Show success dialog
        showSuccessDialog(email);
    }

    private void handleResetEmailError(Exception exception) {
        String errorMessage = exception != null ? exception.getMessage() : "Unknown error occurred";
        Log.e(TAG, "Password reset email failed: " + errorMessage);

        // Provide user-friendly error messages
        String userMessage = getUserFriendlyErrorMessage(errorMessage);
        Toast.makeText(this, userMessage, Toast.LENGTH_LONG).show();
    }

    private String getUserFriendlyErrorMessage(String errorMessage) {
        if (errorMessage.contains("user-not-found")) {
            return "No account found with this email address";
        } else if (errorMessage.contains("invalid-email")) {
            return "Invalid email address";
        } else if (errorMessage.contains("too-many-requests")) {
            return "Too many reset attempts. Please try again later";
        } else if (errorMessage.contains("network-request-failed")) {
            return "Network error. Please check your connection";
        } else if (errorMessage.contains("user-disabled")) {
            return "This account has been disabled";
        } else {
            return "Failed to send reset email. Please try again";
        }
    }

    private void showSuccessDialog(String email) {
        // Create a simple success dialog
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Email Sent!")
                .setMessage("A password reset link has been sent to " + email +
                        "\n\nPlease check your email and follow the instructions to reset your password.")
                .setPositiveButton("Back to Login", (dialog, which) -> {
                    dialog.dismiss();
                    navigateBackToLogin();
                })
                .setNegativeButton("Stay Here", (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();
    }

    private void setLoadingState(boolean loading) {
        isLoading = loading;

        if (buttonResetPassword != null) {
            buttonResetPassword.setEnabled(!loading);
            buttonResetPassword.setText(loading ? "Sending..." : "Send Reset Link");
        }

        // Disable other interactive elements during loading
        if (editTextEmailReset != null) editTextEmailReset.setEnabled(!loading);
        if (tvBackToLogin != null) tvBackToLogin.setEnabled(!loading);
    }

    private void navigateBackToLogin() {
        try {
            Intent intent = new Intent(ForgetPasswordActivity.this, LoginActivity.class);
            // Clear the activity stack and start fresh
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            Log.d(TAG, "Navigating back to LoginActivity");
        } catch (Exception e) {
            Log.e(TAG, "Error navigating back to login: " + e.getMessage());
            Toast.makeText(this, "Error navigating. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "ForgetPasswordActivity destroyed");
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Reset loading state when activity is paused
        setLoadingState(false);
    }
}