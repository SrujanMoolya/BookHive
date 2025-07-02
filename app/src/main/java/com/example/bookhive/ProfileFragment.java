package com.example.bookhive;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import com.example.bookhive.databinding.FragmentProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileFragment extends Fragment {
    private FragmentProfileBinding binding;
    private FirebaseAuth mAuth;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        mAuth = FirebaseAuth.getInstance();
        updateUI(mAuth.getCurrentUser());
        binding.loginButton.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(ProfileFragment.this);
            navController.navigate(R.id.action_ProfileFragment_to_LoginFragment);
        });
        binding.logoutButton.setOnClickListener(v -> {
            mAuth.signOut();
            updateUI(null);
        });
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            NavController navController = NavHostFragment.findNavController(ProfileFragment.this);
            navController.navigate(R.id.action_ProfileFragment_to_LoginFragment);
        } else {
            updateUI(user);
        }
    }

    private void updateUI(FirebaseUser user) {
        if (user == null) {
            binding.profileName.setText("Guest");
            binding.profileEmail.setText("Not logged in");
            binding.profileImage.setImageResource(R.drawable.ic_profile);
            binding.loginButton.setVisibility(View.VISIBLE);
            binding.logoutButton.setVisibility(View.GONE);
        } else {
            binding.profileName.setText(user.getDisplayName() != null ? user.getDisplayName() : "User");
            binding.profileEmail.setText(user.getEmail());
            // Optionally load user.getPhotoUrl() with Glide/Picasso
            binding.loginButton.setVisibility(View.GONE);
            binding.logoutButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 