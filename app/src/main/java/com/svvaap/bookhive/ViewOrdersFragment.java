package com.svvaap.bookhive;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.svvaap.bookhive.databinding.FragmentViewOrdersBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;
import android.widget.TextView;

public class ViewOrdersFragment extends Fragment {
    private FragmentViewOrdersBinding binding;
    private List<Order> orders = new ArrayList<>();
    private OrderAdapter adapter;
    private DatabaseReference ordersRef;
    private ValueEventListener ordersListener;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentViewOrdersBinding.inflate(inflater, container, false);
        setupRecyclerView();
        fetchOrders();
        return binding.getRoot();
    }

    private void setupRecyclerView() {
        adapter = new OrderAdapter(orders);
        binding.ordersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.ordersRecyclerView.setAdapter(adapter);
    }

    private void fetchOrders() {
        ordersRef = FirebaseDatabase.getInstance().getReference("orders");
        ordersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                orders.clear();
                for (DataSnapshot orderSnap : snapshot.getChildren()) {
                    Order order = orderSnap.getValue(Order.class);
                    if (order != null) {
                        order.orderId = orderSnap.getKey();
                        orders.add(order);
                    }
                }
                adapter.notifyDataSetChanged();
                binding.noOrdersText.setVisibility(orders.isEmpty() ? View.VISIBLE : View.GONE);
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

    // Order data model
    public static class Order {
        public String orderId;
        public String userId;
        public String userName;
        public String userEmail;
        public String bookId;
        public String bookTitle;
        public String bookAuthor;
        public double bookPrice;
        public String orderDate;
        public String status; // "pending", "completed", "cancelled"
        public String paymentMethod;

        public Order() {}
    }

    // Order adapter
    private class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {
        private List<Order> orderList;

        public OrderAdapter(List<Order> orderList) {
            this.orderList = orderList;
        }

        @NonNull
        @Override
        public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order, parent, false);
            return new OrderViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
            Order order = orderList.get(position);
            holder.bind(order);
        }

        @Override
        public int getItemCount() {
            return orderList.size();
        }

        class OrderViewHolder extends RecyclerView.ViewHolder {
            private TextView orderIdText, userNameText, bookTitleText, orderDateText, statusText, priceText;

            public OrderViewHolder(@NonNull View itemView) {
                super(itemView);
                orderIdText = itemView.findViewById(R.id.order_id);
                userNameText = itemView.findViewById(R.id.user_name);
                bookTitleText = itemView.findViewById(R.id.book_title);
                orderDateText = itemView.findViewById(R.id.order_date);
                statusText = itemView.findViewById(R.id.order_status);
                priceText = itemView.findViewById(R.id.book_price);
            }

            public void bind(Order order) {
                orderIdText.setText("Order #" + order.orderId);
                userNameText.setText(order.userName != null ? order.userName : "Unknown User");
                bookTitleText.setText(order.bookTitle != null ? order.bookTitle : "Unknown Book");
                orderDateText.setText(order.orderDate != null ? order.orderDate : "Unknown Date");
                statusText.setText(order.status != null ? order.status.toUpperCase() : "PENDING");
                priceText.setText("₹" + order.bookPrice);

                // Set status color
                int statusColor;
                switch (order.status != null ? order.status.toLowerCase() : "pending") {
                    case "completed":
                        statusColor = getResources().getColor(android.R.color.holo_green_dark);
                        break;
                    case "cancelled":
                        statusColor = getResources().getColor(android.R.color.holo_red_dark);
                        break;
                    default:
                        statusColor = getResources().getColor(android.R.color.holo_orange_dark);
                        break;
                }
                statusText.setTextColor(statusColor);
            }
        }
    }
} 