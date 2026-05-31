package com.example.khoga.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.khoga.R;
import com.example.khoga.model.Address;

import java.util.List;

public class AddressAdapter extends RecyclerView.Adapter<AddressAdapter.AddressViewHolder> {

    private List<Address> addressList;
    private OnAddressClickListener listener;

    public interface OnAddressClickListener {
        void onEditClick(Address address);
        void onDeleteClick(Address address);
    }

    public AddressAdapter(List<Address> addressList, OnAddressClickListener listener) {
        this.addressList = addressList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AddressViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_address, parent, false);
        return new AddressViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AddressViewHolder holder, int position) {
        Address address = addressList.get(position);
        holder.tvName.setText("Họ và tên: " + address.recipientName);
        holder.tvPhone.setText("Số điện thoại: " + address.phone);
        holder.tvAddress.setText("Địa chỉ: " + address.detail);

        holder.tvDefaultBadge.setVisibility(address.isDefault ? View.VISIBLE : View.GONE);

        holder.btnEdit.setOnClickListener(v -> listener.onEditClick(address));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(address));
    }

    @Override
    public int getItemCount() {
        return addressList.size();
    }

    public static class AddressViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPhone, tvAddress, tvDefaultBadge;
        ImageView btnEdit, btnDelete;

        public AddressViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvPhone = itemView.findViewById(R.id.tvPhone);
            tvAddress = itemView.findViewById(R.id.tvAddress);
            tvDefaultBadge = itemView.findViewById(R.id.tvDefaultBadge);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}