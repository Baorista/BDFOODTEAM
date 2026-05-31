package com.example.khoga.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.khoga.model.Banner;
import com.example.khoga.model.Category;
import com.example.khoga.model.Product;
import com.example.khoga.repository.ProductRepository;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class ProductViewModel extends ViewModel {

    public static final int SORT_DEFAULT    = 0;
    public static final int SORT_PRICE_ASC  = 1;
    public static final int SORT_PRICE_DESC = 2;
    public static final int PAGE_SIZE       = 8;

    private final ProductRepository repository = new ProductRepository();
    private List<Product> allProducts = new ArrayList<>();

    // LiveData
    private final MutableLiveData<List<Category>> categoriesLD = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Banner>>   bannersLD    = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Product>>  displayedLD  = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Product>        selectedLD   = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean>        loadingLD    = new MutableLiveData<>(false);
    private final MutableLiveData<String>         errorLD      = new MutableLiveData<>(null);
    private final MutableLiveData<Integer>        currentPageLD= new MutableLiveData<>(0);
    private final MutableLiveData<Integer>        totalPagesLD = new MutableLiveData<>(1);
    private final MutableLiveData<Integer>        sortTypeLD   = new MutableLiveData<>(SORT_DEFAULT);
    private final MutableLiveData<String>         searchLD     = new MutableLiveData<>("");

    // Filter state
    private String selectedCategoryId = null;
    private String selectedBrand      = null;
    private double priceMin           = -1;
    private double priceMax           = -1;

    // Pattern dùng để xóa dấu tiếng Việt - tạo 1 lần dùng lại
    private static final Pattern DIACRITICS_PATTERN =
            Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    public ProductViewModel() {
        loadCategories();
        loadBanners();
        loadAllProducts();
    }

    // ── Getters LiveData ─────────────────────────
    public LiveData<List<Category>> getCategories()  { return categoriesLD; }
    public LiveData<List<Banner>>   getBanners()     { return bannersLD; }
    public LiveData<List<Product>>  getDisplayed()   { return displayedLD; }
    public LiveData<Product>        getSelected()    { return selectedLD; }
    public LiveData<Boolean>        getLoading()     { return loadingLD; }
    public LiveData<String>         getError()       { return errorLD; }
    public LiveData<Integer>        getCurrentPage() { return currentPageLD; }
    public LiveData<Integer>        getTotalPages()  { return totalPagesLD; }
    public LiveData<Integer>        getSortType()    { return sortTypeLD; }
    public LiveData<String>         getSearchQuery() { return searchLD; }

    // ── Load ─────────────────────────────────────
    public void loadCategories() {
        repository.loadCategories(
                data -> categoriesLD.postValue(data),
                msg  -> errorLD.postValue(msg));
    }

    public void loadBanners() {
        repository.loadActiveBanners(
                data -> bannersLD.postValue(data),
                msg  -> errorLD.postValue(msg));
    }

    public void loadAllProducts() {
        loadingLD.postValue(true);
        repository.loadProducts(products -> {
            allProducts = products;
            loadingLD.postValue(false);
            applyFiltersAndSort();
        }, msg -> {
            errorLD.postValue(msg);
            loadingLD.postValue(false);
        });
    }

    public void selectProduct(Product product) { selectedLD.setValue(product); }
    public void clearSelectedProduct()         { selectedLD.setValue(null); }
    public void clearError()                   { errorLD.setValue(null); }

    // ═══ FIX LỖI 2: Phát lại dữ liệu trang hiện tại ═══
    // Gọi khi fragment được tạo lại (quay về từ Detail).
    // Giữ nguyên currentPage, filter, sort — chỉ phát lại displayedLD
    // để observer mới nhận được dữ liệu đúng trang.
    public void refreshDisplay() {
        if (!allProducts.isEmpty()) {
            applyFiltersAndSort();
        }
    }

    // ── Search ───────────────────────────────────
    public void onSearchQueryChange(String query) {
        searchLD.setValue(query);
        currentPageLD.setValue(0);
        applyFiltersAndSort();
    }

    // ── Filter ───────────────────────────────────
    public void filterByCategory(String categoryId) {
        this.selectedCategoryId = categoryId;
        currentPageLD.setValue(0);
        applyFiltersAndSort();
    }

    public void filterByPriceRange(double min, double max) {
        this.priceMin = min;
        this.priceMax = max;
        currentPageLD.setValue(0);
        applyFiltersAndSort();
    }

    public void filterByBrand(String brand) {
        this.selectedBrand = brand;
        currentPageLD.setValue(0);
        applyFiltersAndSort();
    }

    public void clearAllFilters() {
        selectedCategoryId = null;
        selectedBrand      = null;
        priceMin           = -1;
        priceMax           = -1;
        searchLD.setValue("");
        sortTypeLD.setValue(SORT_DEFAULT);
        currentPageLD.setValue(0);
        applyFiltersAndSort();
    }

    // ── Sort ─────────────────────────────────────
    public void toggleSortAscending() {
        int cur = intVal(sortTypeLD);
        sortTypeLD.setValue(cur == SORT_PRICE_ASC ? SORT_DEFAULT : SORT_PRICE_ASC);
        currentPageLD.setValue(0);
        applyFiltersAndSort();
    }

    public void toggleSortDescending() {
        int cur = intVal(sortTypeLD);
        sortTypeLD.setValue(cur == SORT_PRICE_DESC ? SORT_DEFAULT : SORT_PRICE_DESC);
        currentPageLD.setValue(0);
        applyFiltersAndSort();
    }

    // ── Pagination ───────────────────────────────
    public void nextPage() {
        int page = intVal(currentPageLD), total = intVal(totalPagesLD);
        if (page < total - 1) { currentPageLD.setValue(page + 1); applyFiltersAndSort(); }
    }

    public void previousPage() {
        int page = intVal(currentPageLD);
        if (page > 0) { currentPageLD.setValue(page - 1); applyFiltersAndSort(); }
    }

    // ── Bỏ dấu tiếng Việt ───────────────────────
    // "Khô gà lá chanh" → "kho ga la chanh"
    // "đồ uống"         → "do uong"
    private static String removeDiacritics(String input) {
        if (input == null) return "";
        // Bước 1: chuyển đ/Đ thành d/D trước (Normalizer không xử lý đ)
        String replaced = input.replace('đ', 'd').replace('Đ', 'D');
        // Bước 2: tách dấu ra khỏi ký tự gốc rồi xóa dấu
        String normalized = Normalizer.normalize(replaced, Normalizer.Form.NFD);
        return DIACRITICS_PATTERN.matcher(normalized).replaceAll("").toLowerCase();
    }

    // ── Core logic ───────────────────────────────
    private void applyFiltersAndSort() {
        List<Product> result = new ArrayList<>(allProducts);

        // Filter danh mục
        if (selectedCategoryId != null && !selectedCategoryId.isEmpty()) {
            List<Product> f = new ArrayList<>();
            for (Product p : result) if (selectedCategoryId.equals(p.getCategoryId())) f.add(p);
            result = f;
        }
        // Filter thương hiệu
        if (selectedBrand != null && !selectedBrand.isEmpty()) {
            List<Product> f = new ArrayList<>();
            for (Product p : result) {
                String brand = p.getBrand();
                if (brand != null && selectedBrand.equalsIgnoreCase(brand)) f.add(p);
            }
            result = f;
        }
        // Filter giá
        if (priceMin >= 0 && priceMax >= 0) {
            List<Product> f = new ArrayList<>();
            for (Product p : result)
                if (p.getDisplayPrice() >= priceMin && p.getDisplayPrice() <= priceMax) f.add(p);
            result = f;
        }
        // Search - dùng removeDiacritics để hỗ trợ tìm không dấu
        String q = removeDiacritics(strVal(searchLD).trim());
        if (!q.isEmpty()) {
            List<Product> f = new ArrayList<>();
            for (Product p : result) {
                String name  = removeDiacritics(p.getName());
                String brand = removeDiacritics(p.getBrand());
                if (name.contains(q) || brand.contains(q)) f.add(p);
            }
            result = f;
        }
        // Sort - dùng getDisplayPrice() để sort theo giá thực tế hiển thị
        int sort = intVal(sortTypeLD);
        if (sort == SORT_PRICE_ASC)
            Collections.sort(result, (a, b) -> Double.compare(a.getDisplayPrice(), b.getDisplayPrice()));
        else if (sort == SORT_PRICE_DESC)
            Collections.sort(result, (a, b) -> Double.compare(b.getDisplayPrice(), a.getDisplayPrice()));

        // Tổng trang
        int total = result.isEmpty() ? 1 : (result.size() - 1) / PAGE_SIZE + 1;
        totalPagesLD.setValue(total);

        int page = intVal(currentPageLD);
        if (page >= total) { page = total - 1; currentPageLD.setValue(page); }

        int start = page * PAGE_SIZE;
        int end   = Math.min(start + PAGE_SIZE, result.size());
        displayedLD.setValue(new ArrayList<>(result.subList(start, end)));
    }

    // Helpers
    private int    intVal(MutableLiveData<Integer> ld) { Integer v = ld.getValue(); return v != null ? v : 0; }
    private String strVal(MutableLiveData<String>  ld) { String  v = ld.getValue(); return v != null ? v : ""; }
}