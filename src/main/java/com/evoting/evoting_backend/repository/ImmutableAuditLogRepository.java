package com.evoting.evoting_backend.repository;

import com.evoting.evoting_backend.model.ImmutableAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ImmutableAuditLogRepository extends JpaRepository<ImmutableAuditLog, Long> {
    
    Optional<ImmutableAuditLog> findTopByOrderByIdDesc();
    
    List<ImmutableAuditLog> findAllByOrderByIdAsc();
    
    List<ImmutableAuditLog> findTop100ByOrderByIdDesc();
    
    @Query("SELECT COUNT(a) FROM ImmutableAuditLog a WHERE a.eventType = :eventType")
    long countByEventType(String eventType);
    
    List<ImmutableAuditLog> findByServiceOrderByTimestampDesc(String service);
    
    @Query("SELECT a FROM ImmutableAuditLog a WHERE a.timestamp >= :startTime AND a.timestamp <= :endTime")
    List<ImmutableAuditLog> findByTimestampRange(java.time.LocalDateTime startTime, java.time.LocalDateTime endTime);
}