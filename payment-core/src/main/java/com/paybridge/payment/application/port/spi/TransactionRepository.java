package com.paybridge.payment.application.port.spi;

import com.paybridge.payment.domain.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}