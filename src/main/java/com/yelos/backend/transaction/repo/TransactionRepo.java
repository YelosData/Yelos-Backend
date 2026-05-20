package com.yelos.backend.transaction.repo;

import com.yelos.backend.transaction.models.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepo extends JpaRepository<Transaction, Long> {

}
