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
    private ValueEventListener booksListener;

    // For purchased books filtering
    private List<String> purchasedBookIds = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentManageBooksBinding.inflate(inflater, container, false);
        setupRecyclerView();
        fetchPurchasedBooks();
        return binding.getRoot();
    }

    private void setupRecyclerView() {
        adapter = new ManageBookAdapter(requireContext(), books, this::deleteBook, this::toggleBookVisibility);
        binding.booksRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.booksRecyclerView.setAdapter(adapter);
    }

    private void fetchPurchasedBooks() {
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            // User not logged in, show all books or show a message
            fetchBooksByIds(new ArrayList<>()); // Pass empty list to show no books, or call fetchBooks() to show all
            return;
        }
        String userId = user.getUid();
        DatabaseReference purchasesRef = com.google.firebase.database.FirebaseDatabase.getInstance()
                .getReference("users").child(userId).child("purchases");

        purchasesRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                purchasedBookIds.clear();
                for (com.google.firebase.database.DataSnapshot ds : snapshot.getChildren()) {
                    // Support both structures: value contains bookId OR key is bookId with value=true
                    String bookId = ds.getValue(String.class);
                    if (bookId != null && !bookId.isEmpty()) {
                        purchasedBookIds.add(bookId);
                    } else if (ds.getKey() != null && (ds.getValue() == null || Boolean.TRUE.equals(ds.getValue()))) {
                        purchasedBookIds.add(ds.getKey());
                    }
                }
                fetchBooksByIds(purchasedBookIds);
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                // Handle error if needed
            }
        });
    }

    private void fetchBooksByIds(List<String> bookIds) {
        booksRef = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("ebooks");
        booksListener = new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                books.clear();
                for (com.google.firebase.database.DataSnapshot bookSnap : snapshot.getChildren()) {
                    Book book = safeMapToBook(bookSnap);
                    String key = bookSnap.getKey();
                    if (book != null) {
                        book.id = key;
                        if (bookIds == null || bookIds.isEmpty() || bookIds.contains(book.id)) {
                            books.add(book);
                        }
                    }
                }
                adapter.notifyDataSetChanged();
                binding.noBooksText.setVisibility(books.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                // Handle error
            }
        };
        booksRef.addValueEventListener(booksListener);
    }

    // Defensive mapping: avoid Firebase automatic failures when types mismatch (e.g., price stored as string)
    private Book safeMapToBook(DataSnapshot snap) {
        if (snap == null) return null;
        Object raw = snap.getValue();
        if (!(raw instanceof java.util.Map)) {
            // fallback to automatic mapping
            try {
                return snap.getValue(Book.class);
            } catch (Exception e) {
                return null;
            }
        }
        java.util.Map map = (java.util.Map) raw;
        Book b = new Book();
        b.title = asString(map.get("title"));
        b.author = asString(map.get("author"));
        b.category = asString(map.get("category"));
        b.language = asString(map.get("language"));
        b.description = asString(map.get("description"));
        b.coverImageUrl = asString(map.get("coverImageUrl"));
        b.fileUrl = asString(map.get("fileUrl"));
        b.visibility = asString(map.get("visibility"));
        b.uploadDate = asString(map.get("uploadDate"));
        b.price = asDouble(map.get("price"));
        return b;
    }

    private String asString(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private double asDouble(Object v) {
        if (v == null) return 0d;
        if (v instanceof Number) return ((Number) v).doubleValue();
        if (v instanceof String) {
            try { return Double.parseDouble(((String) v).trim()); } catch (Exception ignored) {}
        }
        return 0d;
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
        if (booksRef != null && booksListener != null) {
            booksRef.removeEventListener(booksListener);
        }
        binding = null;
    }

    // ManageBook adapter
    private static class ManageBookAdapter extends RecyclerView.Adapter<ManageBookAdapter.ManageBookViewHolder> {
        private final List<Book> bookList;
        private final OnBookActionListener deleteListener;
        private final OnBookActionListener visibilityListener;
        private final android.content.Context context;

        public interface OnBookActionListener {
            void onBookAction(Book book);
        }

        public ManageBookAdapter(android.content.Context context, List<Book> bookList, OnBookActionListener deleteListener, OnBookActionListener visibilityListener) {
            this.context = context;
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
                bookPriceText.setText("₹" + book.price);
                bookVisibilityText.setText(book.visibility != null ? book.visibility.toUpperCase() : "PRIVATE");

                // Set visibility color using context
                int visibilityColor = "public".equals(book.visibility) ? 
                    context.getResources().getColor(android.R.color.holo_green_dark) : 
                    context.getResources().getColor(android.R.color.holo_red_dark);
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