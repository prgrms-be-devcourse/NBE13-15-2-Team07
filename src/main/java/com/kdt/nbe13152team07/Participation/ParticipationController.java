package com.kdt.nbe13152team07.Participation;

import com.kdt.nbe13152team07.Expense.ExpenseRepository;
import com.kdt.nbe13152team07.Expense.ExpenseResponse;
import com.kdt.nbe13152team07.Expense.ExpenseService;
import com.kdt.nbe13152team07.Expense.ExpenseViewRequest;
import com.kdt.nbe13152team07.global.common.PageResponse;
import com.kdt.nbe13152team07.global.security.LoginUser;
import com.kdt.nbe13152team07.room.ParticipationRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/participations")
public class ParticipationController {
    private final ParticipationService participationService;
    @Autowired
    public ParticipationController(ParticipationService participationService, ExpenseService expenseService) {
        this.participationService = participationService;
    }

}
