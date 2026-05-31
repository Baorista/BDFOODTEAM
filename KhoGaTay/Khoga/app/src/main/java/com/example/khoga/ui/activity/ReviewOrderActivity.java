package com.example.khoga.ui.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.example.khoga.R;
import com.example.khoga.adapter.SelectImageAdapter;
import com.example.khoga.adapter.SelectImageAdapter.MediaItem;
import com.example.khoga.model.Review;
import com.example.khoga.model.User;
import com.example.khoga.repository.ReviewRepository;
import com.example.khoga.util.FirebaseHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * ReviewOrderActivity - Màn hình đánh giá sản phẩm (hỗ trợ nhiều sản phẩm)
 *
 * Intent extras cần truyền vào:
 *   - "productIds"    : ArrayList<String> (bắt buộc)
 *   - "productNames"  : ArrayList<String> (bắt buộc)
 *   - "productImages" : ArrayList<String> (bắt buộc)
 *   - "orderId"       : String (bắt buộc)
 *
 * Hỗ trợ tương thích ngược với intent cũ (1 sản phẩm):
 *   - "productId"     : String
 *   - "productName"   : String
 *   - "productImage"  : String
 */
public class ReviewOrderActivity extends AppCompatActivity {

    private static final String CLOUDINARY_PRESET = "android_app_upload";

    // UI
    private LinearLayout layoutProductReviews;
    private MaterialButton btnSubmitReview;
    private ProgressBar progressBar;

    // Data
    private String orderId;
    private final List<ProductReviewData> productReviews = new ArrayList<>();
    private int currentPickingIndex = -1; // index sản phẩm đang chọn ảnh/video

    // Repository
    private ReviewRepository reviewRepository;

    // ================================================================
    // DỮ LIỆU ĐÁNH GIÁ CHO MỖI SẢN PHẨM
    // ================================================================

    private static class ProductReviewData {
        String productId;
        String productName;
        String productImage;
        int rating = 0;
        boolean alreadyReviewed = false;
        ImageView[] stars = new ImageView[5];
        List<MediaItem> mediaItems = new ArrayList<>();
        Uri selectedVideoUri = null;
        SelectImageAdapter imageAdapter;
        RecyclerView rvSelectedImages;
        EditText edtComment;
        View btnPickImage;
        View btnPickVideo;
        View rootView;
    }

    // ================================================================
    // LAUNCHERS (đăng ký 1 lần, dùng chung cho tất cả sản phẩm)
    // ================================================================

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null
                                && currentPickingIndex >= 0
                                && currentPickingIndex < productReviews.size()) {
                            handleImagePickResult(result.getData(), productReviews.get(currentPickingIndex));
                        }
                    }
            );

    private final ActivityResultLauncher<Intent> videoPickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null
                                && currentPickingIndex >= 0
                                && currentPickingIndex < productReviews.size()) {
                            ProductReviewData data = productReviews.get(currentPickingIndex);
                            data.selectedVideoUri = result.getData().getData();
                            data.mediaItems.add(new MediaItem(data.selectedVideoUri, true));
                            data.imageAdapter.notifyItemInserted(data.mediaItems.size() - 1);
                            data.rvSelectedImages.setVisibility(View.VISIBLE);
                        }
                    }
            );

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                        if (isGranted) {
                            openImagePicker();
                        } else {
                            Toast.makeText(this, "Cần quyền truy cập ảnh để thêm hình",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
            );

    private final ActivityResultLauncher<String> videoPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                        if (isGranted) {
                            openVideoPicker();
                        } else {
                            Toast.makeText(this, "Cần quyền truy cập video để thêm video",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
            );

    // ================================================================
    // LIFECYCLE
    // ================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_order);

        reviewRepository = new ReviewRepository();

        orderId = getIntent().getStringExtra("orderId");
        if (orderId == null) {
            Toast.makeText(this, "Thiếu thông tin đơn hàng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        parseProductsFromIntent();

        if (productReviews.isEmpty()) {
            Toast.makeText(this, "Thiếu thông tin sản phẩm", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        inflateProductReviewForms();
        setupSubmitButton();
        checkAlreadyReviewedAll();
    }

    // ================================================================
    // PARSE SẢN PHẨM TỪ INTENT
    // ================================================================

    private void parseProductsFromIntent() {
        ArrayList<String> ids = getIntent().getStringArrayListExtra("productIds");
        ArrayList<String> names = getIntent().getStringArrayListExtra("productNames");
        ArrayList<String> images = getIntent().getStringArrayListExtra("productImages");

        if (ids != null && names != null && images != null
                && ids.size() == names.size() && ids.size() == images.size()) {
            for (int i = 0; i < ids.size(); i++) {
                ProductReviewData data = new ProductReviewData();
                data.productId = ids.get(i);
                data.productName = names.get(i);
                data.productImage = images.get(i);
                productReviews.add(data);
            }
        } else {
            // Tương thích ngược: intent cũ chỉ truyền 1 sản phẩm
            String productId = getIntent().getStringExtra("productId");
            String productName = getIntent().getStringExtra("productName");
            String productImage = getIntent().getStringExtra("productImage");
            if (productId != null) {
                ProductReviewData data = new ProductReviewData();
                data.productId = productId;
                data.productName = productName;
                data.productImage = productImage;
                productReviews.add(data);
            }
        }
    }

    // ================================================================
    // INIT VIEWS
    // ================================================================

    private void initViews() {
        layoutProductReviews = findViewById(R.id.layoutProductReviews);
        btnSubmitReview = findViewById(R.id.btnSubmitReview);
        progressBar = findViewById(R.id.progressBar);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }

    // ================================================================
    // INFLATE FORM ĐÁNH GIÁ CHO MỖI SẢN PHẨM
    // ================================================================

    private void inflateProductReviewForms() {
        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < productReviews.size(); i++) {
            ProductReviewData data = productReviews.get(i);
            View itemView = inflater.inflate(R.layout.item_review_product, layoutProductReviews, false);
            data.rootView = itemView;

            // Product info
            ImageView imgProduct = itemView.findViewById(R.id.imgProduct);
            itemView.<android.widget.TextView>findViewById(R.id.tvProductName).setText(data.productName);
            if (data.productImage != null && !data.productImage.isEmpty()) {
                Glide.with(this)
                        .load(data.productImage)
                        .placeholder(R.drawable.bg_rounded_8dp)
                        .centerCrop()
                        .into(imgProduct);
            }

            // Stars
            data.stars[0] = itemView.findViewById(R.id.star1);
            data.stars[1] = itemView.findViewById(R.id.star2);
            data.stars[2] = itemView.findViewById(R.id.star3);
            data.stars[3] = itemView.findViewById(R.id.star4);
            data.stars[4] = itemView.findViewById(R.id.star5);

            final int productIndex = i;
            for (int s = 0; s < 5; s++) {
                final int starRating = s + 1;
                data.stars[s].setOnClickListener(v -> setRating(productIndex, starRating));
            }

            // Media picker
            data.rvSelectedImages = itemView.findViewById(R.id.rvSelectedImages);
            data.imageAdapter = new SelectImageAdapter(data.mediaItems, position -> {
                MediaItem removed = data.mediaItems.get(position);
                if (removed.isVideo) {
                    data.selectedVideoUri = null;
                }
                data.mediaItems.remove(position);
                data.imageAdapter.notifyItemRemoved(position);
                if (data.mediaItems.isEmpty()) {
                    data.rvSelectedImages.setVisibility(View.GONE);
                }
            });
            data.rvSelectedImages.setAdapter(data.imageAdapter);
            data.rvSelectedImages.setLayoutManager(
                    new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

            data.btnPickImage = itemView.findViewById(R.id.btnPickImage);
            data.btnPickVideo = itemView.findViewById(R.id.btnPickVideo);

            data.btnPickImage.setOnClickListener(v -> {
                long imageCount = data.mediaItems.stream().filter(m -> !m.isVideo).count();
                if (imageCount >= 5) {
                    Toast.makeText(this, "Tối đa 5 ảnh", Toast.LENGTH_SHORT).show();
                    return;
                }
                currentPickingIndex = productIndex;
                checkPermissionAndPickImage();
            });

            data.btnPickVideo.setOnClickListener(v -> {
                if (data.selectedVideoUri != null) {
                    Toast.makeText(this, "Chỉ được chọn 1 video", Toast.LENGTH_SHORT).show();
                    return;
                }
                currentPickingIndex = productIndex;
                checkPermissionAndPickVideo();
            });

            // Comment
            data.edtComment = itemView.findViewById(R.id.edtComment);

            // Ẩn divider dưới cùng cho sản phẩm cuối
            if (i == productReviews.size() - 1) {
                itemView.findViewById(R.id.dividerBottom).setVisibility(View.GONE);
            }

            layoutProductReviews.addView(itemView);
        }
    }

    // ================================================================
    // XỬ LÝ ĐÁNH GIÁ SAO (1-5) CHO TỪNG SẢN PHẨM
    // ================================================================

    private void setRating(int productIndex, int rating) {
        ProductReviewData data = productReviews.get(productIndex);
        data.rating = rating;
        for (int i = 0; i < 5; i++) {
            if (i < rating) {
                data.stars[i].setImageResource(R.drawable.ic_star_filled);
                data.stars[i].setTag(1);
            } else {
                data.stars[i].setImageResource(R.drawable.ic_star_outline);
                data.stars[i].setTag(0);
            }
        }
    }

    // ================================================================
    // CHỌN ẢNH / VIDEO
    // ================================================================

    private void checkPermissionAndPickImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
    }

    private void checkPermissionAndPickVideo() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED) {
                openVideoPicker();
            } else {
                videoPermissionLauncher.launch(Manifest.permission.READ_MEDIA_VIDEO);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                openVideoPicker();
            } else {
                videoPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        imagePickerLauncher.launch(Intent.createChooser(intent, "Chọn ảnh"));
    }

    private void openVideoPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        videoPickerLauncher.launch(Intent.createChooser(intent, "Chọn video"));
    }

    private void handleImagePickResult(Intent data, ProductReviewData productData) {
        long imageCount = productData.mediaItems.stream().filter(m -> !m.isVideo).count();
        if (data.getClipData() != null) {
            int count = data.getClipData().getItemCount();
            for (int i = 0; i < count && imageCount < 5; i++, imageCount++) {
                productData.mediaItems.add(new MediaItem(data.getClipData().getItemAt(i).getUri(), false));
            }
        } else if (data.getData() != null) {
            if (imageCount < 5) {
                productData.mediaItems.add(new MediaItem(data.getData(), false));
            }
        }
        productData.imageAdapter.notifyDataSetChanged();
        productData.rvSelectedImages.setVisibility(
                productData.mediaItems.isEmpty() ? View.GONE : View.VISIBLE);
    }

    // ================================================================
    // KIỂM TRA ĐÃ ĐÁNH GIÁ CHƯA (CHO TỪNG SẢN PHẨM)
    // ================================================================

    private void checkAlreadyReviewedAll() {
        for (int i = 0; i < productReviews.size(); i++) {
            ProductReviewData data = productReviews.get(i);
            reviewRepository.checkAlreadyReviewed(data.productId, orderId, alreadyReviewed -> {
                if (alreadyReviewed) {
                    data.alreadyReviewed = true;
                    disableProductReview(data);
                    checkAllReviewed();
                }
            });
        }
    }

    private void disableProductReview(ProductReviewData data) {
        data.edtComment.setEnabled(false);
        for (ImageView star : data.stars) star.setEnabled(false);
        data.btnPickImage.setEnabled(false);
        data.btnPickVideo.setEnabled(false);
        // Hiển thị trạng thái đã đánh giá
        data.rootView.setAlpha(0.5f);
    }

    private void checkAllReviewed() {
        boolean allReviewed = true;
        for (ProductReviewData data : productReviews) {
            if (!data.alreadyReviewed) {
                allReviewed = false;
                break;
            }
        }
        if (allReviewed) {
            btnSubmitReview.setEnabled(false);
            btnSubmitReview.setText("Đã đánh giá");
            Toast.makeText(this, "Bạn đã đánh giá tất cả sản phẩm rồi",
                    Toast.LENGTH_SHORT).show();
        }
    }

    // ================================================================
    // GỬI ĐÁNH GIÁ
    // Luồng: Validate → Lấy user info → Upload & Submit từng sản phẩm
    //        → Đánh dấu order reviewed → Kết thúc
    // ================================================================

    private void setupSubmitButton() {
        btnSubmitReview.setOnClickListener(v -> {
            // Validate: mỗi sản phẩm chưa đánh giá phải có rating
            boolean hasUnreviewed = false;
            for (ProductReviewData data : productReviews) {
                if (!data.alreadyReviewed) {
                    hasUnreviewed = true;
                    if (data.rating == 0) {
                        Toast.makeText(this, "Vui lòng chọn số sao cho: " + data.productName,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
            }

            if (!hasUnreviewed) {
                Toast.makeText(this, "Tất cả sản phẩm đã được đánh giá",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            if (firebaseUser == null) {
                Toast.makeText(this, "Vui lòng đăng nhập để đánh giá",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            setLoading(true);

            FirebaseHelper.getInstance().getUserRef(firebaseUser.getUid())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            User dbUser = snapshot.getValue(User.class);
                            String userName = "Ẩn danh";
                            String userAvatar = "";
                            if (dbUser != null) {
                                if (dbUser.getDisplayName() != null && !dbUser.getDisplayName().isEmpty()) {
                                    userName = dbUser.getDisplayName();
                                }
                                if (dbUser.getPhotoUrl() != null) {
                                    userAvatar = dbUser.getPhotoUrl();
                                }
                            }

                            // Bắt đầu xử lý từng sản phẩm tuần tự
                            processNextProduct(0, firebaseUser.getUid(), userName, userAvatar);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            setLoading(false);
                            Toast.makeText(ReviewOrderActivity.this,
                                    "Không tải được thông tin người dùng", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    /**
     * Xử lý upload media và submit review cho từng sản phẩm tuần tự.
     * Khi hết sản phẩm → hoàn tất.
     */
    private void processNextProduct(int index, String userId, String userName, String userAvatar) {
        // Tìm sản phẩm tiếp theo chưa đánh giá
        while (index < productReviews.size() && productReviews.get(index).alreadyReviewed) {
            index++;
        }

        if (index >= productReviews.size()) {
            // Đã xử lý hết tất cả sản phẩm
            setLoading(false);
            Toast.makeText(this, "Đánh giá thành công! Cảm ơn bạn.",
                    Toast.LENGTH_LONG).show();
            setResult(RESULT_OK);
            finish();
            return;
        }

        ProductReviewData data = productReviews.get(index);

        Review review = new Review();
        review.setUserId(userId);
        review.setUserName(userName);
        review.setUserAvatar(userAvatar);
        review.setOrderId(orderId);
        review.setProductId(data.productId);
        review.setRating(data.rating);
        review.setComment(data.edtComment.getText().toString().trim());

        final int nextIndex = index + 1;
        uploadVideoToCloudinary(review, data, nextIndex, userId, userName, userAvatar);
    }

    /**
     * Bước 1: Upload video lên Cloudinary (nếu có), sau đó chuyển sang upload ảnh.
     */
    private void uploadVideoToCloudinary(Review review, ProductReviewData data,
                                          int nextIndex, String userId,
                                          String userName, String userAvatar) {
        if (data.selectedVideoUri == null) {
            review.setVideoUrl("");
            uploadImagesToCloudinary(review, data, nextIndex, userId, userName, userAvatar);
            return;
        }

        MediaManager.get()
                .upload(data.selectedVideoUri)
                .unsigned(CLOUDINARY_PRESET)
                .option("resource_type", "video")
                .callback(new UploadCallback() {
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        review.setVideoUrl((String) resultData.get("secure_url"));
                        uploadImagesToCloudinary(review, data, nextIndex, userId, userName, userAvatar);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        setLoading(false);
                        Toast.makeText(ReviewOrderActivity.this,
                                "Upload video thất bại (" + data.productName + "): "
                                        + error.getDescription(),
                                Toast.LENGTH_LONG).show();
                    }

                    @Override public void onReschedule(String requestId, ErrorInfo error) {}
                })
                .dispatch(this);
    }

    /**
     * Bước 2: Upload từng ảnh lên Cloudinary song song, gom URL rồi submit.
     */
    private void uploadImagesToCloudinary(Review review, ProductReviewData data,
                                           int nextIndex, String userId,
                                           String userName, String userAvatar) {
        List<Uri> imageUris = new ArrayList<>();
        for (MediaItem item : data.mediaItems) {
            if (!item.isVideo) imageUris.add(item.uri);
        }

        if (imageUris.isEmpty()) {
            review.setImages(new ArrayList<>());
            submitReview(review, nextIndex, userId, userName, userAvatar);
            return;
        }

        List<String> uploadedUrls = Collections.synchronizedList(new ArrayList<>());
        final int[] remaining = {imageUris.size()};
        final boolean[] hasFailed = {false};

        for (Uri uri : imageUris) {
            MediaManager.get()
                    .upload(uri)
                    .unsigned(CLOUDINARY_PRESET)
                    .callback(new UploadCallback() {
                        @Override public void onStart(String requestId) {}
                        @Override public void onProgress(String requestId, long bytes, long totalBytes) {}

                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            if (!hasFailed[0]) {
                                uploadedUrls.add((String) resultData.get("secure_url"));
                                remaining[0]--;
                                if (remaining[0] == 0) {
                                    review.setImages(new ArrayList<>(uploadedUrls));
                                    submitReview(review, nextIndex, userId, userName, userAvatar);
                                }
                            }
                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            if (!hasFailed[0]) {
                                hasFailed[0] = true;
                                setLoading(false);
                                Toast.makeText(ReviewOrderActivity.this,
                                        "Upload ảnh thất bại (" + data.productName + "): "
                                                + error.getDescription(),
                                        Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override public void onReschedule(String requestId, ErrorInfo error) {}
                    })
                    .dispatch(this);
        }
    }

    /**
     * Bước 3: Lưu review vào Firebase, sau đó xử lý sản phẩm tiếp theo.
     */
    private void submitReview(Review review, int nextIndex,
                               String userId, String userName, String userAvatar) {
        reviewRepository.submitReview(review,
                new ReviewRepository.OnReviewSubmitListener() {
                    @Override
                    public void onSuccess() {
                        processNextProduct(nextIndex, userId, userName, userAvatar);
                    }

                    @Override
                    public void onFailure(String error) {
                        setLoading(false);
                        Toast.makeText(ReviewOrderActivity.this, error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ================================================================
    // HELPERS
    // ================================================================

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSubmitReview.setEnabled(!loading);
        btnSubmitReview.setText(loading ? "Đang gửi..." : "Gửi đánh giá");
    }
}
