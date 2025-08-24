package com.svvaap.bookhive;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.bumptech.glide.Glide;
import com.svvaap.bookhive.databinding.FragmentCatalogueBinding;
import com.svvaap.bookhive.databinding.ItemBookBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class CatalogueFragment extends Fragment {
    private FragmentCatalogueBinding binding;
    private List<Book> allBooks = new ArrayList<>();
    private BookAdapter adapter;
    private DatabaseReference ebooksRef;
    private ValueEventListener ebooksListener;
    private DatabaseReference userPurchasesRef;
    private ValueEventListener userPurchasesListener;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCatalogueBinding.inflate(inflater, container, false);
        setupRecyclerView();
        fetchBooksFromFirebase();
        setupSearch();
        applyIncomingCategoryFilterIfAny();
        return binding.getRoot();
    }

    private void applyIncomingCategoryFilterIfAny() {
        Bundle args = getArguments();
        if (args == null) return;
        String category = args.getString("categoryFilter", null);
        if (category == null || category.isEmpty()) return;
        binding.catalogueSearchInput.setText("");
        filterByCategory(category);
    }

    private void fetchBooksFromFirebase() {
        // If "My Books" (purchased) view, filter by purchases/{uid}
        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null ? com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid != null && getArguments() != null && getArguments().getString("categoryFilter") == null) {
            // User is logged in and accessing "My Books" - show purchased books
            userPurchasesRef = FirebaseDatabase.getInstance().getReference("purchases").child(uid);
            userPurchasesListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (binding == null) return;
                    java.util.Set<String> purchasedIds = new java.util.HashSet<>();
                    for (DataSnapshot s : snapshot.getChildren()) {
                        purchasedIds.add(s.getKey());
                    }
                    loadBooksByIds(purchasedIds);
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            };
            userPurchasesRef.addValueEventListener(userPurchasesListener);
            return;
        } else if (uid == null && getArguments() != null && getArguments().getString("categoryFilter") == null) {
            // User not logged in and trying to access "My Books" - show login prompt
            showLoginPrompt();
            return;
        }
        // Show all books (for category browsing or general catalogue)
        ebooksRef = FirebaseDatabase.getInstance().getReference("ebooks");
        ebooksListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (binding == null) return;
                allBooks.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Book book = safeMapToBook(snap);
                    if (book != null) { book.id = snap.getKey(); allBooks.add(book); }
                }
                // If opened via category filter keep it applied, else show all
                Bundle args = getArguments();
                if (args != null) {
                    String category = args.getString("categoryFilter", null);
                    if (category != null && !category.isEmpty()) {
                        filterByCategory(category);
                    } else {
                        adapter.updateBooks(new ArrayList<>(allBooks));
                    }
                } else {
                    adapter.updateBooks(new ArrayList<>(allBooks));
                }
                safeSetNoBooksVisibility(allBooks.isEmpty());
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        ebooksRef.addValueEventListener(ebooksListener);
    }

    private void showLoginPrompt() {
        if (binding == null) return;
        binding.catalogueGrid.setVisibility(View.GONE);
        binding.catalogueSearchInput.setVisibility(View.GONE);
        binding.noBooksText.setVisibility(View.VISIBLE);
        binding.noBooksText.setText("Please login to view your purchased books");
        binding.noBooksText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        binding.noBooksText.setTextSize(16);
        binding.noBooksText.setTextColor(getResources().getColor(android.R.color.darker_gray));
    }

    private Book safeMapToBook(DataSnapshot snap) {
        Object raw = snap.getValue();
        if (!(raw instanceof java.util.Map)) return snap.getValue(Book.class);
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
        if (v instanceof Number) return ((Number) v).doubleValue();
        if (v instanceof String) {
            try { return Double.parseDouble((String) v); } catch (Exception ignored) {}
        }
        return 0d;
    }

    private void loadBooksByIds(java.util.Set<String> ids) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("ebooks");
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (binding == null) return;
                allBooks.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    String id = snap.getKey();
                    if (id != null && ids.contains(id)) {
                        Book book = safeMapToBook(snap);
                        if (book != null) { book.id = id; allBooks.add(book); }
                    }
                }
                adapter.updateBooks(new ArrayList<>(allBooks));
                safeSetNoBooksVisibility(allBooks.isEmpty());
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupRecyclerView() {
        adapter = new BookAdapter(new ArrayList<>(allBooks));
        binding.catalogueGrid.setLayoutManager(new GridLayoutManager(getContext(), 2));
         binding.catalogueGrid.setAdapter(adapter);
        safeSetNoBooksVisibility(false);
    }

    private void setupSearch() {
        binding.catalogueSearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterBooks(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterBooks(String query) {
        List<Book> filtered = new ArrayList<>();
        for (Book book : allBooks) {
            if (book.title.toLowerCase().contains(query.toLowerCase()) ||
                book.author.toLowerCase().contains(query.toLowerCase())) {
                filtered.add(book);
            }
        }
        adapter.updateBooks(filtered);
        safeSetNoBooksVisibility(filtered.isEmpty());
    }

    private void filterByCategory(String category) {
        List<Book> filtered = new ArrayList<>();
        for (Book book : allBooks) {
            if (book.category != null && book.category.equalsIgnoreCase(category)) {
                filtered.add(book);
            }
        }
        adapter.updateBooks(filtered);
        safeSetNoBooksVisibility(filtered.isEmpty());
    }

    private void safeSetNoBooksVisibility(boolean show) {
        if (binding == null) return;
        binding.noBooksText.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (ebooksRef != null && ebooksListener != null) {
            ebooksRef.removeEventListener(ebooksListener);
            ebooksListener = null;
        }
        if (userPurchasesRef != null && userPurchasesListener != null) {
            userPurchasesRef.removeEventListener(userPurchasesListener);
            userPurchasesListener = null;
        }
        binding = null;
    }

    private void openBookDetails(Book book) {
        Bundle args = new Bundle();
        args.putString("bookId", book.id);
        androidx.navigation.fragment.NavHostFragment.findNavController(this)
                .navigate(R.id.BookDetailFragment, args);
    }

    // --- Data Model ---
//    static class Book {
//        String title, author;
//        int coverRes;
//        String coverImageUrl;
//        Book(String title, String author, int coverRes) {
//            this.title = title;
//            this.author = author;
//            this.coverRes = coverRes;
//        }
//    }

    // --- Adapter ---
    class BookAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<BookAdapter.BookViewHolder> {
        List<Book> books;
        BookAdapter(List<Book> books) { this.books = books; }
        void updateBooks(List<Book> newBooks) {
            this.books = newBooks;
            notifyDataSetChanged();
        }
        @NonNull
        @Override
        public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemBookBinding b = ItemBookBinding.inflate(getLayoutInflater(), parent, false);
            return new BookViewHolder(b);
        }
        @Override
        public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
            Book book = books.get(position);
            holder.binding.bookTitle.setText(book.title);
            holder.binding.bookAuthor.setText(book.author);
            Glide.with(holder.binding.bookCover.getContext())
                .load(book.coverImageUrl)
                .placeholder(R.drawable.sample_book_cover)
                .into(holder.binding.bookCover);
            
            // Add click listener to open book details
            holder.itemView.setOnClickListener(v -> openBookDetails(book));
        }
        @Override
        public int getItemCount() { return books.size(); }
        class BookViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            ItemBookBinding binding;
            BookViewHolder(ItemBookBinding b) { super(b.getRoot()); binding = b; }
        }
    }
} 