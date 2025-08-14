package com.svvaap.bookhive;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.fragment.app.Fragment;
import com.svvaap.bookhive.databinding.FragmentHomeBinding;
import com.svvaap.bookhive.databinding.ItemCategoryBinding;
import com.svvaap.bookhive.databinding.ItemBookBinding;
import com.google.firebase.database.*;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private List<Book> allBooks = new ArrayList<>();
    private BookAdapter bestsellerAdapter;
    private BookAdapter newArrivalsAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        setupCategoryList();
        setupBookLists();
        fetchBooksFromFirebase();
        return binding.getRoot();
    }

    private void setupCategoryList() {
        List<Category> categories = new ArrayList<>();
        categories.add(new Category("Self-help", getCategoryIconRes("Self-help")));
        categories.add(new Category("Fiction", getCategoryIconRes("Fiction")));
        categories.add(new Category("Non-fiction", getCategoryIconRes("Non-fiction")));
        categories.add(new Category("Science", getCategoryIconRes("Science")));
        categories.add(new Category("Biography", getCategoryIconRes("Biography")));
        categories.add(new Category("Other", getCategoryIconRes("Other")));
        binding.categoryList.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.categoryList.setAdapter(new CategoryAdapter(categories));
    }

    private int getCategoryIconRes(String name) {
        if (name == null) return R.drawable.ic_category_generic;
        String key = name.trim().toLowerCase();
        switch (key) {
            case "self-help":
            case "selfhelp":
                return R.drawable.ic_category_self_help;
            case "fiction":
                return R.drawable.ic_category_fiction;
            case "non-fiction":
            case "nonfiction":
                return R.drawable.ic_category_nonfiction;
            case "comics":
                return R.drawable.ic_category_comics;
            case "biographies":
            case "biography":
                return R.drawable.ic_category_biographies;
            case "children":
            case "kids":
                return R.drawable.ic_category_children;
            case "science":
                return R.drawable.ic_category_science;
            case "romance":
                return R.drawable.ic_category_romance;
            case "other":
                return R.drawable.ic_category_other;
            default:
                return R.drawable.ic_category_generic;
        }
    }

    private void setupBookLists() {
        bestsellerAdapter = new BookAdapter(new ArrayList<>(), this::openBookDetail);
        newArrivalsAdapter = new BookAdapter(new ArrayList<>(), this::openBookDetail);
        binding.bestsellerList.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.bestsellerList.setAdapter(bestsellerAdapter);
        binding.newArrivalsList.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.newArrivalsList.setAdapter(newArrivalsAdapter);
    }

    private void fetchBooksFromFirebase() {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("ebooks");
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allBooks.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Book book = safeMapToBook(snap);
                    if (book != null) {
                        book.id = snap.getKey();
                        allBooks.add(book);
                    }
                }
                // For demo: bestsellers = first 5, new arrivals = last 5
                List<Book> bestsellers = new ArrayList<>();
                List<Book> newArrivals = new ArrayList<>();
                for (int i = 0; i < allBooks.size(); i++) {
                    if (i < 5) bestsellers.add(allBooks.get(i));
                    if (i >= allBooks.size() - 5) newArrivals.add(allBooks.get(i));
                }
                bestsellerAdapter.updateBooks(bestsellers);
                newArrivalsAdapter.updateBooks(newArrivals);
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

    private void openBookDetail(Book book) {
        Bundle bundle = new Bundle();
        bundle.putString("bookId", book.id);
        androidx.navigation.fragment.NavHostFragment.findNavController(this)
            .navigate(R.id.BookDetailFragment, bundle);
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
            holder.itemView.setOnClickListener(v -> openCategory(c.name));
        }
        @Override
        public int getItemCount() { return categories.size(); }
        class CategoryViewHolder extends RecyclerView.ViewHolder {
            ItemCategoryBinding binding;
            CategoryViewHolder(ItemCategoryBinding b) { super(b.getRoot()); binding = b; }
        }
    }

    private void openCategory(String category) {
        Bundle bundle = new Bundle();
        bundle.putString("categoryFilter", category);
        androidx.navigation.fragment.NavHostFragment.findNavController(this)
                .navigate(R.id.CatalogueFragment, bundle);
    }

    interface OnBookClickListener {
        void onBookClick(Book book);
    }

    class BookAdapter extends RecyclerView.Adapter<BookAdapter.BookViewHolder> {
        List<Book> books;
        OnBookClickListener listener;
        BookAdapter(List<Book> books, OnBookClickListener listener) {
            this.books = books;
            this.listener = listener;
        }

        void updateBooks(List<Book> newBooks) {
            this.books = newBooks;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemBookBinding b = ItemBookBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
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

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onBookClick(book);
                }
            });
        }

        @Override
        public int getItemCount() {
            return books.size();
        }

        class BookViewHolder extends RecyclerView.ViewHolder {
            ItemBookBinding binding;
            BookViewHolder(ItemBookBinding b) {
                super(b.getRoot());
                binding = b;
            }
        }
    }
}
