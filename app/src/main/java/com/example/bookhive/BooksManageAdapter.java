package com.example.bookhive;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class BooksManageAdapter extends RecyclerView.Adapter<BooksManageAdapter.BookViewHolder> {
    public interface BookActionListener {
        void onEdit(Book book);
        void onDelete(Book book);
    }

    private List<Book> books;
    private BookActionListener listener;

    public BooksManageAdapter(List<Book> books, BookActionListener listener) {
        this.books = books;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_book_manage, parent, false);
        return new BookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
        Book book = books.get(position);
        holder.textTitle.setText(book.title);
        holder.textAuthor.setText(book.author);
        holder.textPrice.setText("₹" + book.price);
        holder.buttonEdit.setOnClickListener(v -> listener.onEdit(book));
        holder.buttonDelete.setOnClickListener(v -> listener.onDelete(book));
    }

    @Override
    public int getItemCount() {
        return books.size();
    }

    public void setBooks(List<Book> books) {
        this.books = books;
        notifyDataSetChanged();
    }

    static class BookViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle, textAuthor, textPrice;
        Button buttonEdit, buttonDelete;
        BookViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.text_title);
            textAuthor = itemView.findViewById(R.id.text_author);
            textPrice = itemView.findViewById(R.id.text_price);
            buttonEdit = itemView.findViewById(R.id.button_edit);
            buttonDelete = itemView.findViewById(R.id.button_delete);
        }
    }
} 