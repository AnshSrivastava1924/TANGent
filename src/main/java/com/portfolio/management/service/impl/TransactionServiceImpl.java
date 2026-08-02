package com.portfolio.management.service;

import com.portfolio.management.dto.request.TransactionRequest;
import com.portfolio.management.dto.response.TransactionResponse;
import com.portfolio.management.model.Transaction;
import com.portfolio.management.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;

    @Override
    public TransactionResponse createTransaction(TransactionRequest request) {

        Transaction transaction = Transaction.builder()
                .assetId(request.getAssetId())
                .transactionType(request.getTransactionType())
                .quantity(request.getQuantity())
                .pricePerUnit(request.getPricePerUnit())
                .totalAmount(request.getTotalAmount())
                .transactionDate(request.getTransactionDate())
                .notes(request.getNotes())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Transaction saved = transactionRepository.save(transaction);

        return mapToResponse(saved);
    }

    @Override
    public TransactionResponse getTransactionById(Long transactionId) {

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        return mapToResponse(transaction);
    }

    @Override
    public List<TransactionResponse> getTransactionsByAsset(Long assetId) {

        return transactionRepository.findByAssetId(assetId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public TransactionResponse updateTransaction(Long transactionId,
                                                 TransactionRequest request) {

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        transaction.setAssetId(request.getAssetId());
        transaction.setTransactionType(request.getTransactionType());
        transaction.setQuantity(request.getQuantity());
        transaction.setPricePerUnit(request.getPricePerUnit());
        transaction.setTotalAmount(request.getTotalAmount());
        transaction.setTransactionDate(request.getTransactionDate());
        transaction.setNotes(request.getNotes());
        transaction.setUpdatedAt(LocalDateTime.now());

        Transaction updated = transactionRepository.save(transaction);

        return mapToResponse(updated);
    }

    @Override
    public void deleteTransaction(Long transactionId) {

        transactionRepository.deleteById(transactionId);
    }

    private TransactionResponse mapToResponse(Transaction transaction) {

        return TransactionResponse.builder()
                .transactionId(transaction.getTransactionId())
                .assetId(transaction.getAssetId())
                .transactionType(transaction.getTransactionType())
                .quantity(transaction.getQuantity())
                .pricePerUnit(transaction.getPricePerUnit())
                .totalAmount(transaction.getTotalAmount())
                .transactionDate(transaction.getTransactionDate())
                .notes(transaction.getNotes())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .build();
    }
}