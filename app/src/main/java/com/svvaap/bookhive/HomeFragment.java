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
import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;
import android.view.inputmethod.EditorInfo;
import android.view.KeyEvent;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private final List<Book> allLatestBooks = new ArrayList<>();
    private final List<Book> latestBooks = new ArrayList<>();
    private BookAdapter latestAdapter;
    private DatabaseReference ebooksRef;
    private ValueEventListener ebooksListener;
    private String selectedCategory = "All";
    private int selectedCategoryPosition = 0;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        setupCategoryList();
        setupSearchBar();
        setupLatestList();
        fetchLatestBooks();
        return binding.getRoot();
    }

    private void setupCategoryList() {
        List<Category> categories = new ArrayList<>();
        categories.add(new Category("All", getCategoryIconRes("All")));
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
            case "all":
                return R.drawable.ic_category_generic;
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

    private void setupSearchBar() {
        if (binding.searchBar != null) {
            binding.searchBar.setSingleLine(true);
            binding.searchBar.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
            binding.searchBar.setOnEditorActionListener((v, actionId, event) -> {
                boolean isEnter = event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN;
                if (actionId == EditorInfo.IME_ACTION_SEARCH || isEnter) {
                    String query = v.getText() != null ? v.getText().toString().trim() : "";
                    Bundle bundle = new Bundle();
                    bundle.putString("initialQuery", query);
                    androidx.navigation.fragment.NavHostFragment.findNavController(this)
                            .navigate(R.id.SearchFragment, bundle);
                    return true;
                }
                return false;
            });
        }
    }

    private void setupLatestList() {
        latestAdapter = new BookAdapter(new ArrayList<>(latestBooks));
        binding.latestList.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(getContext(), 3));
        binding.latestList.setAdapter(latestAdapter);
    }

    private void fetchLatestBooks() {
        ebooksRef = FirebaseDatabase.getInstance().getReference("ebooks");
        ebooksListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (binding == null) return;
                allLatestBooks.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Book book = safeMapToBook(snap);
                    if (book != null) { book.id = snap.getKey(); allLatestBooks.add(book); }
                }
                // Sort newest first by uploadDate (ISO string lex order works; nulls last)
                java.util.Collections.sort(allLatestBooks, (a, b) -> compareUploadDateDesc(a.uploadDate, b.uploadDate));
                applyCategoryFilter();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        ebooksRef.addValueEventListener(ebooksListener);
    }

    private void applyCategoryFilter() {
        latestBooks.clear();
        if (selectedCategory == null || selectedCategory.equalsIgnoreCase("All")) {
            latestBooks.addAll(allLatestBooks);
        } else {
            for (Book b : allLatestBooks) {
                if (b.category != null && b.category.equalsIgnoreCase(selectedCategory)) {
                    latestBooks.add(b);
                }
            }
        }
        latestAdapter.updateBooks(new ArrayList<>(latestBooks));
    }

    private int compareUploadDateDesc(String d1, String d2) {
        if (d1 == null && d2 == null) return 0;
        if (d1 == null) return 1;
        if (d2 == null) return -1;
        return -d1.compareTo(d2);
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
        try {
            Object priceObj = map.get("price");
            if (priceObj instanceof Number) b.price = ((Number) priceObj).doubleValue();
            else if (priceObj instanceof String) b.price = Double.parseDouble((String) priceObj);
        } catch (Exception ignored) {}
        return b;
    }

    private String asString(Object v) { return v == null ? null : String.valueOf(v); }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (ebooksRef != null && ebooksListener != null) {
            ebooksRef.removeEventListener(ebooksListener);
            ebooksListener = null;
        }
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
            // Highlight selected category
            boolean isSelected = position == selectedCategoryPosition;
            holder.binding.categoryIcon.setBackgroundResource(isSelected ? R.drawable.bg_category_selected : 0);
            holder.itemView.setOnClickListener(v -> selectCategory(c.name, position));
        }
        @Override
        public int getItemCount() { return categories.size(); }
        class CategoryViewHolder extends RecyclerView.ViewHolder {
            ItemCategoryBinding binding;
            CategoryViewHolder(ItemCategoryBinding b) { super(b.getRoot()); binding = b; }
        }
    }

    private void selectCategory(String category, int position) {
        selectedCategory = category;
        selectedCategoryPosition = position;
        applyCategoryFilter();
        // Refresh category chips
        binding.categoryList.getAdapter().notifyDataSetChanged();
    }

    class BookAdapter extends RecyclerView.Adapter<BookAdapter.BookViewHolder> {
        List<Book> books;
        BookAdapter(List<Book> books) { this.books = books; }
        void updateBooks(List<Book> newBooks) { this.books = newBooks; notifyDataSetChanged(); }
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
                if (book.id != null) {
                    Bundle bundle = new Bundle();
                    bundle.putString("bookId", book.id);
                    androidx.navigation.fragment.NavHostFragment.findNavController(HomeFragment.this)
                            .navigate(R.id.BookDetailFragment, bundle);
                }
            });
        }
        @Override
        public int getItemCount() { return books.size(); }
        class BookViewHolder extends RecyclerView.ViewHolder {
            ItemBookBinding binding;
            BookViewHolder(ItemBookBinding b) { super(b.getRoot()); binding = b; }
        }
    }
}
