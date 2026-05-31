package com.example.khoga.ui.activity;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.example.khoga.R;

/**
 * CancelOrderDialog — Dialog chọn lý do hủy đơn (theo Figma)
 *
 * Figma:
 * ┌────────────────────────────────────┐
 * │  Lý do hủy                    [X] │
 * │                                    │
 * │  ● Tôi mua nhầm sản phẩm          │
 * │  ○ Tôi tìm thấy sản phẩm khác    │
 * │    giá rẻ hơn                      │
 * │  ○ Khác                            │
 * │                                    │
 * │      [   Xác nhận hủy   ]         │
 * └────────────────────────────────────┘
 */
public class CancelOrderDialog extends DialogFragment {

    private static final String ARG_ORDER_ID = "orderId";
    private OnCancelConfirmListener listener;

    public interface OnCancelConfirmListener {
        void onConfirm(String orderId, String reason);
    }

    public static CancelOrderDialog newInstance(String orderId) {
        CancelOrderDialog dialog = new CancelOrderDialog();
        Bundle args = new Bundle();
        args.putString(ARG_ORDER_ID, orderId);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnCancelConfirmListener(OnCancelConfirmListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Dialog style không có title bar
        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_DeviceDefault_Light_Dialog_Alert);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_cancel_order, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String orderId = getArguments() != null
                ? getArguments().getString(ARG_ORDER_ID) : "";

        RadioGroup radioGroup = view.findViewById(R.id.radioGroupReasons);
        MaterialButton btnConfirm = view.findViewById(R.id.btnConfirmCancel);
        ImageButton btnClose = view.findViewById(R.id.btnCloseDialog);

        // Nút X đóng dialog
        btnClose.setOnClickListener(v -> dismiss());

        // Xác nhận hủy
        btnConfirm.setOnClickListener(v -> {
            int selectedId = radioGroup.getCheckedRadioButtonId();
            if (selectedId == -1) return; // Chưa chọn

            RadioButton selected = view.findViewById(selectedId);
            String reason = selected.getText().toString();

            if (listener != null) {
                listener.onConfirm(orderId, reason);
            }
            dismiss();
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // Dialog chiếm 90% width
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.9),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_dialog_rounded);
        }
    }
}
