package com.hms.hospital.service;

import com.hms.hospital.dto.BedDto;
import com.hms.hospital.dto.BranchCreateRequest;
import com.hms.hospital.dto.BranchDto;
import com.hms.hospital.dto.BranchResponse;
import com.hms.hospital.dto.DepartmentDto;
import com.hms.hospital.dto.RoomDto;

import java.util.List;

public interface HospitalStructureService {
    // Branches
    BranchResponse createBranch(BranchCreateRequest request);
    List<BranchResponse> getAllBranchResponses();
    BranchResponse getBranchResponseById(String id);

    BranchDto createBranch(BranchDto branchDto);
    List<BranchDto> getAllBranches();
    BranchDto getBranchById(String id);

    // Departments
    DepartmentDto createDepartment(DepartmentDto departmentDto);
    List<DepartmentDto> getDepartmentsByBranch(String branchId);
    DepartmentDto getDepartmentById(Long id);

    // Rooms
    RoomDto createRoom(RoomDto roomDto);
    List<RoomDto> getRoomsByDepartment(Long departmentId);
    RoomDto getRoomById(Long id);

    // Beds
    BedDto createBed(BedDto bedDto);
    List<BedDto> getBedsByRoom(Long roomId);
    BedDto getBedById(Long id);
    BedDto updateBedStatus(Long id, String status);
}
