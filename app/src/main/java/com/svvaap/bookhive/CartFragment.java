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
import com.google.firebase.database.*;
import com.google.firebase.auth.FirebaseAuth;
import androidx.navigation.Navigation;

public class CartFragment extends Fragment {
    private FragmentCartBinding binding;
    private BookAdapter adapter;
    private List<Book> cartBooks;
    private DatabaseReference cartRef;
    private ValueEventListener cartListener;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCartBinding.inflate(inflater, container, false);
        setupRecyclerView();
        observeCart();
        binding.checkoutButton.setOnClickListener(v -> openCheckout());
        return binding.getRoot();
    }

    private void observeCart() {
        cartBooks = new ArrayList<>();
        adapter.update(cartBooks);
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) {
            try { Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main).navigate(R.id.LoginFragment); } catch (Exception ignored) {}
            return;
        }
        cartRef = FirebaseDatabase.getInstance().getReference("carts").child(uid);
        cartListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Check if binding is still valid (fragment view exists)
                if (binding == null) {
                    return;
                }
                
                cartBooks.clear();
                double total = 0.0;
                for (DataSnapshot snap : snapshot.getChildren()) {
                    String id = snap.getKey();
                    String title = String.valueOf(snap.child("title").getValue());
                    String author = "";
                    double price = 0.0;
                    Object priceObj = snap.child("price").getValue();
                    if (priceObj instanceof Number) price = ((Number) priceObj).doubleValue();
                    total += price;
                    int coverRes = R.drawable.sample_book_cover;
                    cartBooks.add(new Book(id, title, author, price, coverRes));
                }
                adapter.update(cartBooks);
                
                // Safe access to binding
                if (binding != null && binding.getRoot() != null) {
                    android.widget.TextView totalView = binding.getRoot().findViewById(R.id.cart_total_value);
                    if (totalView != null) totalView.setText(String.format(java.util.Locale.getDefault(), "$%.2f", total));
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        cartRef.addValueEventListener(cartListener);
    }

    private void setupRecyclerView() {
        adapter = new BookAdapter(new ArrayList<>());
        binding.cartList.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.cartList.setAdapter(adapter);
    }

    private void openCheckout() {
        androidx.navigation.fragment.NavHostFragment.findNavController(this)
                .navigate(R.id.CheckoutFragment);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove Firebase listener to prevent callbacks after view is destroyed
        if (cartRef != null && cartListener != null) {
            cartRef.removeEventListener(cartListener);
        }
        binding = null;
    }

    // --- Data Model ---
    static class Book {
        String id;
        String title, author;
        double price;
        int coverRes;
        Book(String id, String title, String author, double price, int coverRes) {
            this.id = id;
            this.title = title;
            this.author = author;
            this.price = price;
            this.coverRes = coverRes;
        }
    }

    // --- Adapter ---
    class BookAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<BookAdapter.BookViewHolder> {
        List<Book> books;
        BookAdapter(List<Book> books) { this.books = books; }
        void update(List<Book> newBooks) { this.books = newBooks; notifyDataSetChanged(); }
        @NonNull
        @Override
        public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            android.view.LayoutInflater inflater = getLayoutInflater();
            android.view.View view = inflater.inflate(R.layout.item_cart_book, parent, false);
            ItemBookBinding b = ItemBookBinding.bind(view);
            return new BookViewHolder(b);
        }
        @Override
        public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
            Book book = books.get(position);
            holder.binding.bookTitle.setText(book.title);
            holder.binding.bookAuthor.setText(book.author);
            holder.binding.bookCover.setImageResource(book.coverRes);
            android.widget.TextView priceView = holder.binding.getRoot().findViewById(R.id.book_price);
            if (priceView != null) priceView.setText(String.format(java.util.Locale.getDefault(), "$%.2f", book.price));
            android.widget.Button deleteBtn = holder.binding.getRoot().findViewById(R.id.button_delete);
            if (deleteBtn != null) deleteBtn.setOnClickListener(v -> deleteFromCart(book));
            holder.itemView.setOnClickListener(v -> openDetails(book));
        }
        @Override
        public int getItemCount() { return books.size(); }
        class BookViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            ItemBookBinding binding;
            BookViewHolder(ItemBookBinding b) { super(b.getRoot()); binding = b; }
        }
    }

    private void deleteFromCart(Book book) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null || book.id == null) return;
        FirebaseDatabase.getInstance().getReference("carts").child(uid).child(book.id).removeValue();
    }

    private void openDetails(Book book) {
        Bundle b = new Bundle();
        b.putString("bookId", book.id);
        androidx.navigation.fragment.NavHostFragment.findNavController(this).navigate(R.id.BookDetailFragment, b);
    }
} 