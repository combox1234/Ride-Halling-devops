package com.ridehailing.pricing.repository;

import com.ridehailing.pricing.entity.PricingZoneEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PricingZoneRepository extends JpaRepository<PricingZoneEntity, Long> {

    Optional<PricingZoneEntity> findByZoneCode(String zoneCode);

    List<PricingZoneEntity> findByActiveTrue();
}
