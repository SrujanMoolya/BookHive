package com.svvaap.bookhive;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.svvaap.bookhive.databinding.FragmentBookReadViewBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class BookReadViewFragment extends Fragment {

    private FragmentBookReadViewBinding binding;
    private String bookId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentBookReadViewBinding.inflate(inflater, container, false);
        if (getArguments() != null) {
            bookId = getArguments().getString("bookId");
        }
        setupReaderOrError();
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void setupReaderOrError() {
        if (bookId == null || bookId.isEmpty()) {
            showNotAvailableError();
            return;
        }
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("ebooks").child(bookId);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Book book = safeMapToBook(snapshot);
                if (book == null || book.fileUrl == null || book.fileUrl.isEmpty()) {
                    showNotAvailableError();
                    return;
                }
                openInWebView(book.fileUrl);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showNotAvailableError();
            }
        });
    }

    private void openInWebView(String url) {
        WebView webView = new WebView(requireContext());
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        webView.setWebViewClient(new WebViewClient());

        String toLoad = url;
        try {
            if (url != null && url.toLowerCase(java.util.Locale.ROOT).endsWith(".pdf")) {
                String encoded = java.net.URLEncoder.encode(url, "UTF-8");
                toLoad = "https://docs.google.com/gview?embedded=1&url=" + encoded;
            }
        } catch (Exception ignored) {}

        webView.loadUrl(toLoad);
        binding.getRoot().removeAllViews();
        binding.getRoot().addView(webView,
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void showNotAvailableError() {
        binding.textviewBookReadView.setText("Book not available");
        binding.textviewBookReadView.setTextColor(0xFFE53935);
        binding.textviewBookReadView.setBackgroundColor(0xFFFFEBEE);
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