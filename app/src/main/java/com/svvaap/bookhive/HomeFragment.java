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
                    Book book = snap.getValue(Book.class);
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

    private void openBookDetail(Book book) {
        Bundle bundle = new Bundle();
        bundle.putString("bookId", book.id);
        BookDetailFragment fragment = new BookDetailFragment();
        fragment.setArguments(bundle);
        requireActivity().getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit();
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
        }
        @Override
        public int getItemCount() { return categories.size(); }
        class CategoryViewHolder extends RecyclerView.ViewHolder {
            ItemCategoryBinding binding;
            CategoryViewHolder(ItemCategoryBinding b) { super(b.getRoot()); binding = b; }
        }
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
