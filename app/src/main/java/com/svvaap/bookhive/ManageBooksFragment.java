package com.svvaap.bookhive;

import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ManageBooksFragment extends Fragment implements BooksManageAdapter.BookActionListener {
    private RecyclerView recyclerBooks;
    private BooksManageAdapter adapter;
    private List<Book> bookList = new ArrayList<>();
    private DatabaseReference dbRef;
    private Uri editSelectedCoverUri;
    private Uri editSelectedPdfUri;

    private final ActivityResultLauncher<String> editPickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> editSelectedCoverUri = uri
    );

    private final ActivityResultLauncher<String> editPickPdfLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> editSelectedPdfUri = uri
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manage_books, container, false);
        recyclerBooks = view.findViewById(R.id.recycler_books);
        recyclerBooks.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new BooksManageAdapter(bookList, this);
        recyclerBooks.setAdapter(adapter);
        dbRef = FirebaseDatabase.getInstance().getReference("ebooks");
        fetchBooks();
        return view;
    }

    private void fetchBooks() {
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                bookList.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Book book = snap.getValue(Book.class);
                    if (book != null) {
                        book.id = snap.getKey();
                        bookList.add(book);
                    }
                }
                adapter.setBooks(bookList);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load books", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onEdit(Book book) {
        showEditDialog(book);
    }

    @Override
    public void onDelete(Book book) {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Book")
                .setMessage("Are you sure you want to delete this book?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    dbRef.child(book.id).removeValue().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(getContext(), "Book deleted", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "Failed to delete", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditDialog(Book book) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.fragment_upload_book, null, false);
        EditText inputTitle = dialogView.findViewById(R.id.input_title);
        EditText inputAuthor = dialogView.findViewById(R.id.input_author);
        EditText inputDescription = dialogView.findViewById(R.id.input_description);
        EditText inputPrice = dialogView.findViewById(R.id.input_price);
        Button buttonSelectCover = dialogView.findViewById(R.id.button_select_cover);
        Button buttonSelectPdf = dialogView.findViewById(R.id.button_select_pdf);
        TextView textCoverStatus = dialogView.findViewById(R.id.text_cover_status);
        TextView textPdfStatus = dialogView.findViewById(R.id.text_pdf_status);
        Spinner spinnerCategory = dialogView.findViewById(R.id.spinner_category);
        Spinner spinnerLanguage = dialogView.findViewById(R.id.spinner_language);
        Spinner spinnerVisibility = dialogView.findViewById(R.id.spinner_visibility);

        // Set current values
        inputTitle.setText(book.title);
        inputAuthor.setText(book.author);
        inputDescription.setText(book.description);
        inputPrice.setText(String.valueOf(book.price));
        textCoverStatus.setText(book.coverImageUrl != null ? book.coverImageUrl : "No file selected");
        textPdfStatus.setText(book.fileUrl != null ? book.fileUrl : "No file selected");

        buttonSelectCover.setOnClickListener(v -> editPickImageLauncher.launch("image/*"));
        buttonSelectPdf.setOnClickListener(v -> editPickPdfLauncher.launch("application/pdf"));

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, new String[]{"Self-help", "Fiction", "Non-fiction", "Science", "Biography", "Other"});
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);
        int catPos = categoryAdapter.getPosition(book.category);
        spinnerCategory.setSelection(catPos >= 0 ? catPos : 0);

        ArrayAdapter<String> languageAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, new String[]{"English", "Hindi", "French", "German", "Other"});
        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLanguage.setAdapter(languageAdapter);
        int langPos = languageAdapter.getPosition(book.language);
        spinnerLanguage.setSelection(langPos >= 0 ? langPos : 0);

        ArrayAdapter<String> visibilityAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, new String[]{"public", "private"});
        visibilityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerVisibility.setAdapter(visibilityAdapter);
        int visPos = visibilityAdapter.getPosition(book.visibility);
        spinnerVisibility.setSelection(visPos >= 0 ? visPos : 0);

        new AlertDialog.Builder(getContext())
                .setTitle("Edit Book")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    final String title = inputTitle.getText().toString().trim();
                    final String author = inputAuthor.getText().toString().trim();
                    final String description = inputDescription.getText().toString().trim();
                    final String priceStr = inputPrice.getText().toString().trim();
                    final String category = spinnerCategory.getSelectedItem().toString();
                    final String language = spinnerLanguage.getSelectedItem().toString();
                    final String visibility = spinnerVisibility.getSelectedItem().toString();
                    final double price = Double.parseDouble(priceStr);
                    if (TextUtils.isEmpty(title) || TextUtils.isEmpty(author)) {
                        Toast.makeText(getContext(), "Please fill required fields", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // Perform optional uploads if new files were selected; otherwise keep existing URLs
                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    Handler main = new Handler(Looper.getMainLooper());
                    executor.execute(() -> {
                        String coverUrl = book.coverImageUrl;
                        String fileUrl = book.fileUrl;
                        try {
                            if (editSelectedCoverUri != null) {
                                coverUrl = uploadToCloudinary(editSelectedCoverUri, true);
                            }
                            if (editSelectedPdfUri != null) {
                                fileUrl = uploadToCloudinary(editSelectedPdfUri, false);
                            }
                        } catch (Exception e) {
                            String msg = "Upload failed: " + e.getMessage();
                            main.post(() -> Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show());
                            return;
                        }

                        Map<String, Object> update = new HashMap<>();
                        update.put("title", title);
                        update.put("author", author);
                        update.put("description", description);
                        update.put("price", price);
                        update.put("coverImageUrl", coverUrl);
                        update.put("fileUrl", fileUrl);
                        update.put("category", category);
                        update.put("language", language);
                        update.put("visibility", visibility);
                        dbRef.child(book.id).updateChildren(update).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(getContext(), "Book updated", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getContext(), "Failed to update", Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String uploadToCloudinary(Uri uri, boolean isImage) throws Exception {
        if (uri == null) throw new IllegalArgumentException("No file selected");
        String cloudName = BuildConfig.CLOUDINARY_CLOUD_NAME;
        String unsignedPreset = BuildConfig.CLOUDINARY_UNSIGNED_PRESET;
        if (TextUtils.isEmpty(cloudName) || TextUtils.isEmpty(unsignedPreset)) {
            throw new IllegalStateException("Cloudinary not configured");
        }

        String uploadUrl = "https://api.cloudinary.com/v1_1/" + cloudName + "/" + (isImage ? "image" : "raw") + "/upload";
        java.io.InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
        if (inputStream == null) throw new IllegalStateException("Cannot open file");

        MediaType mediaType = MediaType.parse(isImage ? "image/*" : "application/pdf");
        RequestBody fileBody = RequestBody.create(readAllBytes(inputStream), mediaType);

        MultipartBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("upload_preset", unsignedPreset)
                .addFormDataPart("file", "upload" + (isImage ? ".jpg" : ".pdf"), fileBody)
                .build();

        Request request = new Request.Builder()
                .url(uploadUrl)
                .post(requestBody)
                .build();

        OkHttpClient client = new OkHttpClient();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IllegalStateException("Upload failed: " + response.code());
            String body = response.body() != null ? response.body().string() : null;
            if (body == null) throw new IllegalStateException("Empty response");
            JSONObject json = new JSONObject(body);
            return json.optString("secure_url", json.optString("url"));
        }
    }

    private byte[] readAllBytes(java.io.InputStream is) throws java.io.IOException {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int nRead;
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }
} 