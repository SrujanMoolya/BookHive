package com.example.bookhive;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class UploadBookFragment extends Fragment {
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PICK_FILE_REQUEST = 2;

    private TextInputEditText inputTitle, inputAuthor, inputDescription, inputPrice, inputCoverUrl, inputFileUrl;
    private Spinner spinnerCategory, spinnerLanguage, spinnerVisibility;
    private TextView textUploadDate;
    private Button buttonUploadBook, buttonPickDate;
    private String uploadDateISO;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_upload_book, container, false);
        inputTitle = view.findViewById(R.id.input_title);
        inputAuthor = view.findViewById(R.id.input_author);
        inputDescription = view.findViewById(R.id.input_description);
        inputPrice = view.findViewById(R.id.input_price);
        inputCoverUrl = view.findViewById(R.id.input_cover_url);
        inputFileUrl = view.findViewById(R.id.input_file_url);
        spinnerCategory = view.findViewById(R.id.spinner_category);
        spinnerLanguage = view.findViewById(R.id.spinner_language);
        spinnerVisibility = view.findViewById(R.id.spinner_visibility);
        textUploadDate = view.findViewById(R.id.text_upload_date);
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
        String coverImageUrl = inputCoverUrl.getText().toString().trim();
        String fileUrl = inputFileUrl.getText().toString().trim();
        String visibility = spinnerVisibility.getSelectedItem().toString();
        String uploadDate = uploadDateISO != null ? uploadDateISO : new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault()).format(new java.util.Date());

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(author) || TextUtils.isEmpty(coverImageUrl) || TextUtils.isEmpty(fileUrl)) {
            Toast.makeText(getContext(), "Please fill all required fields and provide URLs", Toast.LENGTH_SHORT).show();
            return;
        }
        double price = 0;
        try { price = Double.parseDouble(priceStr); } catch (Exception ignored) {}

        // Save to database
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("ebooks");
        String bookId = "book" + System.currentTimeMillis();
        java.util.Map<String, Object> bookData = new java.util.HashMap<>();
        bookData.put("title", title);
        bookData.put("author", author);
        bookData.put("category", category);
        bookData.put("language", language);
        bookData.put("description", description);
        bookData.put("price", price);
        bookData.put("coverImageUrl", coverImageUrl);
        bookData.put("fileUrl", fileUrl);
        bookData.put("visibility", visibility);
        bookData.put("uploadDate", uploadDate);
        dbRef.child(bookId).setValue(bookData).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Book uploaded successfully!", Toast.LENGTH_LONG).show();
                clearForm();
            } else {
                Toast.makeText(getContext(), "Failed to upload book", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void clearForm() {
        inputTitle.setText("");
        inputAuthor.setText("");
        inputDescription.setText("");
        inputPrice.setText("");
        inputCoverUrl.setText("");
        inputFileUrl.setText("");
        spinnerCategory.setSelection(0);
        spinnerLanguage.setSelection(0);
        spinnerVisibility.setSelection(0);
        textUploadDate.setText("Select date...");
        uploadDateISO = null;
    }
} 