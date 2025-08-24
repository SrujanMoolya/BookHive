package com.svvaap.bookhive;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.svvaap.bookhive.databinding.FragmentManageBooksBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class ManageBooksFragment extends Fragment {
    private FragmentManageBooksBinding binding;
    private List<Book> books = new ArrayList<>();
    private ManageBookAdapter adapter;
    private DatabaseReference booksRef;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentManageBooksBinding.inflate(inflater, container, false);
        setupRecyclerView();
        fetchBooks();
        return binding.getRoot();
    }

    private void setupRecyclerView() {
        adapter = new ManageBookAdapter(books, this::deleteBook, this::toggleBookVisibility);
        binding.booksRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.booksRecyclerView.setAdapter(adapter);
    }

    private void fetchBooks() {
        booksRef = FirebaseDatabase.getInstance().getReference("ebooks");
        booksRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                books.clear();
                for (DataSnapshot bookSnap : snapshot.getChildren()) {
                    Book book = bookSnap.getValue(Book.class);
                    if (book != null) {
                        book.id = bookSnap.getKey();
                        books.add(book);
                    }
                }
                adapter.notifyDataSetChanged();
                binding.noBooksText.setVisibility(books.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }

    private void deleteBook(Book book) {
        if (book.id == null) return;
        
        DatabaseReference bookRef = FirebaseDatabase.getInstance().getReference("ebooks").child(book.id);
        bookRef.removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Book deleted successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Failed to delete book", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void toggleBookVisibility(Book book) {
        if (book.id == null) return;
        
        String newVisibility = "public".equals(book.visibility) ? "private" : "public";
        DatabaseReference bookRef = FirebaseDatabase.getInstance().getReference("ebooks").child(book.id);
        bookRef.child("visibility").setValue(newVisibility).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Book visibility updated to " + newVisibility, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Failed to update book visibility", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (booksRef != null) {
            booksRef.removeEventListener(null);
        }
        binding = null;
    }

    // ManageBook adapter
    private class ManageBookAdapter extends RecyclerView.Adapter<ManageBookAdapter.ManageBookViewHolder> {
        private List<Book> bookList;
        private OnBookActionListener deleteListener;
        private OnBookActionListener visibilityListener;

        public interface OnBookActionListener {
            void onBookAction(Book book);
        }

        public ManageBookAdapter(List<Book> bookList, OnBookActionListener deleteListener, OnBookActionListener visibilityListener) {
            this.bookList = bookList;
            this.deleteListener = deleteListener;
            this.visibilityListener = visibilityListener;
        }

        @NonNull
        @Override
        public ManageBookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_manage_book, parent, false);
            return new ManageBookViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ManageBookViewHolder holder, int position) {
            Book book = bookList.get(position);
            holder.bind(book);
        }

        @Override
        public int getItemCount() {
            return bookList.size();
        }

        class ManageBookViewHolder extends RecyclerView.ViewHolder {
            private TextView bookTitleText, bookAuthorText, bookCategoryText, bookPriceText, bookVisibilityText;
            private View deleteButton, visibilityButton;

            public ManageBookViewHolder(@NonNull View itemView) {
                super(itemView);
                bookTitleText = itemView.findViewById(R.id.book_title);
                bookAuthorText = itemView.findViewById(R.id.book_author);
                bookCategoryText = itemView.findViewById(R.id.book_category);
                bookPriceText = itemView.findViewById(R.id.book_price);
                bookVisibilityText = itemView.findViewById(R.id.book_visibility);
                deleteButton = itemView.findViewById(R.id.delete_button);
                visibilityButton = itemView.findViewById(R.id.visibility_button);
            }

            public void bind(Book book) {
                bookTitleText.setText(book.title != null ? book.title : "Unknown Title");
                bookAuthorText.setText(book.author != null ? book.author : "Unknown Author");
                bookCategoryText.setText(book.category != null ? book.category : "Unknown Category");
                bookPriceText.setText("$" + book.price);
                bookVisibilityText.setText(book.visibility != null ? book.visibility.toUpperCase() : "PRIVATE");

                // Set visibility color
                int visibilityColor = "public".equals(book.visibility) ? 
                    getResources().getColor(android.R.color.holo_green_dark) : 
                    getResources().getColor(android.R.color.holo_red_dark);
                bookVisibilityText.setTextColor(visibilityColor);

                // Set button listeners
                deleteButton.setOnClickListener(v -> {
                    if (deleteListener != null) {
                        deleteListener.onBookAction(book);
                    }
                });

                visibilityButton.setOnClickListener(v -> {
                    if (visibilityListener != null) {
                        visibilityListener.onBookAction(book);
                    }
                });
            }
        }
    }
} 