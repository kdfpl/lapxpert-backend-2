package com.lapxpert.backend.hoadon.domain.service;

import com.lapxpert.backend.hoadon.domain.enums.TrangThaiDonHang;
import com.lapxpert.backend.hoadon.domain.enums.TrangThaiThanhToan;
import com.lapxpert.backend.hoadon.domain.enums.PhuongThucThanhToan;
import com.lapxpert.backend.hoadon.domain.enums.LoaiHoaDon;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.EnumSet;

/**
 * Order State Machine Service
 * Manages valid state transitions for orders
 */
@Slf4j
@Service
public class OrderStateMachineService {
    
    // Define valid state transitions
    private static final Map<TrangThaiDonHang, Set<TrangThaiDonHang>> VALID_TRANSITIONS = new HashMap<>();
    
    static {
        // From CHO_XAC_NHAN (Pending Confirmation)
        VALID_TRANSITIONS.put(TrangThaiDonHang.CHO_XAC_NHAN, EnumSet.of(
            TrangThaiDonHang.DANG_XU_LY,
            TrangThaiDonHang.DA_HUY
        ));
        
        // From DANG_XU_LY (Processing)
        VALID_TRANSITIONS.put(TrangThaiDonHang.DANG_XU_LY, EnumSet.of(
            TrangThaiDonHang.DANG_GIAO_HANG,
            TrangThaiDonHang.HOAN_THANH,
            TrangThaiDonHang.DA_HUY
        ));
        
        // From DANG_GIAO_HANG (Shipping)
        VALID_TRANSITIONS.put(TrangThaiDonHang.DANG_GIAO_HANG, EnumSet.of(
            TrangThaiDonHang.DA_GIAO_HANG,
            TrangThaiDonHang.DA_HUY
        ));
        
        // From DA_GIAO_HANG (Delivered)
        VALID_TRANSITIONS.put(TrangThaiDonHang.DA_GIAO_HANG, EnumSet.of(
            TrangThaiDonHang.HOAN_THANH,
            TrangThaiDonHang.YEU_CAU_TRA_HANG
        ));
        
        // From YEU_CAU_TRA_HANG (Return Requested)
        VALID_TRANSITIONS.put(TrangThaiDonHang.YEU_CAU_TRA_HANG, EnumSet.of(
            TrangThaiDonHang.DA_TRA_HANG,
            TrangThaiDonHang.HOAN_THANH
        ));
        
        // Terminal states (no transitions allowed)
        VALID_TRANSITIONS.put(TrangThaiDonHang.HOAN_THANH, EnumSet.noneOf(TrangThaiDonHang.class));
        VALID_TRANSITIONS.put(TrangThaiDonHang.DA_HUY, EnumSet.noneOf(TrangThaiDonHang.class));
        VALID_TRANSITIONS.put(TrangThaiDonHang.DA_TRA_HANG, EnumSet.noneOf(TrangThaiDonHang.class));
    }
    
    /**
     * Check if state transition is valid
     */
    public boolean isValidTransition(TrangThaiDonHang currentState, TrangThaiDonHang newState) {
        if (currentState == null || newState == null) {
            return false;
        }
        
        if (currentState.equals(newState)) {
            return true; // Same state is always valid
        }
        
        Set<TrangThaiDonHang> validNextStates = VALID_TRANSITIONS.get(currentState);
        return validNextStates != null && validNextStates.contains(newState);
    }
    
    /**
     * Get valid next states for current state
     */
    public Set<TrangThaiDonHang> getValidNextStates(TrangThaiDonHang currentState) {
        return VALID_TRANSITIONS.getOrDefault(currentState, EnumSet.noneOf(TrangThaiDonHang.class));
    }
    
    /**
     * Validate state transition with business rules
     */
    public StateTransitionResult validateTransition(TrangThaiDonHang currentState, TrangThaiDonHang newState,
                                                   TrangThaiThanhToan paymentStatus, LoaiHoaDon orderType,
                                                   PhuongThucThanhToan paymentMethod) {
        
        // Check basic state machine validity
        if (!isValidTransition(currentState, newState)) {
            return StateTransitionResult.invalid(
                String.format("Invalid state transition from %s to %s", currentState, newState)
            );
        }
        
        // Business rule validations
        if (newState == TrangThaiDonHang.DANG_XU_LY) {
            // Can only move to processing if payment is confirmed (except for COD)
            if (paymentMethod != PhuongThucThanhToan.COD && paymentStatus != TrangThaiThanhToan.DA_THANH_TOAN) {
                return StateTransitionResult.invalid("Cannot process order without confirmed payment");
            }
        }
        
        if (newState == TrangThaiDonHang.DANG_GIAO_HANG) {
            // Can only ship online orders
            if (orderType == LoaiHoaDon.TAI_QUAY) {
                return StateTransitionResult.invalid("POS orders cannot be shipped");
            }
        }
        
        if (newState == TrangThaiDonHang.HOAN_THANH) {
            // POS orders can complete directly, online orders need delivery confirmation
            if (orderType == LoaiHoaDon.ONLINE && currentState != TrangThaiDonHang.DA_GIAO_HANG) {
                return StateTransitionResult.invalid("Online orders must be delivered before completion");
            }
        }
        
        return StateTransitionResult.valid();
    }
    
    /**
     * State transition result
     */
    public static class StateTransitionResult {
        private final boolean valid;
        private final String errorMessage;
        
        private StateTransitionResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
        
        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
        
        public static StateTransitionResult valid() {
            return new StateTransitionResult(true, null);
        }
        
        public static StateTransitionResult invalid(String errorMessage) {
            return new StateTransitionResult(false, errorMessage);
        }
    }
}
