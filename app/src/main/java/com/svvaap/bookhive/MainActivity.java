package com.svvaap.bookhive;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.widget.Toast;
import com.razorpay.PaymentResultListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity implements PaymentResultListener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_content_main);
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.HomeFragment, R.id.SearchFragment, R.id.CatalogueFragment, R.id.ProfileFragment, R.id.CartFragment
            ).build();
            NavigationUI.setupWithNavController(toolbar, navController, appBarConfiguration);
            // Enable Up button across all fragments
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> onBackPressed());

            BottomNavigationView bottomNav = findViewById(R.id.bottom_nav_view);
            NavigationUI.setupWithNavController(bottomNav, navController);

            // If user is not logged in, show Login screen first
            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                navController.navigate(R.id.LoginFragment);
            }
        }
    }

    @Override
    public void onPaymentSuccess(String razorpayPaymentID) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid != null) {
            // If coming from Buy Now, mark that single book as purchased as well using NavController args
            String singleBookId = null;
            NavHostFragment host = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
            if (host != null) {
                NavController nav = host.getNavController();
                if (nav.getCurrentBackStackEntry() != null && nav.getCurrentBackStackEntry().getArguments() != null) {
                    singleBookId = nav.getCurrentBackStackEntry().getArguments().getString("singleBookId", null);
                }
            }
            if (singleBookId != null) {
                FirebaseDatabase.getInstance().getReference("purchases").child(uid).child(singleBookId).setValue(true);
            } else {
                DatabaseReference cartRef = FirebaseDatabase.getInstance().getReference("carts").child(uid);
                cartRef.get().addOnSuccessListener(snapshot -> {
                    for (com.google.firebase.database.DataSnapshot snap : snapshot.getChildren()) {
                        String bookId = snap.getKey();
                        if (bookId != null) {
                            FirebaseDatabase.getInstance().getReference("purchases").child(uid).child(bookId).setValue(true);
                        }
                    }
                    cartRef.removeValue();
                });
            }
        }
        Toast.makeText(this, "Payment success", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPaymentError(int code, String response) {
        Toast.makeText(this, "Payment failed", Toast.LENGTH_SHORT).show();
    }
}