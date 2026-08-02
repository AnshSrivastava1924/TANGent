package com.portfolio.management.service;

import com.portfolio.management.dto.request.TransactionRequest;
import com.portfolio.management.dto.response.TransactionResponse;

import java.util.List;

public interface TransactionService {

    TransactionResponse createTransaction(TransactionRequest request);

    TransactionResponse getTransactionById(Long transactionId);

    List<TransactionResponse> getTransactionsByAsset(Long assetId);

    TransactionResponse updateTransaction(Long transactionId,
                                          TransactionRequest request);

    void deleteTransaction(Long transactionId);
}