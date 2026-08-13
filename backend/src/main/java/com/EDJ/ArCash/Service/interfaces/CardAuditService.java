package com.EDJ.ArCash.Service.interfaces;

import com.EDJ.ArCash.Models.CardAuditEvent;
import com.EDJ.ArCash.Models.Imp.CardAuditType;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Models.VirtualCard;
import java.util.List;

public interface CardAuditService {
    void record(User user, VirtualCard card, CardAuditType type, String meta);

    List<CardAuditEvent> latestForUser(Long userId);
}
