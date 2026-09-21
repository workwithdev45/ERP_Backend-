package com.hms.hospital.repository;

import com.hms.hospital.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByTenantIdAndDepartmentId(String tenantId, Long departmentId);
    Optional<Room> findByTenantIdAndDepartmentIdAndRoomNumber(String tenantId, Long departmentId, String roomNumber);
}
