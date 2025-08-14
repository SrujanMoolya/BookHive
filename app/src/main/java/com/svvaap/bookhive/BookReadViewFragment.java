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
                Book book = snapshot.getValue(Book.class);
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
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl(url);
        binding.getRoot().removeAllViews();
        binding.getRoot().addView(webView,
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void showNotAvailableError() {
        binding.textviewBookReadView.setText("Book not available");
        binding.textviewBookReadView.setTextColor(0xFFE53935);
        binding.textviewBookReadView.setBackgroundColor(0xFFFFEBEE);
    }
} 