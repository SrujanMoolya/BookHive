package com.svvaap.bookhive;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import androidx.navigation.Navigation;

public class BookDetailFragment extends Fragment {
    private String bookId;
    private Book book;
    private TextView titleView, authorView, descriptionView, progressView;
    private TextView categoryView, languageView, uploadDateView, priceView, purchaseStatusView;
    private ImageView coverView;
    private Button readBookButton;
    private Button addToCartButton;
    private Button buyNowButton;
    private ProgressBar readingProgressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_book_detail, container, false);
        coverView = view.findViewById(R.id.book_detail_cover);
        titleView = view.findViewById(R.id.book_detail_title);
        authorView = view.findViewById(R.id.book_detail_author);
        categoryView = view.findViewById(R.id.book_detail_category);
        languageView = view.findViewById(R.id.book_detail_language);
        uploadDateView = view.findViewById(R.id.book_detail_upload_date);
        priceView = view.findViewById(R.id.book_detail_price);
        descriptionView = view.findViewById(R.id.book_detail_description);
        progressView = view.findViewById(R.id.book_detail_progress);
        purchaseStatusView = view.findViewById(R.id.book_detail_purchase_status);
        readBookButton = view.findViewById(R.id.read_book_button);
        addToCartButton = view.findViewById(R.id.add_to_cart_button);
        buyNowButton = view.findViewById(R.id.buy_now_button);
        readingProgressBar = view.findViewById(R.id.reading_progress_bar);

        if (getArguments() != null) {
            bookId = getArguments().getString("bookId");
            if (bookId != null) {
                fetchBookDetails();
            } else {
                // Optionally show an error message or handle gracefully
                titleView.setText("Error: Book ID not found");
                authorView.setText("");
                descriptionView.setText("");
                progressView.setText("");
                readingProgressBar.setProgress(0);
            }
        }

        readBookButton.setOnClickListener(v -> openBookReader());
        addToCartButton.setOnClickListener(v -> addToCart());
        buyNowButton.setOnClickListener(v -> startCheckout());
        return view;
    }

    private void fetchBookDetails() {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("ebooks").child(bookId);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                book = safeMapToBook(snapshot);
                if (book != null) {
                    titleView.setText(book.title);
                    authorView.setText(book.author);
                    descriptionView.setText(book.description);
                    categoryView.setText(book.category != null ? book.category : "");
                    languageView.setText(book.language != null ? book.language : "");
                    uploadDateView.setText(book.uploadDate != null ? ("Uploaded: " + book.uploadDate) : "");
                    priceView.setText(String.format(java.util.Locale.getDefault(), "Price: $%.2f", book.price));
                    Glide.with(coverView.getContext())
                        .load(book.coverImageUrl)
                        .placeholder(R.drawable.sample_book_cover)
                        .into(coverView);
                    // For demo, set progress to 0
                    progressView.setText("Progress: 0%");
                    readingProgressBar.setProgress(0);
                    updatePurchaseStatus();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updatePurchaseStatus() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            purchaseStatusView.setText("Not purchased (login required)");
            purchaseStatusView.setTextColor(0xFFE53935);
            toggleActionsForPurchased(false);
            return;
        }
        DatabaseReference purchasesRef = FirebaseDatabase.getInstance().getReference("purchases")
            .child(user.getUid()).child(bookId == null ? "" : bookId);
        purchasesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean purchased = snapshot.exists();
                if (purchased) {
                    purchaseStatusView.setText("Purchased");
                    purchaseStatusView.setTextColor(0xFF388E3C);
                } else {
                    purchaseStatusView.setText("Not purchased");
                    purchaseStatusView.setTextColor(0xFFE53935);
                }
                toggleActionsForPurchased(purchased);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void toggleActionsForPurchased(boolean purchased) {
        readBookButton.setVisibility(purchased ? View.VISIBLE : View.GONE);
        addToCartButton.setVisibility(purchased ? View.GONE : View.VISIBLE);
        buyNowButton.setVisibility(purchased ? View.GONE : View.VISIBLE);
    }

    private void addToCart() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || bookId == null) {
            android.widget.Toast.makeText(getContext(), "Login required to add to cart", android.widget.Toast.LENGTH_SHORT).show();
            // close this overlay and open Login screen
            try { requireActivity().getSupportFragmentManager().popBackStack(); } catch (Exception ignored) {}
            try { Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main).navigate(R.id.LoginFragment); } catch (Exception ignored) {}
            return;
        }
        DatabaseReference cartRef = FirebaseDatabase.getInstance().getReference("carts")
                .child(user.getUid()).child(bookId);
        java.util.Map<String, Object> item = new java.util.HashMap<>();
        item.put("title", book != null ? book.title : "");
        item.put("price", book != null ? book.price : 0);
        cartRef.setValue(item).addOnCompleteListener(t -> {
            android.widget.Toast.makeText(getContext(), t.isSuccessful() ? "Added to cart" : "Failed to add to cart", android.widget.Toast.LENGTH_SHORT).show();
        });
    }

    private void startCheckout() {
        // Navigate to checkout for single-book immediate purchase
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            android.widget.Toast.makeText(getContext(), "Login required", android.widget.Toast.LENGTH_SHORT).show();
            try { requireActivity().getSupportFragmentManager().popBackStack(); } catch (Exception ignored) {}
            try { Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main).navigate(R.id.LoginFragment); } catch (Exception ignored) {}
            return;
        }
        Bundle args = new Bundle();
        args.putString("singleBookId", bookId);
        args.putDouble("singleAmount", book != null ? book.price : 0);
        androidx.navigation.fragment.NavHostFragment.findNavController(this)
                .navigate(R.id.CheckoutFragment, args);
    }

    private void openBookReader() {
        Bundle bundle = new Bundle();
        bundle.putString("bookId", bookId);
        BookReadViewFragment fragment = new BookReadViewFragment();
        fragment.setArguments(bundle);
        androidx.navigation.fragment.NavHostFragment.findNavController(this)
            .navigate(R.id.BookReadViewFragment, bundle);
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
} 