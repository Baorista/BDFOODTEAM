package com.example.khoga.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.khoga.R;
import com.example.khoga.adapter.BannerAdapter;
import com.example.khoga.adapter.CategoryAdapter;
import com.example.khoga.adapter.ProductAdapter;
import com.example.khoga.viewmodel.ProductViewModel;

public class ProductListFragment extends Fragment {

    private ProductViewModel viewModel;

    // Views
    private ViewPager2       vpBanner;
    private RecyclerView     rvCategories, rvProducts;
    private EditText         etSearch;
    private ImageButton      btnSortAsc, btnSortDesc;
    private Button           btnPrev, btnNext;
    private TextView         tvPage, tvEmpty, tvError;
    private ProgressBar      progressBar;
    private NestedScrollView nestedScrollView;

    // Adapters
    private BannerAdapter    bannerAdapter;
    private CategoryAdapter  categoryAdapter;
    private ProductAdapter   productAdapter;

    // [FIX Bug 2] Lưu scroll position khi rời khỏi fragment
    private int savedScrollY = -1;

    // [FIX Bug 2] Ngăn TextWatcher trigger onSearchQueryChange khi đang restore state
    // (vì onSearchQueryChange sẽ reset currentPage về 0)
    private boolean isRestoringState = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_product_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindViews(view);
        setupAdapters();

        // Bật cờ trước khi setup ViewModel và Listeners
        // để TextWatcher không gọi onSearchQueryChange → không reset page
        isRestoringState = true;

        setupViewModel();
        setupListeners();

        // Tắt cờ sau khi mọi thứ đã khôi phục xong
        etSearch.post(() -> isRestoringState = false);

        // [FIX Bug 2] Khôi phục scroll position khi quay lại từ detail
        if (savedScrollY >= 0) {
            final int scrollY = savedScrollY;
            nestedScrollView.post(() -> nestedScrollView.scrollTo(0, scrollY));
            savedScrollY = -1;
        }
    }

    @Override
    public void onDestroyView() {
        // [FIX Bug 2] Lưu scroll position trước khi view bị destroy
        if (nestedScrollView != null) {
            savedScrollY = nestedScrollView.getScrollY();
        }
        super.onDestroyView();
    }

    // ══════════════════════════════════════════════════════════════
    // PUBLIC METHODS — được gọi từ MainActivity khi ấn tab bottom nav
    // ══════════════════════════════════════════════════════════════

    /**
     * [FIX Bug 1] Cuộn lên đầu trang rồi focus vào ô tìm kiếm.
     * Gọi từ MainActivity khi người dùng ấn tab Search.
     */
    public void scrollToSearchAndFocus() {
        if (nestedScrollView == null || etSearch == null) return;

        nestedScrollView.scrollTo(0, 0);

        etSearch.post(() -> {
            etSearch.requestFocus();
            showKeyboard(etSearch);
        });
    }

    /**
     * Cuộn về đầu trang. Gọi từ MainActivity khi ấn tab Home.
     */
    public void scrollToTop() {
        if (nestedScrollView != null) {
            nestedScrollView.smoothScrollTo(0, 0);
        }
    }

    // ══════════════════════════════════════════════════════════════

    private void bindViews(View v) {
        vpBanner         = v.findViewById(R.id.vpBanner);
        rvCategories     = v.findViewById(R.id.rvCategories);
        rvProducts       = v.findViewById(R.id.rvProducts);
        etSearch         = v.findViewById(R.id.etSearch);
        btnSortAsc       = v.findViewById(R.id.btnSortAsc);
        btnSortDesc      = v.findViewById(R.id.btnSortDesc);
        btnPrev          = v.findViewById(R.id.btnPrev);
        btnNext          = v.findViewById(R.id.btnNext);
        tvPage           = v.findViewById(R.id.tvPage);
        tvEmpty          = v.findViewById(R.id.tvEmpty);
        tvError          = v.findViewById(R.id.tvError);
        progressBar      = v.findViewById(R.id.progressBar);
        nestedScrollView = v.findViewById(R.id.nestedScrollView);
    }

    private void setupAdapters() {
        // Banner slider
        bannerAdapter = new BannerAdapter();
        vpBanner.setAdapter(bannerAdapter);

        // Category chips (Horizontal)
        categoryAdapter = new CategoryAdapter(categoryId ->
                viewModel.filterByCategory(categoryId));
        rvCategories.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvCategories.setAdapter(categoryAdapter);

        // Products grid (2 columns)
        productAdapter = new ProductAdapter(product -> {
            viewModel.selectProduct(product);
            Bundle args = new Bundle();
            args.putString("productId", product.getProductId());
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_list_to_detail, args);
        });
        rvProducts.setLayoutManager(new GridLayoutManager(getContext(), 2));
        rvProducts.setNestedScrollingEnabled(false);
        rvProducts.setAdapter(productAdapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(requireActivity()).get(ProductViewModel.class);

        viewModel.getBanners().observe(getViewLifecycleOwner(),
                banners -> bannerAdapter.setBanners(banners));

        viewModel.getCategories().observe(getViewLifecycleOwner(),
                categories -> categoryAdapter.setCategories(categories));

        viewModel.getDisplayed().observe(getViewLifecycleOwner(), products -> {
            productAdapter.setProducts(products);
            tvEmpty.setVisibility(products.isEmpty() ? View.VISIBLE : View.GONE);
            rvProducts.setVisibility(products.isEmpty() ? View.GONE : View.VISIBLE);
        });

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            if (loading) {
                rvProducts.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.GONE);
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                tvError.setVisibility(View.VISIBLE);
                tvError.setText("⚠ " + error);
                viewModel.clearError();
            } else {
                tvError.setVisibility(View.GONE);
            }
        });

        viewModel.getCurrentPage().observe(getViewLifecycleOwner(), page -> updatePagination());
        viewModel.getTotalPages().observe(getViewLifecycleOwner(), total -> updatePagination());

        viewModel.getSortType().observe(getViewLifecycleOwner(), sortType -> {
            btnSortAsc.setAlpha(sortType == ProductViewModel.SORT_PRICE_ASC ? 1.0f : 0.4f);
            btnSortDesc.setAlpha(sortType == ProductViewModel.SORT_PRICE_DESC ? 1.0f : 0.4f);
        });

        // [FIX Bug 2] Khôi phục nội dung search từ ViewModel khi quay lại
        // (isRestoringState = true nên TextWatcher sẽ bỏ qua, không reset page)
        String currentQuery = viewModel.getSearchQuery().getValue();
        if (currentQuery != null && !currentQuery.isEmpty()) {
            etSearch.setText(currentQuery);
            etSearch.setSelection(currentQuery.length());
        }

        // [FIX Bug 2] Phát lại dữ liệu trang hiện tại cho observer mới
        // ViewModel giữ nguyên currentPage — chỉ phát lại displayedLD
        viewModel.refreshDisplay();
    }

    private void setupListeners() {
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = etSearch.getText().toString().trim();
                viewModel.onSearchQueryChange(query);
                hideKeyboard(v);
                return true;
            }
            return false;
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                // [FIX Bug 2] Bỏ qua nếu đang khôi phục state
                // để tránh gọi onSearchQueryChange("") → reset currentPage về 0
                if (isRestoringState) return;

                if (s.toString().isEmpty()) {
                    viewModel.onSearchQueryChange("");
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnSortAsc.setOnClickListener(v  -> viewModel.toggleSortAscending());
        btnSortDesc.setOnClickListener(v -> viewModel.toggleSortDescending());
        btnPrev.setOnClickListener(v     -> viewModel.previousPage());
        btnNext.setOnClickListener(v     -> viewModel.nextPage());
    }

    private void updatePagination() {
        Integer page  = viewModel.getCurrentPage().getValue();
        Integer total = viewModel.getTotalPages().getValue();
        int currentPage = (page != null) ? page : 0;
        int totalPages = (total != null) ? total : 1;

        tvPage.setText("Trang " + (currentPage + 1) + " / " + totalPages);
        btnPrev.setEnabled(currentPage > 0);
        btnNext.setEnabled(currentPage < totalPages - 1);
    }

    private void showKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) requireContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) requireContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}