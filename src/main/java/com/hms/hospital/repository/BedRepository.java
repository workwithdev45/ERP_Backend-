package com.hms.hospital.repository;

import com.hms.hospital.entity.Bed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BedRepository extends JpaRepository<Bed, Long> {
    List<Bed> findByTenantIdAndRoomId(String tenantId, Long roomId);
    List<Bed> findByTenantIdAndStatus(String tenantId, Bed.BedStatus status);
    Optional<Bed> findByTenantIdAndRoomIdAndBedNumber(String tenantId, Long roomId, String bedNumber);
}
