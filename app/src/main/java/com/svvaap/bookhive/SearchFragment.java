package com.svvaap.bookhive;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.svvaap.bookhive.databinding.FragmentSearchBinding;
import com.svvaap.bookhive.databinding.ItemBookBinding;
import java.util.ArrayList;
import java.util.List;
import com.google.firebase.database.*;
import com.bumptech.glide.Glide;
import androidx.navigation.Navigation;

public class SearchFragment extends Fragment {
    private FragmentSearchBinding binding;
    private List<com.svvaap.bookhive.Book> allBooks = new ArrayList<>();
    private BookAdapter adapter;
    private String initialQuery;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSearchBinding.inflate(inflater, container, false);
        if (getArguments() != null) {
            initialQuery = getArguments().getString("initialQuery", null);
        }
        setupRecyclerView();
        fetchBooksFromFirebase();
        setupSearch();
        if (initialQuery != null && !initialQuery.isEmpty()) {
            binding.searchInput.setText(initialQuery);
            binding.searchInput.setSelection(binding.searchInput.getText().length());
        }
        return binding.getRoot();
    }

    private void fetchBooksFromFirebase() {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("ebooks");
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allBooks.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Book book = safeMapToBook(snap);
                    if (book != null) { book.id = snap.getKey(); allBooks.add(book); }
                }
                adapter.updateBooks(new ArrayList<>(allBooks));
                String current = binding.searchInput.getText() != null ? binding.searchInput.getText().toString() : "";
                if (current.isEmpty()) {
                    binding.noResultsText.setVisibility(allBooks.isEmpty() ? View.VISIBLE : View.GONE);
                } else {
                    filterBooks(current);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
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

    private void setupRecyclerView() {
        adapter = new BookAdapter(new ArrayList<>(allBooks));
        binding.searchResultsList.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.searchResultsList.setAdapter(adapter);
        binding.noResultsText.setVisibility(View.GONE);
    }

    private void setupSearch() {
        binding.searchInput.addTextChangedListener(new TextWatcher() {
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
        List<com.svvaap.bookhive.Book> filtered = new ArrayList<>();
        for (com.svvaap.bookhive.Book book : allBooks) {
            String title = book.title != null ? book.title : "";
            String author = book.author != null ? book.author : "";
            if (title.toLowerCase().contains(query.toLowerCase()) ||
                author.toLowerCase().contains(query.toLowerCase())) {
                filtered.add(book);
            }
        }
        adapter.updateBooks(filtered);
        binding.noResultsText.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // --- Adapter ---
    class BookAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<BookAdapter.BookViewHolder> {
        List<com.svvaap.bookhive.Book> books;
        BookAdapter(List<com.svvaap.bookhive.Book> books) { this.books = books; }
        void updateBooks(List<com.svvaap.bookhive.Book> newBooks) {
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
            com.svvaap.bookhive.Book book = books.get(position);
            holder.binding.bookTitle.setText(book.title);
            holder.binding.bookAuthor.setText(book.author);
            Glide.with(holder.binding.bookCover.getContext())
                .load(book.coverImageUrl)
                .placeholder(R.drawable.sample_book_cover)
                .into(holder.binding.bookCover);
            holder.itemView.setOnClickListener(v -> {
                if (book.id != null) {
                    Bundle bundle = new Bundle();
                    bundle.putString("bookId", book.id);
                    Navigation.findNavController(v).navigate(R.id.BookDetailFragment, bundle);
                }
            });
        }
        @Override
        public int getItemCount() { return books.size(); }
        class BookViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            ItemBookBinding binding;
            BookViewHolder(ItemBookBinding b) { super(b.getRoot()); binding = b; }
        }
    }
} 