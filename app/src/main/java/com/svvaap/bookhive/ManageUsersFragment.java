package com.svvaap.bookhive;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.svvaap.bookhive.databinding.FragmentManageUsersBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class ManageUsersFragment extends Fragment {
    private FragmentManageUsersBinding binding;
    private List<User> users = new ArrayList<>();
    private UserAdapter adapter;
    private DatabaseReference usersRef;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentManageUsersBinding.inflate(inflater, container, false);
        setupRecyclerView();
        fetchUsers();
        return binding.getRoot();
    }

    private void setupRecyclerView() {
        adapter = new UserAdapter(users, this::toggleUserStatus);
        binding.usersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.usersRecyclerView.setAdapter(adapter);
    }

    private void fetchUsers() {
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                users.clear();
                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    User user = userSnap.getValue(User.class);
                    if (user != null) {
                        user.userId = userSnap.getKey();
                        users.add(user);
                    }
                }
                adapter.notifyDataSetChanged();
                binding.noUsersText.setVisibility(users.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }

    private void toggleUserStatus(User user) {
        if (user.userId == null) return;
        
        String newStatus = "active".equals(user.status) ? "suspended" : "active";
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(user.userId);
        userRef.child("status").setValue(newStatus).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "User " + newStatus, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Failed to update user status", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (usersRef != null) {
            usersRef.removeEventListener(null);
        }
        binding = null;
    }

    // User data model
    public static class User {
        public String userId;
        public String name;
        public String email;
        public String photoUrl;
        public String status; // "active", "suspended"
        public String joinDate;
        public int totalPurchases;

        public User() {}
    }

    // User adapter
    private class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
        private List<User> userList;
        private OnUserActionListener actionListener;

        public interface OnUserActionListener {
            void onUserAction(User user);
        }

        public UserAdapter(List<User> userList, OnUserActionListener actionListener) {
            this.userList = userList;
            this.actionListener = actionListener;
        }

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
            return new UserViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            User user = userList.get(position);
            holder.bind(user);
        }

        @Override
        public int getItemCount() {
            return userList.size();
        }

        class UserViewHolder extends RecyclerView.ViewHolder {
            private TextView userNameText, userEmailText, userStatusText, joinDateText, purchasesText, actionButton;

            public UserViewHolder(@NonNull View itemView) {
                super(itemView);
                userNameText = itemView.findViewById(R.id.user_name);
                userEmailText = itemView.findViewById(R.id.user_email);
                userStatusText = itemView.findViewById(R.id.user_status);
                joinDateText = itemView.findViewById(R.id.join_date);
                purchasesText = itemView.findViewById(R.id.total_purchases);
                actionButton = itemView.findViewById(R.id.action_button);
            }

            public void bind(User user) {
                userNameText.setText(user.name != null ? user.name : "Unknown User");
                userEmailText.setText(user.email != null ? user.email : "No Email");
                userStatusText.setText(user.status != null ? user.status.toUpperCase() : "ACTIVE");
                joinDateText.setText(user.joinDate != null ? user.joinDate : "Unknown Date");
                purchasesText.setText("Purchases: " + user.totalPurchases);

                // Set status color
                int statusColor = "active".equals(user.status) ? 
                    getResources().getColor(android.R.color.holo_green_dark) : 
                    getResources().getColor(android.R.color.holo_red_dark);
                userStatusText.setTextColor(statusColor);

                // Set action button
                String actionText = "active".equals(user.status) ? "Suspend" : "Activate";
                actionButton.setText(actionText);
                actionButton.setOnClickListener(v -> {
                    if (actionListener != null) {
                        actionListener.onUserAction(user);
                    }
                });
            }
        }
    }
} 