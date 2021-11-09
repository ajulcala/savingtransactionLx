package com.savingtransaction.app.models.dao;

import com.savingtransaction.app.models.documents.Transaction;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface TransactionDao extends ReactiveMongoRepository<Transaction, String> {
    Flux<Transaction> findBySavingAccountId(String id);
}
