package com.example.bookhive;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.fragment.app.Fragment;
import com.example.bookhive.databinding.FragmentHomeBinding;
import com.example.bookhive.databinding.ItemCategoryBinding;
import com.example.bookhive.databinding.ItemBookBinding;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        setupCategoryList();
        setupBestsellerList();
        setupNewArrivalsList();
        return binding.getRoot();
    }

    private void setupCategoryList() {
        List<Category> categories = new ArrayList<>();
        categories.add(new Category("Fiction", R.drawable.sample_category));
        categories.add(new Category("Non-Fiction", R.drawable.sample_category));
        categories.add(new Category("Comics", R.drawable.sample_category));
        categories.add(new Category("Biographies", R.drawable.sample_category));
        categories.add(new Category("Children", R.drawable.sample_category));
        categories.add(new Category("Science", R.drawable.sample_category));
        categories.add(new Category("Romance", R.drawable.sample_category));
        binding.categoryList.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.categoryList.setAdapter(new CategoryAdapter(categories));
    }

    private void setupBestsellerList() {
        List<Book> books = new ArrayList<>();
        books.add(new Book("The Great Gatsby", "F. Scott Fitzgerald", R.drawable.sample_book_cover));
        books.add(new Book("To Kill a Mockingbird", "Harper Lee", R.drawable.sample_book_cover));
        books.add(new Book("1984", "George Orwell", R.drawable.sample_book_cover));
        books.add(new Book("Harry Potter", "J.K. Rowling", R.drawable.sample_book_cover));
        books.add(new Book("The Hobbit", "J.R.R. Tolkien", R.drawable.sample_book_cover));
        binding.bestsellerList.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.bestsellerList.setAdapter(new BookAdapter(books));
    }

    private void setupNewArrivalsList() {
        List<Book> books = new ArrayList<>();
        books.add(new Book("Project Hail Mary", "Andy Weir", R.drawable.sample_book_cover));
        books.add(new Book("The Midnight Library", "Matt Haig", R.drawable.sample_book_cover));
        books.add(new Book("Klara and the Sun", "Kazuo Ishiguro", R.drawable.sample_book_cover));
        books.add(new Book("The Last Thing He Told Me", "Laura Dave", R.drawable.sample_book_cover));
        books.add(new Book("Malibu Rising", "Taylor Jenkins Reid", R.drawable.sample_book_cover));
        binding.newArrivalsList.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.newArrivalsList.setAdapter(new BookAdapter(books));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // --- Data Models ---
    static class Category {
        String name;
        int iconRes;
        Category(String name, int iconRes) {
            this.name = name;
            this.iconRes = iconRes;
        }
    }
    static class Book {
        String title, author;
        int coverRes;
        Book(String title, String author, int coverRes) {
            this.title = title;
            this.author = author;
            this.coverRes = coverRes;
        }
    }

    // --- Adapters ---
    class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {
        List<Category> categories;
        CategoryAdapter(List<Category> categories) { this.categories = categories; }
        @NonNull
        @Override
        public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemCategoryBinding b = ItemCategoryBinding.inflate(getLayoutInflater(), parent, false);
            return new CategoryViewHolder(b);
        }
        @Override
        public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
            Category c = categories.get(position);
            holder.binding.categoryLabel.setText(c.name);
            holder.binding.categoryIcon.setImageResource(c.iconRes);
        }
        @Override
        public int getItemCount() { return categories.size(); }
        class CategoryViewHolder extends RecyclerView.ViewHolder {
            ItemCategoryBinding binding;
            CategoryViewHolder(ItemCategoryBinding b) { super(b.getRoot()); binding = b; }
        }
    }

    class BookAdapter extends RecyclerView.Adapter<BookAdapter.BookViewHolder> {
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
        class BookViewHolder extends RecyclerView.ViewHolder {
            ItemBookBinding binding;
            BookViewHolder(ItemBookBinding b) { super(b.getRoot()); binding = b; }
        }
    }
} 