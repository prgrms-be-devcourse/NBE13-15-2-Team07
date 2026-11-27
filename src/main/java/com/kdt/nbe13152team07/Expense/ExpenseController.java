package com.kdt.nbe13152team07.Expense;

import com.kdt.nbe13152team07.global.common.PageResponse;
import com.kdt.nbe13152team07.global.security.LoginUser;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {
    private final ExpenseService expenseService;
    @Autowired
    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }
    //E2 지출 목록 확인
    @GetMapping
    public ResponseEntity<PageResponse<ExpenseResponse>> findMyExpenses(
            @AuthenticationPrincipal LoginUser loginUser,
            @Valid @ModelAttribute ExpenseViewRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(
                expenseService.findMyExpenses(loginUser.userId(), request, page, size));
    }
}
