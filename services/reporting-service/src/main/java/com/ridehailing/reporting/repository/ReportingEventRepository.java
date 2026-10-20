package com.ridehailing.reporting.repository;

import com.ridehailing.reporting.entity.ReportingEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportingEventRepository extends JpaRepository<ReportingEventEntity, Long> {

    Optional<ReportingEventEntity> findByEventId(String eventId);

    boolean existsByEventId(String eventId);

    List<ReportingEventEntity> findTop50ByOrderByOccurredAtDesc();
}
