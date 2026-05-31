package com.example.khoga.ui.activity;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.khoga.R;
import com.example.khoga.ui.fragment.ProductListFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        }

        setupBottomNavigation();
        handleNavigateIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleNavigateIntent(intent);
    }

    private void handleNavigateIntent(Intent intent) {
        if (intent == null || navController == null) return;
        String navigateTo = intent.getStringExtra("NAVIGATE_TO");
        if ("orderList".equals(navigateTo)) {
            intent.removeExtra("NAVIGATE_TO");
            navController.navigate(R.id.action_global_orderList);
        }
    }

    private Fragment getCurrentFragment() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            return navHostFragment.getChildFragmentManager().getPrimaryNavigationFragment();
        }
        return null;
    }

    private boolean isOnProductList() {
        return navController.getCurrentDestination() != null
                && navController.getCurrentDestination().getId() == R.id.productListFragment;
    }

    private void handleHomePress() {
        if (isOnProductList()) {
            Fragment current = getCurrentFragment();
            if (current instanceof ProductListFragment) {
                ((ProductListFragment) current).scrollToTop();
            }
        } else {
            navController.popBackStack(R.id.productListFragment, false);
        }
    }

    private void handleSearchPress(BottomNavigationView bottomNav) {
        if (!isOnProductList()) {
            navController.popBackStack(R.id.productListFragment, false);
        }
        bottomNav.post(() -> {
            Fragment current = getCurrentFragment();
            if (current instanceof ProductListFragment) {
                ((ProductListFragment) current).scrollToSearchAndFocus();
            }
        });
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav == null || navController == null) return;

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) { handleHomePress(); return true; }
            if (id == R.id.nav_search) { handleSearchPress(bottomNav); return true; }
            if (id == R.id.nav_cart) {
                if (navController.getCurrentDestination() != null
                        && navController.getCurrentDestination().getId() != R.id.cartFragment)
                    navController.navigate(R.id.action_global_cart);
                return true;
            }
            if (id == R.id.nav_ai) {
                if (navController.getCurrentDestination() != null
                        && navController.getCurrentDestination().getId() != R.id.chatFragment)
                    navController.navigate(R.id.action_global_chat);
                return true;
            }
            if (id == R.id.nav_profile) {
                if (navController.getCurrentDestination() != null
                        && navController.getCurrentDestination().getId() != R.id.profileFragment)
                    navController.navigate(R.id.action_global_profile);
                return true;
            }
            return true;
        });

        bottomNav.setOnItemReselectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) handleHomePress();
            if (id == R.id.nav_search) handleSearchPress(bottomNav);
        });

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int destId = destination.getId();
            if (destId == R.id.productListFragment)
                bottomNav.getMenu().findItem(R.id.nav_home).setChecked(true);
            else if (destId == R.id.cartFragment)
                bottomNav.getMenu().findItem(R.id.nav_cart).setChecked(true);
            else if (destId == R.id.chatFragment)
                bottomNav.getMenu().findItem(R.id.nav_ai).setChecked(true);
            else if (destId == R.id.profileFragment)
                bottomNav.getMenu().findItem(R.id.nav_profile).setChecked(true);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null && navController != null && isOnProductList()) {
            bottomNav.getMenu().findItem(R.id.nav_home).setChecked(true);
        }
    }

    public NavController getNavController() { return navController; }
}