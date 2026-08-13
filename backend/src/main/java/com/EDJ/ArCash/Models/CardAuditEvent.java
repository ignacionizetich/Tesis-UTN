package com.EDJ.ArCash.Models;

import com.EDJ.ArCash.Models.Imp.CardAuditType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "card_audit_events")
public class CardAuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "card_id")
    private VirtualCard card;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private CardAuditType eventType;

    @Column(name = "meta", length = 255)
    private String meta;

    @Column(name = "created_at")
    private String createdAt;

    @PrePersist
    private void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
    }
}
