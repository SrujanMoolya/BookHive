package com.svvaap.bookhive;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import org.json.JSONObject;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.svvaap.bookhive.databinding.FragmentCheckoutBinding;
import com.razorpay.Checkout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;
import androidx.navigation.Navigation;

public class CheckoutFragment extends Fragment {

    private FragmentCheckoutBinding binding;
    private double amountTotal = 0.0; // reserved for future UI totals

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCheckoutBinding.inflate(inflater, container, false);
        // Preload Razorpay SDK
        Checkout.preload(requireContext().getApplicationContext());
        binding.textviewCheckout.setText("Checkout");
        binding.textviewCheckout.setOnClickListener(v -> startPayment());
        // Auto-start payment when this screen opens
        binding.getRoot().post(this::startPayment);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void startPayment() {
        try {
            String uid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
            if (uid == null) { 
                Toast.makeText(getContext(), "Login required", Toast.LENGTH_SHORT).show();
                try { Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main).navigate(R.id.LoginFragment); } catch (Exception ignored) {}
                return; 
            }
            // support single-book buy now
            Bundle args = getArguments();
            if (args != null && args.containsKey("singleBookId")) {
                double total = args.getDouble("singleAmount", 0);
                if (total <= 0.0) { Toast.makeText(getContext(), "Invalid amount", Toast.LENGTH_SHORT).show(); return; }
                openRazorpay(total);
            } else {
                DatabaseReference cartRef = FirebaseDatabase.getInstance().getReference("carts").child(uid);
                cartRef.get().addOnSuccessListener(snapshot -> {
                    double total = 0.0;
                    for (com.google.firebase.database.DataSnapshot snap : snapshot.getChildren()) {
                        Object priceObj = snap.child("price").getValue();
                        if (priceObj instanceof Number) total += ((Number) priceObj).doubleValue();
                    }
                    if (total <= 0.0) { Toast.makeText(getContext(), "Cart is empty", Toast.LENGTH_SHORT).show(); return; }
                    openRazorpay(total);
                });
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Payment init failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void openRazorpay(double total) {
        try {
            Checkout checkout = new Checkout();
            checkout.setKeyID("rzp_live_Bzuaj1lGfUylYf");
            JSONObject options = new JSONObject();
            options.put("name", "BookHive");
            options.put("description", "Book purchase");
            options.put("currency", "INR");
            long amountPaise = Math.round(total * 100);
            options.put("amount", amountPaise);
            checkout.open(requireActivity(), options);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Payment init failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
} 