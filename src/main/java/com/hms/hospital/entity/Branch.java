package com.hms.hospital.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hms.tenant.context.TenantContext;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "branches")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Branch implements Serializable, Persistable<String> {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "tenant_id", nullable = false, updatable = false, length = 50)
    private String tenantId;

    @Column(name = "branch_name", nullable = false, length = 150)
    private String branchName;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "active")
    @Builder.Default
    private boolean active = true;

    @OneToMany(mappedBy = "branch", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Department> departments = new ArrayList<>();

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Kolkata")
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Kolkata")
    @LastModifiedDate
    @Column(name = "updated_at")
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @Version
    @Column(name = "version")
    private Long version;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostPersist
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }

    public String getName() {
        return branchName;
    }

    public void setName(String name) {
        this.branchName = name;
    }

    @PrePersist
    public void prePersistTenant() {
        if (this.tenantId == null) {
            String current = TenantContext.getTenantId();
            this.tenantId = current != null ? current : "hms-main";
        }
    }

    @PreUpdate
    public void preUpdateTenant() {
        if (this.tenantId == null) {
            String current = TenantContext.getTenantId();
            this.tenantId = current != null ? current : "hms-main";
        }
    }
}
