package com.ridehailing.driver.repository;

import com.ridehailing.driver.entity.DriverEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpringDataDriverRepository extends JpaRepository<DriverEntity, Long> {

    Optional<DriverEntity> findByUserId(Long userId);

    Optional<DriverEntity> findByEmailIgnoreCase(String email);

    Optional<DriverEntity> findByLicensePlateIgnoreCase(String licensePlate);

    boolean existsByUserId(Long userId);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByLicensePlateIgnoreCase(String licensePlate);
}
