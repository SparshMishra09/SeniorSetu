package com.example.seniorsetu;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    private static final String TAG = "SignupActivity";

    // UI Components
    private TextInputEditText editTextUsername, editTextEmail, editTextPassword, editTextPhone, editTextAge;
    private Button buttonSignup;
    private ImageView backArrow;
    private TextView tvLoginLink;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private DatabaseReference mDatabase;
    private FirebaseAuth.AuthStateListener mAuthListener;

    // Flag to prevent auto-navigation on initial load
    private boolean hasUserInteracted = false;
    private boolean isSignupInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize Firebase components
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
            db = FirebaseFirestore.getInstance();
            mDatabase = FirebaseDatabase.getInstance().getReference();
            Log.d(TAG, "Firebase initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase: " + e.getMessage());
            Toast.makeText(this, "Error initializing app. Please try again.", Toast.LENGTH_LONG).show();
        }
    }

    private void initializeViews() {
        try {
            editTextUsername = findViewById(R.id.editTextUsername);
            editTextEmail = findViewById(R.id.editTextEmail);
            editTextPassword = findViewById(R.id.editTextPassword);
            editTextPhone = findViewById(R.id.editTextPhone);
            editTextAge = findViewById(R.id.editTextAge);
            buttonSignup = findViewById(R.id.buttonSignup);
            backArrow = findViewById(R.id.backArrow);
            tvLoginLink = findViewById(R.id.tvLoginLink);
            Log.d(TAG, "Views initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage());
            Toast.makeText(this, "Error loading interface. Please restart the app.", Toast.LENGTH_LONG).show();
        }
    }

    private void setClickListeners() {
        // Back arrow click listener
        if (backArrow != null) {
            backArrow.setOnClickListener(v -> {
                hasUserInteracted = true;
                navigateToLogin();
            });
        }

        // Sign in link click listener
        if (tvLoginLink != null) {
            tvLoginLink.setOnClickListener(v -> {
                hasUserInteracted = true;
                navigateToLogin();
            });
        }

        // Sign up button click listener
        if (buttonSignup != null) {
            buttonSignup.setOnClickListener(v -> {
                hasUserInteracted = true;
                createAccount();
            });
        }
    }

    private void setupBackButton() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                hasUserInteracted = true;
                navigateToLogin();
            }
        });
    }

    private void setupAuthStateListener() {
        mAuthListener = firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null && hasUserInteracted && isSignupInProgress && isUserProperlyAuthenticated(user)) {
                // User is properly signed up and authenticated
                Log.d(TAG, "User successfully signed up and authenticated: " + user.getUid());
                // Navigation will be handled by the signup success callback
            } else if (user != null) {
                // User exists but either hasn't interacted or isn't from current signup
                Log.d(TAG, "User found but not from current signup process");
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

    private void createAccount() {
        // Get input values
        String username = getTextFromEditText(editTextUsername);
        String email = getTextFromEditText(editTextEmail);
        String password = getTextFromEditText(editTextPassword);
        String phone = getTextFromEditText(editTextPhone);
        String ageStr = getTextFromEditText(editTextAge);

        // Validate inputs
        if (!validateInputs(username, email, password, phone, ageStr)) {
            return;
        }

        // Show loading state
        setLoadingState(true);
        isSignupInProgress = true;

        // Create user with email and password
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "User account created successfully");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Update user profile with username
                            updateUserProfile(user, username);
                            // Save additional user data
                            saveUserData(user.getUid(), username, email, phone, ageStr);
                        } else {
                            handleAuthError("User creation failed - user is null");
                        }
                    } else {
                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() : "Unknown error occurred";
                        handleAuthError("Authentication failed: " + errorMessage);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Create account failed: " + e.getMessage());
                    handleAuthError("Failed to create account: " + e.getMessage());
                });
    }

    private String getTextFromEditText(TextInputEditText editText) {
        return editText != null && editText.getText() != null ?
                editText.getText().toString().trim() : "";
    }

    private boolean validateInputs(String username, String email, String password, String phone, String ageStr) {
        // Check if any field is empty
        if (TextUtils.isEmpty(username)) {
            showFieldError(editTextUsername, "Username is required");
            return false;
        }

        if (TextUtils.isEmpty(email)) {
            showFieldError(editTextEmail, "Email is required");
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            showFieldError(editTextPassword, "Password is required");
            return false;
        }

        if (TextUtils.isEmpty(phone)) {
            showFieldError(editTextPhone, "Phone number is required");
            return false;
        }

        if (TextUtils.isEmpty(ageStr)) {
            showFieldError(editTextAge, "Age is required");
            return false;
        }

        // Validate username length
        if (username.length() < 3) {
            showFieldError(editTextUsername, "Username must be at least 3 characters");
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

        // Validate phone number (basic validation)
        if (phone.length() < 10) {
            showFieldError(editTextPhone, "Please enter a valid phone number");
            return false;
        }

        // Validate age
        try {
            int age = Integer.parseInt(ageStr);
            if (age < 1 || age > 120) {
                showFieldError(editTextAge, "Please enter a valid age (1-120)");
                return false;
            }
        } catch (NumberFormatException e) {
            showFieldError(editTextAge, "Please enter a valid age");
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

    private void updateUserProfile(FirebaseUser user, String username) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(username)
                .build();

        user.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "User profile updated successfully");
                    } else {
                        Log.w(TAG, "Failed to update user profile: " +
                                (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                });
    }

    private void saveUserData(String userId, String username, String email, String phone, String ageStr) {
        // Create a user data map
        Map<String, Object> userData = new HashMap<>();
        userData.put("username", username);
        userData.put("email", email);
        userData.put("phone", phone);
        userData.put("age", Integer.parseInt(ageStr));
        userData.put("createdAt", System.currentTimeMillis());

        // Save to Firestore first
        db.collection("users").document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User data saved to Firestore successfully");
                    // After Firestore success, save to Realtime Database
                    saveToRealtimeDatabase(userId, userData);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving to Firestore: " + e.getMessage());
                    // Even if Firestore fails, try Realtime Database
                    saveToRealtimeDatabase(userId, userData);
                });
    }

    private void saveToRealtimeDatabase(String userId, Map<String, Object> userData) {
        mDatabase.child("users").child(userId).setValue(userData)
                .addOnCompleteListener(task -> {
                    setLoadingState(false);
                    isSignupInProgress = false;

                    if (task.isSuccessful()) {
                        Log.d(TAG, "User data saved to Realtime Database successfully");
                        Toast.makeText(SignupActivity.this, "Account created successfully!",
                                Toast.LENGTH_SHORT).show();

                        // Navigate to MainActivity (home page) after successful signup
                        navigateToMain();

                    } else {
                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() : "Unknown error";
                        Log.e(TAG, "Error saving to Realtime Database: " + errorMessage);

                        // Still consider it a success if user was created
                        Toast.makeText(SignupActivity.this, "Account created! Some data may not be saved.",
                                Toast.LENGTH_LONG).show();

                        // Navigate to MainActivity even if database save failed
                        navigateToMain();
                    }
                });
    }

    private void handleAuthError(String errorMessage) {
        Log.e(TAG, errorMessage);
        setLoadingState(false);
        isSignupInProgress = false;

        // Provide user-friendly error messages
        String userMessage = errorMessage;
        if (errorMessage.contains("email-already-in-use")) {
            userMessage = "This email address is already registered";
        } else if (errorMessage.contains("weak-password")) {
            userMessage = "Password is too weak. Please choose a stronger password";
        } else if (errorMessage.contains("invalid-email")) {
            userMessage = "Invalid email address";
        } else if (errorMessage.contains("network-request-failed")) {
            userMessage = "Network error. Please check your connection";
        } else if (errorMessage.contains("too-many-requests")) {
            userMessage = "Too many requests. Please try again later";
        }

        Toast.makeText(SignupActivity.this, userMessage, Toast.LENGTH_LONG).show();
    }

    private void setLoadingState(boolean isLoading) {
        if (buttonSignup != null) {
            buttonSignup.setEnabled(!isLoading);
            buttonSignup.setText(isLoading ? "Creating Account..." : "Create Account");
        }

        // Disable other UI elements during loading
        if (backArrow != null) backArrow.setEnabled(!isLoading);
        if (tvLoginLink != null) tvLoginLink.setEnabled(!isLoading);
        if (editTextUsername != null) editTextUsername.setEnabled(!isLoading);
        if (editTextEmail != null) editTextEmail.setEnabled(!isLoading);
        if (editTextPassword != null) editTextPassword.setEnabled(!isLoading);
        if (editTextPhone != null) editTextPhone.setEnabled(!isLoading);
        if (editTextAge != null) editTextAge.setEnabled(!isLoading);
    }

    private void navigateToLogin() {
        try {
            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to login: " + e.getMessage());
            Toast.makeText(this, "Error navigating. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToMain() {
        try {
            Intent intent = new Intent(SignupActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to main: " + e.getMessage());
            Toast.makeText(this, "Account created but navigation failed. Please login manually.",
                    Toast.LENGTH_LONG).show();
            navigateToLogin();
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
        // Clean up resources
        isSignupInProgress = false;
        Log.d(TAG, "SignupActivity destroyed");
    }
}