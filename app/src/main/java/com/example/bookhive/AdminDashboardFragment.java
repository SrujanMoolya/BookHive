package com.example.bookhive;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import com.google.firebase.auth.FirebaseAuth;

public class AdminDashboardFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_dashboard, container, false);

        TextView uploadBooksButton = view.findViewById(R.id.button_upload_books);
        TextView viewOrdersButton = view.findViewById(R.id.button_view_orders);
        TextView manageUsersButton = view.findViewById(R.id.button_manage_users);
        TextView bookSalesAnalyticsButton = view.findViewById(R.id.button_book_sales_analytics);
        TextView feedbacksButton = view.findViewById(R.id.button_feedbacks);
        TextView manageBooksButton = view.findViewById(R.id.button_manage_books);
        View logoutButton = view.findViewById(R.id.button_logout);

        NavController navController = NavHostFragment.findNavController(this);

        uploadBooksButton.setOnClickListener(v -> navController.navigate(R.id.action_AdminDashboardFragment_to_UploadBookFragment));
        viewOrdersButton.setOnClickListener(v -> navController.navigate(R.id.action_AdminDashboardFragment_to_ViewOrdersFragment));
        manageUsersButton.setOnClickListener(v -> navController.navigate(R.id.action_AdminDashboardFragment_to_ManageUsersFragment));
        bookSalesAnalyticsButton.setOnClickListener(v -> navController.navigate(R.id.action_AdminDashboardFragment_to_BookSalesAnalyticsFragment));
        feedbacksButton.setOnClickListener(v -> navController.navigate(R.id.action_AdminDashboardFragment_to_FeedbacksFragment));
        manageBooksButton.setOnClickListener(v -> navController.navigate(R.id.action_AdminDashboardFragment_to_ManageBooksFragment));
        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(getContext(), "Logged out", Toast.LENGTH_SHORT).show();
            navController.popBackStack(R.id.LoginFragment, false);
        });

        return view;
    }
} 