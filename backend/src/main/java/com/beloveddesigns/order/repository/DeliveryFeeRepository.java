package com.beloveddesigns.order.repository;

import com.beloveddesigns.order.entity.DeliveryFee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeliveryFeeRepository extends JpaRepository<DeliveryFee, Long> {

    Optional<DeliveryFee> findByCityKey(String cityKey);
}
