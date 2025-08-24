package com.svvaap.bookhive;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import com.svvaap.bookhive.databinding.FragmentLoginBinding;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.*;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LoginFragment extends Fragment {
    private FragmentLoginBinding binding;
    private FirebaseAuth mAuth;
    private GoogleSignInClient googleSignInClient;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        mAuth = FirebaseAuth.getInstance();

        // Google Sign-In options
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // Add this to your strings.xml from Firebase console
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);

        binding.loginButton.setOnClickListener(v -> loginWithEmail());
        binding.registerButton.setOnClickListener(v -> registerWithEmail());
        binding.googleSigninButton.setOnClickListener(v -> signInWithGoogle());

        return binding.getRoot();
    }

    private void loginWithEmail() {
        String email = binding.emailInput.getText().toString().trim();
        String password = binding.passwordInput.getText().toString().trim();
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(getContext(), "Email and password required", Toast.LENGTH_SHORT).show();
            return;
        }
        // Admin login check
        if (email.equals("admin@gmail.com") && password.equals("admin@123")) {
            NavController navController = NavHostFragment.findNavController(LoginFragment.this);
            navController.navigate(R.id.action_LoginFragment_to_AdminDashboardFragment);
            return;
        }
        binding.loading.setVisibility(View.VISIBLE);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), task -> {
                    binding.loading.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        onAuthSuccess(mAuth.getCurrentUser());
                    } else {
                        Toast.makeText(getContext(), "Login failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void registerWithEmail() {
        String email = binding.emailInput.getText().toString().trim();
        String password = binding.passwordInput.getText().toString().trim();
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(getContext(), "Email and password required", Toast.LENGTH_SHORT).show();
            return;
        }
        binding.loading.setVisibility(View.VISIBLE);
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), task -> {
                    binding.loading.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        onAuthSuccess(mAuth.getCurrentUser());
                    } else {
                        Toast.makeText(getContext(), "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    // Google Sign-In
    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        firebaseAuthWithGoogle(account.getIdToken());
                    } catch (ApiException e) {
                        Toast.makeText(getContext(), "Google sign-in failed", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private void signInWithGoogle() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void firebaseAuthWithGoogle(String idToken) {
        binding.loading.setVisibility(View.VISIBLE);
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(requireActivity(), task -> {
                    binding.loading.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        onAuthSuccess(mAuth.getCurrentUser());
                    } else {
                        Toast.makeText(getContext(), "Google sign-in failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void onAuthSuccess(FirebaseUser user) {
        // Save user info to database
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.child(user.getUid()).setValue(new UserProfile(
                user.getDisplayName() != null ? user.getDisplayName() : "",
                user.getEmail(),
                user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : ""
        ));
        // Navigate to Home and clear Login from back stack
        NavController navController = NavHostFragment.findNavController(LoginFragment.this);
        NavOptions navOptions = new NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, true)
                .build();
        navController.navigate(R.id.HomeFragment, null, navOptions);
    }

    public static class UserProfile {
        public String name, email, photoUrl;
        public UserProfile() {}
        public UserProfile(String name, String email, String photoUrl) {
            this.name = name;
            this.email = email;
            this.photoUrl = photoUrl;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 