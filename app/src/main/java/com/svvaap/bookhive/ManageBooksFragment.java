package com.svvaap.bookhive;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.*;
import java.util.*;

public class ManageBooksFragment extends Fragment implements BooksManageAdapter.BookActionListener {
    private RecyclerView recyclerBooks;
    private BooksManageAdapter adapter;
    private List<Book> bookList = new ArrayList<>();
    private DatabaseReference dbRef;

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
        EditText inputCoverUrl = dialogView.findViewById(R.id.input_cover_url);
        EditText inputFileUrl = dialogView.findViewById(R.id.input_file_url);
        Spinner spinnerCategory = dialogView.findViewById(R.id.spinner_category);
        Spinner spinnerLanguage = dialogView.findViewById(R.id.spinner_language);
        Spinner spinnerVisibility = dialogView.findViewById(R.id.spinner_visibility);

        // Set current values
        inputTitle.setText(book.title);
        inputAuthor.setText(book.author);
        inputDescription.setText(book.description);
        inputPrice.setText(String.valueOf(book.price));
        inputCoverUrl.setText(book.coverImageUrl);
        inputFileUrl.setText(book.fileUrl);

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
                    String title = inputTitle.getText().toString().trim();
                    String author = inputAuthor.getText().toString().trim();
                    String description = inputDescription.getText().toString().trim();
                    String priceStr = inputPrice.getText().toString().trim();
                    String coverUrl = inputCoverUrl.getText().toString().trim();
                    String fileUrl = inputFileUrl.getText().toString().trim();
                    String category = spinnerCategory.getSelectedItem().toString();
                    String language = spinnerLanguage.getSelectedItem().toString();
                    String visibility = spinnerVisibility.getSelectedItem().toString();
                    double price = 0;
                    try { price = Double.parseDouble(priceStr); } catch (Exception ignored) {}
                    if (TextUtils.isEmpty(title) || TextUtils.isEmpty(author) || TextUtils.isEmpty(coverUrl) || TextUtils.isEmpty(fileUrl)) {
                        Toast.makeText(getContext(), "Please fill all required fields and provide URLs", Toast.LENGTH_SHORT).show();
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
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
} 