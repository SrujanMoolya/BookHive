package com.svvaap.bookhive;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class BookDetailFragment extends Fragment {
    private String bookId;
    private Book book;
    private TextView titleView, authorView, descriptionView, progressView;
    private Button readBookButton;
    private ProgressBar readingProgressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_book_detail, container, false);
        titleView = view.findViewById(R.id.book_detail_title);
        authorView = view.findViewById(R.id.book_detail_author);
        descriptionView = view.findViewById(R.id.book_detail_description);
        progressView = view.findViewById(R.id.book_detail_progress);
        readBookButton = view.findViewById(R.id.read_book_button);
        readingProgressBar = view.findViewById(R.id.reading_progress_bar);

        if (getArguments() != null) {
            bookId = getArguments().getString("bookId");
            if (bookId != null) {
                fetchBookDetails();
            } else {
                // Optionally show an error message or handle gracefully
                titleView.setText("Error: Book ID not found");
                authorView.setText("");
                descriptionView.setText("");
                progressView.setText("");
                readingProgressBar.setProgress(0);
            }
        }

        readBookButton.setOnClickListener(v -> openBookReader());
        return view;
    }

    private void fetchBookDetails() {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("ebooks").child(bookId);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                book = snapshot.getValue(Book.class);
                if (book != null) {
                    titleView.setText(book.title);
                    authorView.setText(book.author);
                    descriptionView.setText(book.description);
                    // For demo, set progress to 0
                    progressView.setText("Progress: 0%");
                    readingProgressBar.setProgress(0);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void openBookReader() {
        Bundle bundle = new Bundle();
        bundle.putString("bookId", bookId);
        BookReadViewFragment fragment = new BookReadViewFragment();
        fragment.setArguments(bundle);
        requireActivity().getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit();
    }
} 