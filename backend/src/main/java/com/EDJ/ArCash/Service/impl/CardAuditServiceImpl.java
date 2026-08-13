package com.EDJ.ArCash.Service.impl;
import com.EDJ.ArCash.Service.interfaces.CardAuditService;

import com.EDJ.ArCash.Models.CardAuditEvent;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Models.VirtualCard;
import com.EDJ.ArCash.Models.Imp.CardAuditType;
import com.EDJ.ArCash.Repository.CardAuditEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CardAuditServiceImpl implements CardAuditService {

    private final CardAuditEventRepository auditRepository;

    public CardAuditServiceImpl(CardAuditEventRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @Transactional
    public void record(User user, VirtualCard card, CardAuditType type, String meta) {
        CardAuditEvent event = new CardAuditEvent();
        event.setUser(user);
        event.setCard(card);
        event.setEventType(type);
        event.setMeta(meta);
        auditRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<CardAuditEvent> latestForUser(Long userId) {
        return auditRepository.findTop30ByUser_IdOrderByIdDesc(userId);
    }
}
