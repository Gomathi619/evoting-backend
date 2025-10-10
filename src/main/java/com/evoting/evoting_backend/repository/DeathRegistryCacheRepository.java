package com.evoting.evoting_backend.repository;

import com.evoting.evoting_backend.model.DeathRegistryCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface DeathRegistryCacheRepository extends JpaRepository<DeathRegistryCache, String> {
    
    @Query("SELECT c FROM DeathRegistryCache c WHERE c.expiresAt < :now")
    List<DeathRegistryCache> findExpiredEntries(@Param("now") LocalDateTime now);
    
    @Query("SELECT COUNT(c) FROM DeathRegistryCache c WHERE c.isAlive = true")
    long countAliveEntries();
    
    @Query("SELECT COUNT(c) FROM DeathRegistryCache c WHERE c.isAlive = false")
    long countDeceasedEntries();
}