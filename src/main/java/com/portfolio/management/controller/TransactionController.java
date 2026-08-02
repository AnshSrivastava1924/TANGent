package com.portfolio.management.controller;

import com.portfolio.management.dto.request.TransactionRequest;
import com.portfolio.management.dto.response.TransactionResponse;
import com.portfolio.management.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(
            @Valid @RequestBody TransactionRequest request) {

        return new ResponseEntity<>(
                transactionService.createTransaction(request),
                HttpStatus.CREATED);
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransactionById(
            @PathVariable Long transactionId) {

        return ResponseEntity.ok(
                transactionService.getTransactionById(transactionId));
    }

    @GetMapping("/asset/{assetId}")
    public ResponseEntity<List<TransactionResponse>> getTransactionsByAsset(
            @PathVariable Long assetId) {

        return ResponseEntity.ok(
                transactionService.getTransactionsByAsset(assetId));
    }

    @PutMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> updateTransaction(
            @PathVariable Long transactionId,
            @Valid @RequestBody TransactionRequest request) {

        return ResponseEntity.ok(
                transactionService.updateTransaction(transactionId, request));
    }

    @DeleteMapping("/{transactionId}")
    public ResponseEntity<String> deleteTransaction(
            @PathVariable Long transactionId) {

        transactionService.deleteTransaction(transactionId);

        return ResponseEntity.ok("Transaction deleted successfully");
    }
}