package com.example.bookhive;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.bookhive.databinding.FragmentSearchBinding;
import com.example.bookhive.databinding.ItemBookBinding;
import java.util.ArrayList;
import java.util.List;
import com.google.firebase.database.*;
import com.bumptech.glide.Glide;

public class SearchFragment extends Fragment {
    private FragmentSearchBinding binding;
    private List<Book> allBooks = new ArrayList<>();
    private BookAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSearchBinding.inflate(inflater, container, false);
        setupRecyclerView();
        fetchBooksFromFirebase();
        setupSearch();
        return binding.getRoot();
    }

    private void fetchBooksFromFirebase() {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("ebooks");
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allBooks.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Book book = snap.getValue(Book.class);
                    if (book != null) allBooks.add(book);
                }
                adapter.updateBooks(new ArrayList<>(allBooks));
                binding.noResultsText.setVisibility(allBooks.isEmpty() ? View.VISIBLE : View.GONE);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
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
        List<Book> filtered = new ArrayList<>();
        for (Book book : allBooks) {
            if (book.title.toLowerCase().contains(query.toLowerCase()) ||
                book.author.toLowerCase().contains(query.toLowerCase())) {
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

    // --- Data Model ---
    static class Book {
        String title, author;
        int coverRes;
        String coverImageUrl;
        Book(String title, String author, int coverRes, String coverImageUrl) {
            this.title = title;
            this.author = author;
            this.coverRes = coverRes;
            this.coverImageUrl = coverImageUrl;
        }
    }

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
        }
        @Override
        public int getItemCount() { return books.size(); }
        class BookViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            ItemBookBinding binding;
            BookViewHolder(ItemBookBinding b) { super(b.getRoot()); binding = b; }
        }
    }
} 