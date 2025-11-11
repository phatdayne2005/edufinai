package vn.uth.financeservice.service;

import lombok.AllArgsConstructor;
import vn.uth.financeservice.dto.ExpenseRequestDto;
import vn.uth.financeservice.entity.Expense;
import vn.uth.financeservice.entity.ExpenseType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.uth.financeservice.repository.ExpenseRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;

    public ExpenseService(ExpenseRepository expenseRepository){
        this.expenseRepository = expenseRepository;
    }

    @Transactional
    public Expense createExpense(ExpenseRequestDto request, UUID userId) {
        Expense e = new Expense();
        e.setExpenseId(UUID.randomUUID());
        e.setUserId(userId);
        e.setType(ExpenseType.valueOf(request.getType()));
        e.setAmount(request.getAmount());
        e.setCategory(request.getCategory());
        e.setNote(request.getNote());
        e.setLinkedAccount(request.getLinkedAccount());
        e.setGoalId(request.getGoalId());
        e.setStatus("ACTIVE");
        e.setCreatedAt(LocalDateTime.now());
        e.setUpdatedAt(LocalDateTime.now());
        return expenseRepository.save(e);
    }

    @Transactional(readOnly = true)
    public List<Expense> getExpenseHistory(UUID userId) {
        return expenseRepository.findByUserId(userId);
    }
}

