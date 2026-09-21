package com.hms.hospital.repository;

import com.hms.hospital.entity.StaffHospitalMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StaffHospitalMapRepository extends JpaRepository<StaffHospitalMap, Long> {

    Optional<StaffHospitalMap> findByEmail(String email);

    boolean existsByEmail(String email);
}
