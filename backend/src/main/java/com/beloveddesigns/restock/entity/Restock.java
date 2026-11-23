package com.beloveddesigns.restock.entity;

import com.beloveddesigns.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "restocks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Restock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "expected_date", nullable = false)
    private LocalDate expectedDate;

    @Column(name = "received_at")
    private Instant receivedAt;

    @Column(length = 150)
    private String supplier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RestockStatus status = RestockStatus.PREVU;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @OneToMany(mappedBy = "restock", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RestockItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void addItem(RestockItem item) {
        item.setRestock(this);
        items.add(item);
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
