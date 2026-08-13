package com.EDJ.ArCash.Repository;

import com.EDJ.ArCash.Models.CardAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardAuditEventRepository extends JpaRepository<CardAuditEvent, Long> {
    List<CardAuditEvent> findTop30ByUser_IdOrderByIdDesc(Long userId);
}
