package com.hms.hospital.entity;

import com.hms.tenant.entity.TenantAwareEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "departments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Department extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "department_type", length = 50)
    @Builder.Default
    private DepartmentType departmentType = DepartmentType.CLINICAL;

    @Column(name = "active")
    @Builder.Default
    private boolean active = true;

    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Room> rooms = new ArrayList<>();

    public enum DepartmentType {
        CLINICAL, NON_CLINICAL, ADMINISTRATIVE, DIAGNOSTIC, EMERGENCY
    }
}
