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

public class SearchFragment extends Fragment {
    private FragmentSearchBinding binding;
    private List<Book> allBooks;
    private BookAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSearchBinding.inflate(inflater, container, false);
        setupBooks();
        setupRecyclerView();
        setupSearch();
        return binding.getRoot();
    }

    private void setupBooks() {
        allBooks = new ArrayList<>();
        allBooks.add(new Book("The Great Gatsby", "F. Scott Fitzgerald", R.drawable.sample_book_cover));
        allBooks.add(new Book("To Kill a Mockingbird", "Harper Lee", R.drawable.sample_book_cover));
        allBooks.add(new Book("1984", "George Orwell", R.drawable.sample_book_cover));
        allBooks.add(new Book("Harry Potter", "J.K. Rowling", R.drawable.sample_book_cover));
        allBooks.add(new Book("The Hobbit", "J.R.R. Tolkien", R.drawable.sample_book_cover));
        allBooks.add(new Book("Project Hail Mary", "Andy Weir", R.drawable.sample_book_cover));
        allBooks.add(new Book("The Midnight Library", "Matt Haig", R.drawable.sample_book_cover));
        allBooks.add(new Book("Klara and the Sun", "Kazuo Ishiguro", R.drawable.sample_book_cover));
        allBooks.add(new Book("The Last Thing He Told Me", "Laura Dave", R.drawable.sample_book_cover));
        allBooks.add(new Book("Malibu Rising", "Taylor Jenkins Reid", R.drawable.sample_book_cover));
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
        Book(String title, String author, int coverRes) {
            this.title = title;
            this.author = author;
            this.coverRes = coverRes;
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
            holder.binding.bookCover.setImageResource(book.coverRes);
        }
        @Override
        public int getItemCount() { return books.size(); }
        class BookViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            ItemBookBinding binding;
            BookViewHolder(ItemBookBinding b) { super(b.getRoot()); binding = b; }
        }
    }
} 