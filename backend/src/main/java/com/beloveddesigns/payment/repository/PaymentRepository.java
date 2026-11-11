package com.beloveddesigns.payment.repository;

import com.beloveddesigns.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
