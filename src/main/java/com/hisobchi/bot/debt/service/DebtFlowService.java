package com.hisobchi.bot.debt.service;

import com.hisobchi.bot.debt.entity.DebtType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DebtFlowService {

    public enum FlowType {
        PAY_BORROWED,
        RETURN_LENT,
        CREATE_BORROW,
        CREATE_LEND
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DebtSession {
        private FlowType flowType;
        private Long debtId;
        private String personName;
        private BigDecimal initialAmount;
        private BigDecimal paymentAmount;
        private String paymentMethod;
        private LocalDate dueDate;
        private boolean isFull;
    }

    private final ConcurrentHashMap<Long, DebtSession> sessions = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Long, DebtType> debtTypes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, String> debtPersons = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, BigDecimal> debtAmounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, LocalDate> debtDates = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, String> paymentMethods = new ConcurrentHashMap<>();

    // Partial/Full payment flow state
    private final ConcurrentHashMap<Long, Long> selectedDebtIds = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, BigDecimal> paymentAmounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, String> flowPaymentMethods = new ConcurrentHashMap<>();

    // Draft / Edit state
    private final ConcurrentHashMap<Long, Long> activeDraftIds = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Long> extendingDebtIds = new ConcurrentHashMap<>();

    public DebtSession startSession(Long userId, FlowType flowType, Long debtId, String personName, BigDecimal initialAmount) {
        DebtSession session = DebtSession.builder()
                .flowType(flowType)
                .debtId(debtId)
                .personName(personName)
                .initialAmount(initialAmount)
                .build();
        sessions.put(userId, session);
        return session;
    }

    public DebtSession getSession(Long userId) {
        return sessions.get(userId);
    }

    public void clearSession(Long userId) {
        sessions.remove(userId);
    }

    public void clear(Long userId) {
        sessions.remove(userId);
        debtTypes.remove(userId);
        debtPersons.remove(userId);
        debtAmounts.remove(userId);
        debtDates.remove(userId);
        paymentMethods.remove(userId);
        selectedDebtIds.remove(userId);
        paymentAmounts.remove(userId);
        flowPaymentMethods.remove(userId);
        activeDraftIds.remove(userId);
        extendingDebtIds.remove(userId);
    }

    public void setDebtType(Long userId, DebtType type) { debtTypes.put(userId, type); }
    public DebtType getDebtType(Long userId) { return debtTypes.get(userId); }

    public void setDebtPerson(Long userId, String person) { debtPersons.put(userId, person); }
    public String getDebtPerson(Long userId) { return debtPersons.get(userId); }

    public void setDebtAmount(Long userId, BigDecimal amount) { debtAmounts.put(userId, amount); }
    public BigDecimal getDebtAmount(Long userId) { return debtAmounts.get(userId); }

    public void setDebtDate(Long userId, LocalDate date) { debtDates.put(userId, date); }
    public LocalDate getDebtDate(Long userId) { return debtDates.get(userId); }

    public void setPaymentMethod(Long userId, String method) { paymentMethods.put(userId, method); }
    public String getPaymentMethod(Long userId) { return paymentMethods.get(userId); }

    public void setSelectedDebtId(Long userId, Long debtId) { selectedDebtIds.put(userId, debtId); }
    public Long getSelectedDebtId(Long userId) { return selectedDebtIds.get(userId); }

    public void setPaymentAmount(Long userId, BigDecimal amount) { paymentAmounts.put(userId, amount); }
    public BigDecimal getPaymentAmount(Long userId) { return paymentAmounts.get(userId); }

    public void setFlowPaymentMethod(Long userId, String method) { flowPaymentMethods.put(userId, method); }
    public String getFlowPaymentMethod(Long userId) { return flowPaymentMethods.get(userId); }

    public void setActiveDraftId(Long userId, Long draftId) { activeDraftIds.put(userId, draftId); }
    public Long getActiveDraftId(Long userId) { return activeDraftIds.get(userId); }

    public void setExtendingDebtId(Long userId, Long debtId) { extendingDebtIds.put(userId, debtId); }
    public Long getExtendingDebtId(Long userId) { return extendingDebtIds.get(userId); }
}
