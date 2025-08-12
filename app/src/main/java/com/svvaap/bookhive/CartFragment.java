package com.svvaap.bookhive;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.svvaap.bookhive.databinding.FragmentCartBinding;
import com.svvaap.bookhive.databinding.ItemBookBinding;
import java.util.ArrayList;
import java.util.List;

public class CartFragment extends Fragment {
    private FragmentCartBinding binding;
    private BookAdapter adapter;
    private List<Book> cartBooks;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCartBinding.inflate(inflater, container, false);
        setupCart();
        setupRecyclerView();
        binding.checkoutButton.setOnClickListener(v -> {
            // TODO: Implement checkout logic
        });
        return binding.getRoot();
    }

    private void setupCart() {
        cartBooks = new ArrayList<>();
        cartBooks.add(new Book("The Great Gatsby", "F. Scott Fitzgerald", R.drawable.sample_book_cover));
        cartBooks.add(new Book("1984", "George Orwell", R.drawable.sample_book_cover));
        cartBooks.add(new Book("Harry Potter", "J.K. Rowling", R.drawable.sample_book_cover));
    }

    private void setupRecyclerView() {
        adapter = new BookAdapter(cartBooks);
        binding.cartList.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.cartList.setAdapter(adapter);
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