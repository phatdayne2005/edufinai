package vn.uth.financeservice.controller;

import vn.uth.financeservice.dto.ExpenseRequestDto;
import vn.uth.financeservice.entity.Expense;
import vn.uth.financeservice.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/expense")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    public ResponseEntity<?> create(@Validated @RequestBody ExpenseRequestDto request) {
        // TODO: lấy userId từ JWT; tạm thời fake user cho demo
        UUID userId = UUID.nameUUIDFromBytes("demo-user".getBytes());
        Expense e = expenseService.createExpense(request, userId);
        Map<String, Object> resp = new HashMap<>();
        resp.put("expense_id", e.getExpenseId());
        resp.put("status", "SUCCESS");
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("id") UUID id, @Validated @RequestBody ExpenseRequestDto request) {
        UUID userId = UUID.nameUUIDFromBytes("demo-user".getBytes());
        Expense e = expenseService.updateExpense(id, request, userId);
        Map<String, Object> resp = new HashMap<>();
        resp.put("expense_id", e.getExpenseId());
        resp.put("status", "SUCCESS");
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/history")
    public ResponseEntity<?> history() {
        UUID userId = UUID.nameUUIDFromBytes("demo-user".getBytes());
        return ResponseEntity.ok(expenseService.getExpenseHistory(userId));
    }
}
