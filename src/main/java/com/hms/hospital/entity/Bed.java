package com.hms.hospital.entity;

import com.hms.tenant.entity.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "beds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bed extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "bed_number", nullable = false, length = 50)
    private String bedNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30)
    @Builder.Default
    private BedStatus status = BedStatus.AVAILABLE;

    public enum BedStatus {
        AVAILABLE, OCCUPIED, UNDER_MAINTENANCE, RESERVED, CLEANING
    }
}
