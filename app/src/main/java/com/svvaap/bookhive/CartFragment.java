package com.svvaap.bookhive;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.svvaap.bookhive.databinding.FragmentCartBinding;
import com.svvaap.bookhive.databinding.ItemCartBookBinding;
import java.util.ArrayList;
import java.util.List;
import com.google.firebase.database.*;
import com.google.firebase.auth.FirebaseAuth;

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

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (uid == null) {
            try {
                androidx.navigation.Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main)
                        .navigate(R.id.LoginFragment);
            } catch (Exception ignored) {}
            return;
        }

        cartRef = FirebaseDatabase.getInstance().getReference("carts").child(uid);
        cartListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (binding == null) return;

                cartBooks.clear();
                double total = 0.0;

                for (DataSnapshot snap : snapshot.getChildren()) {
                    String id = snap.getKey();
                    String title = String.valueOf(snap.child("title").getValue());
                    String author = String.valueOf(snap.child("author").getValue());
                    String coverImageUrl = String.valueOf(snap.child("coverImageUrl").getValue());

                    double price = 0.0;
                    Object priceObj = snap.child("price").getValue();
                    if (priceObj instanceof Number) price = ((Number) priceObj).doubleValue();

                    total += price;

                    cartBooks.add(new Book(id, title, author, price, coverImageUrl));
                }

                adapter.update(cartBooks);

                if (binding != null && binding.getRoot() != null) {
                    android.widget.TextView totalView = binding.getRoot().findViewById(R.id.cart_total_value);
                    if (totalView != null) {
                        totalView.setText(String.format(java.util.Locale.getDefault(), "₹%.2f", total));
                    }
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
        if (cartRef != null && cartListener != null) {
            cartRef.removeEventListener(cartListener);
        }
        binding = null;
    }

    // --- Data Model ---
    static class Book {
        String id;
        String title, author, coverImageUrl;
        double price;

        Book(String id, String title, String author, double price, String coverImageUrl) {
            this.id = id;
            this.title = title;
            this.author = author;
            this.price = price;
            this.coverImageUrl = coverImageUrl;
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
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            ItemCartBookBinding b = ItemCartBookBinding.inflate(inflater, parent, false);
            return new BookViewHolder(b);
        }

        @Override
        public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
            Book book = books.get(position);

            holder.binding.bookTitle.setText(book.title);
            holder.binding.bookAuthor.setText(book.author);


            // ✅ Load image with Glide
            if (book.coverImageUrl != null && !book.coverImageUrl.equals("null")) {
                Glide.with(holder.binding.bookCover.getContext())
                        .load(book.coverImageUrl)
                        .placeholder(R.drawable.sample_book_cover)
                        .error(R.drawable.sample_book_cover)
                        .into(holder.binding.bookCover);

                Log.d("CoverURL", "Loading: " + book.coverImageUrl);
            } else {
                holder.binding.bookCover.setImageResource(R.drawable.sample_book_cover);
            }

            holder.binding.bookPrice.setText(
                    String.format(java.util.Locale.getDefault(), "₹%.2f", book.price)
            );

            holder.binding.buttonDelete.setOnClickListener(v -> deleteFromCart(book));
            holder.itemView.setOnClickListener(v -> openDetails(book));
        }

        @Override
        public int getItemCount() { return books.size(); }

        class BookViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            ItemCartBookBinding binding;
            BookViewHolder(ItemCartBookBinding b) { super(b.getRoot()); binding = b; }
        }
    }

    private void deleteFromCart(Book book) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
        if (uid == null || book.id == null) return;
        FirebaseDatabase.getInstance().getReference("carts").child(uid).child(book.id).removeValue();
    }

    private void openDetails(Book book) {
        Bundle b = new Bundle();
        b.putString("bookId", book.id);
        androidx.navigation.fragment.NavHostFragment.findNavController(this)
                .navigate(R.id.BookDetailFragment, b);
    }
}