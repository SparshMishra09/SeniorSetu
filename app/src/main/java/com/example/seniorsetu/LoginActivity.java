package com.example.seniorsetu;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    // UI Components
    private TextInputEditText editTextEmail, editTextPassword;
    private Button buttonLogin, btnGuestLogin;
    private TextView tvCreateAccount, tvForgotPassword;
    private LinearLayout btnPhoneLogin;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    // Flag to prevent auto-navigation on initial load
    private boolean hasUserInteracted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase
        initializeFirebase();

        // Initialize UI components
        initializeViews();

        // Set click listeners
        setClickListeners();

        // Handle back button press
        setupBackButton();

        // Setup auth state listener
        setupAuthStateListener();

        // Debug current auth state
        debugAuthState();
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
            editTextEmail = findViewById(R.id.editTextEmail);
            editTextPassword = findViewById(R.id.editTextPassword);
            buttonLogin = findViewById(R.id.buttonLogin);
            btnGuestLogin = findViewById(R.id.btnGuestLogin);
            tvCreateAccount = findViewById(R.id.tvCreateAccount);
            tvForgotPassword = findViewById(R.id.tvForgotPassword);
            btnPhoneLogin = findViewById(R.id.btnPhoneLogin);
            Log.d(TAG, "Views initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage());
            Toast.makeText(this, "Error loading interface. Please restart the app.", Toast.LENGTH_LONG).show();
        }
    }

    private void setClickListeners() {
        // Sign In button click listener
        if (buttonLogin != null) {
            buttonLogin.setOnClickListener(v -> {
                hasUserInteracted = true;
                signInUser();
            });
        }

        // Create Account link click listener
        if (tvCreateAccount != null) {
            tvCreateAccount.setOnClickListener(v -> {
                hasUserInteracted = true;
                navigateToSignup();
            });
        }

        // Forgot Password click listener
        if (tvForgotPassword != null) {
            tvForgotPassword.setOnClickListener(v -> {
                hasUserInteracted = true;
                handleForgotPassword();
            });
        }

        // Guest Login button click listener
        if (btnGuestLogin != null) {
            btnGuestLogin.setOnClickListener(v -> {
                hasUserInteracted = true;
                handleGuestLogin();
            });
        }

        // Phone Login button click listener
        if (btnPhoneLogin != null) {
            btnPhoneLogin.setOnClickListener(v -> {
                hasUserInteracted = true;
                handlePhoneLogin();
            });
        }
    }

    private void setupBackButton() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Exit the app when back is pressed on login screen
                finishAffinity();
            }
        });
    }

    private void setupAuthStateListener() {
        mAuthListener = firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null && hasUserInteracted && isUserProperlyAuthenticated(user)) {
                // User is properly signed in and has interacted with the app
                Log.d(TAG, "User is properly authenticated: " + user.getUid());
                // Don't auto-navigate, let the login method handle navigation
            } else if (user != null) {
                // User exists but either hasn't interacted or isn't properly authenticated
                Log.d(TAG, "User found but not properly authenticated or no interaction yet");
            } else {
                // User is signed out
                Log.d(TAG, "User is signed out");
            }
        };
    }

    private boolean isUserProperlyAuthenticated(FirebaseUser user) {
        // Add additional checks to ensure user is properly authenticated
        return user != null &&
                !TextUtils.isEmpty(user.getUid()) &&
                user.getEmail() != null &&
                (user.isEmailVerified() || true); // Set to true if you're skipping email verification
    }

    private void debugAuthState() {
        try {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                Log.d(TAG, "=== Current User Details ===");
                Log.d(TAG, "UID: " + currentUser.getUid());
                Log.d(TAG, "Email: " + currentUser.getEmail());
                Log.d(TAG, "Display Name: " + currentUser.getDisplayName());
                Log.d(TAG, "Email Verified: " + currentUser.isEmailVerified());
                Log.d(TAG, "Anonymous: " + currentUser.isAnonymous());
                Log.d(TAG, "===========================");

                // Uncomment this line if you want to clear the cached user for testing
                // mAuth.signOut();
                // Log.d(TAG, "User signed out for testing");
            } else {
                Log.d(TAG, "No current user found");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error debugging auth state: " + e.getMessage());
        }
    }

    private void signInUser() {
        // Get input values
        String email = getTextFromEditText(editTextEmail);
        String password = getTextFromEditText(editTextPassword);

        // Validate inputs
        if (!validateInputs(email, password)) {
            return;
        }

        // Show loading state
        setLoadingState(true);

        // Sign in with email and password
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    setLoadingState(false);

                    if (task.isSuccessful()) {
                        Log.d(TAG, "User signed in successfully");
                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user != null) {
                            // Check if email is verified (optional)
                            if (user.isEmailVerified() || true) { // Set to true to skip email verification
                                Toast.makeText(LoginActivity.this, "Welcome back!", Toast.LENGTH_SHORT).show();
                                navigateToMain();
                            } else {
                                Toast.makeText(LoginActivity.this,
                                        "Please verify your email address first.", Toast.LENGTH_LONG).show();
                                mAuth.signOut(); // Sign out unverified user
                            }
                        } else {
                            handleAuthError("Sign in failed - user is null");
                        }
                    } else {
                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() : "Unknown error occurred";
                        handleAuthError("Sign in failed: " + errorMessage);
                    }
                })
                .addOnFailureListener(e -> {
                    setLoadingState(false);
                    Log.e(TAG, "Sign in failed: " + e.getMessage());
                    handleAuthError("Failed to sign in: " + e.getMessage());
                });
    }

    private String getTextFromEditText(TextInputEditText editText) {
        return editText != null && editText.getText() != null ?
                editText.getText().toString().trim() : "";
    }

    private boolean validateInputs(String email, String password) {
        // Check if email is empty
        if (TextUtils.isEmpty(email)) {
            showFieldError(editTextEmail, "Email is required");
            return false;
        }

        // Check if password is empty
        if (TextUtils.isEmpty(password)) {
            showFieldError(editTextPassword, "Password is required");
            return false;
        }

        // Validate email format
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showFieldError(editTextEmail, "Please enter a valid email address");
            return false;
        }

        // Validate password length
        if (password.length() < 6) {
            showFieldError(editTextPassword, "Password must be at least 6 characters");
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

    private void handleForgotPassword() {
        try {
            Intent intent = new Intent(LoginActivity.this, ForgetPasswordActivity.class);

            // Optionally pass the email if user has entered it
            String email = getTextFromEditText(editTextEmail);
            if (!TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                intent.putExtra("email", email);
            }

            startActivity(intent);
            Log.d(TAG, "Navigating to ForgetPasswordActivity");
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to forgot password: " + e.getMessage());
            Toast.makeText(this, "Error navigating. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleGuestLogin() {
        Toast.makeText(this, "Continuing as guest...", Toast.LENGTH_SHORT).show();
        // You can implement guest functionality here
        // For now, we'll navigate to MainActivity
        navigateToMain();
    }

    private void handlePhoneLogin() {
        Toast.makeText(this, "Phone login coming soon!", Toast.LENGTH_SHORT).show();
        // Implement phone authentication here if needed
        // You would typically use Firebase Phone Auth for this
    }

    private void setLoadingState(boolean isLoading) {
        if (buttonLogin != null) {
            buttonLogin.setEnabled(!isLoading);
            buttonLogin.setText(isLoading ? "Signing In..." : "Sign In");
        }

        // Disable other buttons during loading
        if (btnGuestLogin != null) btnGuestLogin.setEnabled(!isLoading);
        if (tvCreateAccount != null) tvCreateAccount.setEnabled(!isLoading);
        if (btnPhoneLogin != null) btnPhoneLogin.setEnabled(!isLoading);
    }

    private void handleAuthError(String errorMessage) {
        Log.e(TAG, errorMessage);

        // Provide user-friendly error messages
        String userMessage = errorMessage;
        if (errorMessage.contains("user-not-found")) {
            userMessage = "No account found with this email address";
        } else if (errorMessage.contains("wrong-password")) {
            userMessage = "Incorrect password";
        } else if (errorMessage.contains("invalid-email")) {
            userMessage = "Invalid email address";
        } else if (errorMessage.contains("user-disabled")) {
            userMessage = "This account has been disabled";
        } else if (errorMessage.contains("too-many-requests")) {
            userMessage = "Too many failed attempts. Please try again later";
        } else if (errorMessage.contains("network-request-failed")) {
            userMessage = "Network error. Please check your connection";
        }

        Toast.makeText(LoginActivity.this, userMessage, Toast.LENGTH_LONG).show();
    }

    private void navigateToSignup() {
        try {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
            // Don't finish() here so user can come back to login
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to signup: " + e.getMessage());
            Toast.makeText(this, "Error navigating. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToMain() {
        try {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // Finish login activity so user can't go back to it
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to main: " + e.getMessage());
            Toast.makeText(this, "Login successful but navigation failed. Please restart the app.",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Add auth state listener
        if (mAuth != null && mAuthListener != null) {
            mAuth.addAuthStateListener(mAuthListener);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Remove auth state listener
        if (mAuth != null && mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up resources if needed
        Log.d(TAG, "LoginActivity destroyed");
    }
}