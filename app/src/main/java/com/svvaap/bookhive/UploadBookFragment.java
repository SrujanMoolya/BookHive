package com.svvaap.bookhive;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.*;

public class UploadBookFragment extends Fragment {
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PICK_FILE_REQUEST = 2;

    private TextInputEditText inputTitle, inputAuthor, inputDescription, inputPrice, inputCoverUrl, inputFileUrl;
    private Spinner spinnerCategory, spinnerLanguage, spinnerVisibility;
    private TextView textUploadDate;
    private Button buttonUploadBook, buttonPickDate;
    private Button buttonSelectCover, buttonSelectPdf;
    private TextView textCoverStatus, textPdfStatus;
    private String uploadDateISO;

    private Uri selectedCoverUri;
    private Uri selectedPdfUri;

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedCoverUri = uri;
                    textCoverStatus.setText("Selected: " + uri.getLastPathSegment());
                }
            }
    );

    private final ActivityResultLauncher<String> pickPdfLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedPdfUri = uri;
                    textPdfStatus.setText("Selected: " + uri.getLastPathSegment());
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_upload_book, container, false);
        inputTitle = view.findViewById(R.id.input_title);
        inputAuthor = view.findViewById(R.id.input_author);
        inputDescription = view.findViewById(R.id.input_description);
        inputPrice = view.findViewById(R.id.input_price);
        // URL fields kept for backwards-compat if needed but hidden usage
        inputCoverUrl = null;
        inputFileUrl = null;
        spinnerCategory = view.findViewById(R.id.spinner_category);
        spinnerLanguage = view.findViewById(R.id.spinner_language);
        spinnerVisibility = view.findViewById(R.id.spinner_visibility);
        textUploadDate = view.findViewById(R.id.text_upload_date);
        buttonSelectCover = view.findViewById(R.id.button_select_cover);
        buttonSelectPdf = view.findViewById(R.id.button_select_pdf);
        textCoverStatus = view.findViewById(R.id.text_cover_status);
        textPdfStatus = view.findViewById(R.id.text_pdf_status);
        buttonUploadBook = view.findViewById(R.id.button_upload_book);
        buttonPickDate = view.findViewById(R.id.button_pick_date);

        // Setup spinners
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, new String[]{"Self-help", "Fiction", "Non-fiction", "Science", "Biography", "Other"});
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        ArrayAdapter<String> languageAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, new String[]{"English", "Hindi", "French", "German", "Other"});
        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLanguage.setAdapter(languageAdapter);

        ArrayAdapter<String> visibilityAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, new String[]{"public", "private"});
        visibilityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerVisibility.setAdapter(visibilityAdapter);

        buttonPickDate.setOnClickListener(v -> pickDate());
        buttonSelectCover.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        buttonSelectPdf.setOnClickListener(v -> pickPdfLauncher.launch("application/pdf"));
        buttonUploadBook.setOnClickListener(v -> uploadBook());

        return view;
    }

    private void pickDate() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
            calendar.set(year, month, dayOfMonth);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
            uploadDateISO = sdf.format(calendar.getTime());
            textUploadDate.setText(uploadDateISO);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void uploadBook() {
        String title = inputTitle.getText().toString().trim();
        String author = inputAuthor.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();
        String language = spinnerLanguage.getSelectedItem().toString();
        String description = inputDescription.getText().toString().trim();
        String priceStr = inputPrice.getText().toString().trim();
        String visibility = spinnerVisibility.getSelectedItem().toString();
        String uploadDate = uploadDateISO != null ? uploadDateISO : new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault()).format(new java.util.Date());

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(author) || selectedCoverUri == null || selectedPdfUri == null) {
            Toast.makeText(requireContext(), "Please fill fields and select files", Toast.LENGTH_SHORT).show();
            return;
        }
        double price = 0;
        try { price = Double.parseDouble(priceStr); } catch (Exception ignored) {}

        buttonUploadBook.setEnabled(false);
        textCoverStatus.setText("Uploading...");
        textPdfStatus.setText("Uploading...");

        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        executor.execute(() -> {
            String coverUrl;
            String pdfUrl;
            try {
                coverUrl = uploadToCloudinary(selectedCoverUri, true);
                pdfUrl = uploadToCloudinary(selectedPdfUri, false);
            } catch (Exception e) {
                mainHandler.post(() -> {
                    buttonUploadBook.setEnabled(true);
                    Toast.makeText(requireContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    textCoverStatus.setText("Upload failed");
                    textPdfStatus.setText("Upload failed");
                });
                return;
            }

            java.util.Map<String, Object> bookData = new java.util.HashMap<>();
            bookData.put("title", title);
            bookData.put("author", author);
            bookData.put("category", category);
            bookData.put("language", language);
            bookData.put("description", description);
            bookData.put("price", priceStr);
            bookData.put("coverImageUrl", coverUrl);
            bookData.put("fileUrl", pdfUrl);
            bookData.put("visibility", visibility);
            bookData.put("uploadDate", uploadDate);

            com.google.firebase.database.DatabaseReference dbRef = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("ebooks");
            String bookId = "book" + System.currentTimeMillis();
            dbRef.child(bookId).setValue(bookData).addOnCompleteListener(task -> {
                buttonUploadBook.setEnabled(true);
                if (task.isSuccessful()) {
                    Toast.makeText(requireContext(), "Book uploaded successfully!", Toast.LENGTH_LONG).show();
                    clearForm();
                } else {
                    Toast.makeText(requireContext(), "Failed to upload book", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void clearForm() {
        inputTitle.setText("");
        inputAuthor.setText("");
        inputDescription.setText("");
        inputPrice.setText("");
        selectedCoverUri = null;
        selectedPdfUri = null;
        textCoverStatus.setText("No file selected");
        textPdfStatus.setText("No file selected");
        spinnerCategory.setSelection(0);
        spinnerLanguage.setSelection(0);
        spinnerVisibility.setSelection(0);
        textUploadDate.setText("Select date...");
        uploadDateISO = null;
    }

    private String uploadToCloudinary(Uri uri, boolean isImage) throws Exception {
        if (uri == null) throw new IllegalArgumentException("No file selected");

        // Step 1: Get signature from your Vercel API
        okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
        okhttp3.Request sigRequest = new okhttp3.Request.Builder()
                .url("https://bookhive-backend.vercel.app/api/get-signature.js")
                .build();

        okhttp3.Response sigResponse = client.newCall(sigRequest).execute();
        if (!sigResponse.isSuccessful()) throw new IllegalStateException("Failed to get signature");
        String sigBody = sigResponse.body().string();
        org.json.JSONObject sigJson = new org.json.JSONObject(sigBody);

        String cloudName = sigJson.getString("cloudName");
        String apiKey = sigJson.getString("apiKey");
        String signature = sigJson.getString("signature");
        String timestamp = String.valueOf(sigJson.getLong("timestamp"));
        String folder = sigJson.getString("folder");

        // Step 2: Prepare file
        android.content.ContentResolver resolver = requireContext().getContentResolver();
        java.io.InputStream inputStream = resolver.openInputStream(uri);
        if (inputStream == null) throw new IllegalStateException("Cannot open file");

        okhttp3.MediaType mediaType = okhttp3.MediaType.parse(isImage ? "image/*" : "application/pdf");
        okhttp3.RequestBody fileBody = okhttp3.RequestBody.create(readAllBytes(inputStream), mediaType);

        // Step 3: Build request to Cloudinary
        okhttp3.MultipartBody requestBody = new okhttp3.MultipartBody.Builder()
                .setType(okhttp3.MultipartBody.FORM)
                .addFormDataPart("file", isImage ? "cover.jpg" : "book.pdf", fileBody)
                .addFormDataPart("api_key", apiKey)
                .addFormDataPart("timestamp", timestamp)
                .addFormDataPart("signature", signature)
                .addFormDataPart("folder", folder)
                .build();

        // String uploadUrl = "https://api.cloudinary.com/v1_1/" + cloudName + "/auto/upload";
        String resourceType = isImage ? "image" : "raw";
String uploadUrl = "https://api.cloudinary.com/v1_1/" + cloudName + "/" + resourceType + "/upload";
        okhttp3.Request uploadRequest = new okhttp3.Request.Builder()
                .url(uploadUrl)
                .post(requestBody)
                .build();

        okhttp3.Response uploadResponse = client.newCall(uploadRequest).execute();
        if (!uploadResponse.isSuccessful()) throw new IllegalStateException("Upload failed: " + uploadResponse.code());
        String uploadBody = uploadResponse.body() != null ? uploadResponse.body().string() : null;
        if (uploadBody == null) throw new IllegalStateException("Empty upload response");

        org.json.JSONObject uploadJson = new org.json.JSONObject(uploadBody);
        return uploadJson.optString("secure_url", uploadJson.optString("url"));
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