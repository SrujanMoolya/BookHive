package com.svvaap.bookhive;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.svvaap.bookhive.databinding.FragmentBookSalesAnalyticsBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;
import android.widget.TextView;

public class BookSalesAnalyticsFragment extends Fragment {
    private FragmentBookSalesAnalyticsBinding binding;
    private List<BookSale> bookSales = new ArrayList<>();
    private BookSaleAdapter adapter;
    private DatabaseReference ordersRef;
    private DatabaseReference booksRef;
    private ValueEventListener ordersListener;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentBookSalesAnalyticsBinding.inflate(inflater, container, false);
        setupRecyclerView();
        fetchSalesData();
        return binding.getRoot();
    }

    private void setupRecyclerView() {
        adapter = new BookSaleAdapter(bookSales);
        binding.salesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.salesRecyclerView.setAdapter(adapter);
    }

    private void fetchSalesData() {
        ordersRef = FirebaseDatabase.getInstance().getReference("orders");
        booksRef = FirebaseDatabase.getInstance().getReference("ebooks");

        ordersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                bookSales.clear();
                double totalRevenue = 0;
                int totalOrders = 0;

                for (DataSnapshot orderSnap : snapshot.getChildren()) {
                    Order order = orderSnap.getValue(Order.class);
                    if (order != null && "completed".equals(order.status)) {
                        totalRevenue += order.bookPrice;
                        totalOrders++;

                        // Find book details
                        BookSale bookSale = new BookSale();
                        bookSale.bookId = order.bookId;
                        bookSale.bookTitle = order.bookTitle;
                        bookSale.bookAuthor = order.bookAuthor;
                        bookSale.salesCount = 1;
                        bookSale.revenue = order.bookPrice;

                        // Check if book already exists in list
                        boolean found = false;
                        for (BookSale existing : bookSales) {
                            if (existing.bookId != null && existing.bookId.equals(order.bookId)) {
                                existing.salesCount++;
                                existing.revenue += order.bookPrice;
                                found = true;
                                break;
                            }
                        }

                        if (!found) {
                            bookSales.add(bookSale);
                        }
                    }
                }

                // Sort by revenue (highest first)
                Collections.sort(bookSales, (a, b) -> Double.compare(b.revenue, a.revenue));

                // Update UI
                binding.totalRevenueText.setText("$" + String.format("%.2f", totalRevenue));
                binding.totalOrdersText.setText(String.valueOf(totalOrders));
                binding.topBooksText.setText("Top " + Math.min(bookSales.size(), 5) + " Books");

                adapter.notifyDataSetChanged();
                binding.noDataText.setVisibility(bookSales.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        };
        ordersRef.addValueEventListener(ordersListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (ordersRef != null && ordersListener != null) {
            ordersRef.removeEventListener(ordersListener);
        }
        binding = null;
    }

    // BookSale data model
    public static class BookSale {
        public String bookId;
        public String bookTitle;
        public String bookAuthor;
        public int salesCount;
        public double revenue;
        public String category;

        public BookSale() {}
    }

    // Order data model (for internal use)
    private static class Order {
        public String bookId;
        public String bookTitle;
        public String bookAuthor;
        public double bookPrice;
        public String status;

        public Order() {}
    }

    // BookSale adapter
    private class BookSaleAdapter extends RecyclerView.Adapter<BookSaleAdapter.BookSaleViewHolder> {
        private List<BookSale> saleList;

        public BookSaleAdapter(List<BookSale> saleList) {
            this.saleList = saleList;
        }

        @NonNull
        @Override
        public BookSaleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_book_sale, parent, false);
            return new BookSaleViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull BookSaleViewHolder holder, int position) {
            BookSale bookSale = saleList.get(position);
            holder.bind(bookSale, position + 1);
        }

        @Override
        public int getItemCount() {
            return saleList.size();
        }

        class BookSaleViewHolder extends RecyclerView.ViewHolder {
            private TextView rankText, bookTitleText, bookAuthorText, salesCountText, revenueText;

            public BookSaleViewHolder(@NonNull View itemView) {
                super(itemView);
                rankText = itemView.findViewById(R.id.rank);
                bookTitleText = itemView.findViewById(R.id.book_title);
                bookAuthorText = itemView.findViewById(R.id.book_author);
                salesCountText = itemView.findViewById(R.id.sales_count);
                revenueText = itemView.findViewById(R.id.revenue);
            }

            public void bind(BookSale bookSale, int rank) {
                rankText.setText("#" + rank);
                bookTitleText.setText(bookSale.bookTitle != null ? bookSale.bookTitle : "Unknown Book");
                bookAuthorText.setText(bookSale.bookAuthor != null ? bookSale.bookAuthor : "Unknown Author");
                salesCountText.setText("Sold: " + bookSale.salesCount);
                revenueText.setText("$" + String.format("%.2f", bookSale.revenue));
                
                // Set rank color for top 3
                int rankColor;
                switch (rank) {
                    case 1:
                        rankColor = getResources().getColor(android.R.color.holo_orange_dark);
                        break;
                    case 2:
                        rankColor = getResources().getColor(android.R.color.holo_blue_dark);
                        break;
                    case 3:
                        rankColor = getResources().getColor(android.R.color.holo_green_dark);
                        break;
                    default:
                        rankColor = getResources().getColor(android.R.color.darker_gray);
                        break;
                }
                rankText.setTextColor(rankColor);
            }
        }
    }
} 