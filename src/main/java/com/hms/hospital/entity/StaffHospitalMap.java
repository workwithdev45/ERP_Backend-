package com.hms.hospital.entity;

import com.hms.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "staff_hospital_map")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffHospitalMap extends BaseEntity {

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "hospital_ids", nullable = false, columnDefinition = "TEXT")
    private String hospitalIds; // Comma-separated or JSON list of portalIds
}
