package com.evoting.evoting_backend.repository;

import com.evoting.evoting_backend.model.Election;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ElectionRepository extends JpaRepository<Election, Long> {
}