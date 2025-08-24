package com.svvaap.bookhive;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.svvaap.bookhive.databinding.FragmentFeedbacksBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import android.widget.TextView;

public class FeedbacksFragment extends Fragment {
    private FragmentFeedbacksBinding binding;
    private List<Feedback> feedbacks = new ArrayList<>();
    private FeedbackAdapter adapter;
    private DatabaseReference feedbacksRef;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentFeedbacksBinding.inflate(inflater, container, false);
        setupRecyclerView();
        fetchFeedbacks();
        return binding.getRoot();
    }

    private void setupRecyclerView() {
        adapter = new FeedbackAdapter(feedbacks);
        binding.feedbacksRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.feedbacksRecyclerView.setAdapter(adapter);
    }

    private void fetchFeedbacks() {
        feedbacksRef = FirebaseDatabase.getInstance().getReference("feedbacks");
        feedbacksRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                feedbacks.clear();
                double totalRating = 0;
                int totalFeedbacks = 0;
                
                for (DataSnapshot feedbackSnap : snapshot.getChildren()) {
                    Feedback feedback = feedbackSnap.getValue(Feedback.class);
                    if (feedback != null) {
                        feedback.feedbackId = feedbackSnap.getKey();
                        feedbacks.add(feedback);
                        totalRating += feedback.rating;
                        totalFeedbacks++;
                    }
                }
                
                // Sort by date (newest first)
                Collections.sort(feedbacks, (a, b) -> b.date.compareTo(a.date));
                
                // Update UI
                if (totalFeedbacks > 0) {
                    double averageRating = totalRating / totalFeedbacks;
                    binding.averageRatingText.setText(String.format("%.1f", averageRating));
                    binding.totalFeedbacksText.setText(String.valueOf(totalFeedbacks));
                } else {
                    binding.averageRatingText.setText("0.0");
                    binding.totalFeedbacksText.setText("0");
                }
                
                adapter.notifyDataSetChanged();
                binding.noFeedbacksText.setVisibility(feedbacks.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (feedbacksRef != null) {
            feedbacksRef.removeEventListener(null);
        }
        binding = null;
    }

    // Feedback data model
    public static class Feedback {
        public String feedbackId;
        public String userId;
        public String userName;
        public String bookId;
        public String bookTitle;
        public String comment;
        public double rating; // 1-5 stars
        public String date;
        public String status; // "pending", "reviewed", "resolved"

        public Feedback() {}
    }

    // Feedback adapter
    private class FeedbackAdapter extends RecyclerView.Adapter<FeedbackAdapter.FeedbackViewHolder> {
        private List<Feedback> feedbackList;

        public FeedbackAdapter(List<Feedback> feedbackList) {
            this.feedbackList = feedbackList;
        }

        @NonNull
        @Override
        public FeedbackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_feedback, parent, false);
            return new FeedbackViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull FeedbackViewHolder holder, int position) {
            Feedback feedback = feedbackList.get(position);
            holder.bind(feedback);
        }

        @Override
        public int getItemCount() {
            return feedbackList.size();
        }

        class FeedbackViewHolder extends RecyclerView.ViewHolder {
            private TextView userNameText, bookTitleText, commentText, ratingText, dateText, statusText;

            public FeedbackViewHolder(@NonNull View itemView) {
                super(itemView);
                userNameText = itemView.findViewById(R.id.user_name);
                bookTitleText = itemView.findViewById(R.id.book_title);
                commentText = itemView.findViewById(R.id.comment);
                ratingText = itemView.findViewById(R.id.rating);
                dateText = itemView.findViewById(R.id.date);
                statusText = itemView.findViewById(R.id.status);
            }

            public void bind(Feedback feedback) {
                userNameText.setText(feedback.userName != null ? feedback.userName : "Unknown User");
                bookTitleText.setText(feedback.bookTitle != null ? feedback.bookTitle : "Unknown Book");
                commentText.setText(feedback.comment != null ? feedback.comment : "No comment");
                ratingText.setText("★ " + feedback.rating + "/5");
                dateText.setText(feedback.date != null ? feedback.date : "Unknown Date");
                statusText.setText(feedback.status != null ? feedback.status.toUpperCase() : "PENDING");

                // Set rating color
                int ratingColor;
                if (feedback.rating >= 4) {
                    ratingColor = getResources().getColor(android.R.color.holo_green_dark);
                } else if (feedback.rating >= 3) {
                    ratingColor = getResources().getColor(android.R.color.holo_orange_dark);
                } else {
                    ratingColor = getResources().getColor(android.R.color.holo_red_dark);
                }
                ratingText.setTextColor(ratingColor);

                // Set status color
                int statusColor;
                switch (feedback.status != null ? feedback.status.toLowerCase() : "pending") {
                    case "resolved":
                        statusColor = getResources().getColor(android.R.color.holo_green_dark);
                        break;
                    case "reviewed":
                        statusColor = getResources().getColor(android.R.color.holo_blue_dark);
                        break;
                    default:
                        statusColor = getResources().getColor(android.R.color.holo_orange_dark);
                        break;
                }
                statusText.setTextColor(statusColor);
            }
        }
    }
} 