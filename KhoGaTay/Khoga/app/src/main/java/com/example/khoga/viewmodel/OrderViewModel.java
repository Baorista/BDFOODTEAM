package com.example.khoga.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.khoga.model.Order;
import com.example.khoga.repository.OrderRepository;

import java.util.List;

public class OrderViewModel extends AndroidViewModel {

    private final OrderRepository orderRepository;

    private final MutableLiveData<List<Order>> userOrders = new MutableLiveData<>();
    private final MutableLiveData<Order> selectedOrder = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> cancelSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> orderCreated = new MutableLiveData<>();

    public OrderViewModel(@NonNull Application application) {
        super(application);
        orderRepository = new OrderRepository();
    }

    // ===== Getters =====

    public MutableLiveData<List<Order>> getUserOrders() { return userOrders; }
    public MutableLiveData<Order> getSelectedOrder() { return selectedOrder; }
    public MutableLiveData<Boolean> getIsLoading() { return isLoading; }
    public MutableLiveData<Boolean> getCancelSuccess() { return cancelSuccess; }
    public MutableLiveData<String> getErrorMessage() { return errorMessage; }
    public MutableLiveData<Boolean> getOrderCreated() { return orderCreated; }

    // ===== Actions =====

    public void loadUserOrders(String userId) {
        orderRepository.getOrdersByUser(userId, userOrders);
    }

    public void loadOrderById(String orderId) {
        orderRepository.getOrderById(orderId, selectedOrder);
    }

    public void cancelOrder(String orderId, String reason) {
        orderRepository.cancelOrder(orderId, reason,
                unused -> cancelSuccess.postValue(true),
                e -> errorMessage.postValue(e.getMessage()));
    }
}
