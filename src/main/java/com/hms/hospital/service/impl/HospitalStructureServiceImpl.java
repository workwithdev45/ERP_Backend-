package com.hms.hospital.service.impl;

import com.hms.common.exception.BadRequestException;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.hospital.dto.BedDto;
import com.hms.hospital.dto.BranchCreateRequest;
import com.hms.hospital.dto.BranchDto;
import com.hms.hospital.dto.BranchResponse;
import com.hms.hospital.dto.DepartmentDto;
import com.hms.hospital.dto.RoomDto;
import com.hms.hospital.entity.Bed;
import com.hms.hospital.entity.Branch;
import com.hms.hospital.entity.Department;
import com.hms.hospital.entity.Room;
import com.hms.hospital.repository.BedRepository;
import com.hms.hospital.repository.BranchRepository;
import com.hms.hospital.repository.DepartmentRepository;
import com.hms.hospital.repository.RoomRepository;
import com.hms.hospital.service.HospitalStructureService;
import com.hms.tenant.context.TenantContext;
import com.hms.tenant.service.TenantResolverService;
import com.hms.user.entity.User;
import com.hms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HospitalStructureServiceImpl implements HospitalStructureService {

    private final BranchRepository branchRepository;
    private final DepartmentRepository departmentRepository;
    private final RoomRepository roomRepository;
    private final BedRepository bedRepository;
    private final UserRepository userRepository;
    private final TenantResolverService tenantResolverService;

    // Branches
    @Override
    @Transactional
    public BranchResponse createBranch(BranchCreateRequest request) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new BadRequestException("Tenant ID is required to create a branch");
        }

        String rawBranchName = request.getBranchName() != null && !request.getBranchName().isBlank()
                ? request.getBranchName().trim()
                : (request.getName() != null ? request.getName().trim() : "");
        if (rawBranchName.isBlank()) {
            throw new BadRequestException("Branch name is required");
        }

        if (branchRepository.existsByTenantIdAndBranchName(tenantId, rawBranchName)) {
            throw new BadRequestException("Branch with name '" + rawBranchName + "' already exists for this hospital");
        }

        Branch branch = Branch.builder()
                .id(UUID.randomUUID().toString())
                .branchName(rawBranchName)
                .address(request.getResolvedAddress())
                .phone(request.getPhone())
                .email(request.getEmail())
                .active(true)
                .build();
        branch.setTenantId(tenantId);

        Branch saved = branchRepository.save(branch);
        return mapToBranchResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchResponse> getAllBranchResponses() {
        String tenantId = TenantContext.getTenantId();
        return branchRepository.findByTenantId(tenantId).stream()
                .map(this::mapToBranchResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BranchResponse getBranchResponseById(String id) {
        String tenantId = TenantContext.getTenantId();
        Branch branch = branchRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", "id", id));
        return mapToBranchResponse(branch);
    }

    @Override
    @Transactional
    public BranchDto createBranch(BranchDto branchDto) {
        String tenantId = TenantContext.getTenantId();
        String rawBranchName = branchDto.getBranchName() != null && !branchDto.getBranchName().isBlank()
                ? branchDto.getBranchName().trim()
                : (branchDto.getName() != null ? branchDto.getName().trim() : "");

        if (rawBranchName.isBlank()) {
            throw new BadRequestException("Branch name is required");
        }

        if (branchRepository.existsByTenantIdAndBranchName(tenantId, rawBranchName)) {
            throw new BadRequestException("Branch with name '" + rawBranchName + "' already exists");
        }

        Branch branch = Branch.builder()
                .id(branchDto.getId() != null ? branchDto.getId() : UUID.randomUUID().toString())
                .branchName(rawBranchName)
                .address(branchDto.getAddress())
                .phone(branchDto.getPhone())
                .email(branchDto.getEmail())
                .active(true)
                .build();
        branch.setTenantId(tenantId);

        Branch saved = branchRepository.save(branch);
        return mapToBranchDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchDto> getAllBranches() {
        String tenantId = TenantContext.getTenantId();
        return branchRepository.findByTenantId(tenantId).stream()
                .map(this::mapToBranchDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BranchDto getBranchById(String id) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", "id", id));
        return mapToBranchDto(branch);
    }

    // Departments
    @Override
    @Transactional
    public DepartmentDto createDepartment(DepartmentDto dto) {
        String tenantId = TenantContext.getTenantId();
        Branch branch = branchRepository.findById(dto.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch", "id", dto.getBranchId()));

        Department department = Department.builder()
                .branch(branch)
                .name(dto.getName())
                .code(dto.getCode())
                .departmentType(dto.getDepartmentType() != null ? dto.getDepartmentType() : Department.DepartmentType.CLINICAL)
                .active(true)
                .build();
        department.setTenantId(tenantId);

        Department saved = departmentRepository.save(department);
        return mapToDepartmentDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentDto> getDepartmentsByBranch(String branchId) {
        String tenantId = TenantContext.getTenantId();
        return departmentRepository.findByTenantIdAndBranchId(tenantId, branchId).stream()
                .map(this::mapToDepartmentDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentDto getDepartmentById(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));
        return mapToDepartmentDto(department);
    }

    // Rooms
    @Override
    @Transactional
    public RoomDto createRoom(RoomDto dto) {
        String tenantId = TenantContext.getTenantId();
        Department department = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", dto.getDepartmentId()));

        Room room = Room.builder()
                .department(department)
                .roomNumber(dto.getRoomNumber())
                .roomType(dto.getRoomType() != null ? dto.getRoomType() : Room.RoomType.GENERAL)
                .floorNumber(dto.getFloorNumber())
                .active(true)
                .build();
        room.setTenantId(tenantId);

        Room saved = roomRepository.save(room);
        return mapToRoomDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomDto> getRoomsByDepartment(Long departmentId) {
        String tenantId = TenantContext.getTenantId();
        return roomRepository.findByTenantIdAndDepartmentId(tenantId, departmentId).stream()
                .map(this::mapToRoomDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RoomDto getRoomById(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room", "id", id));
        return mapToRoomDto(room);
    }

    // Beds
    @Override
    @Transactional
    public BedDto createBed(BedDto dto) {
        String tenantId = TenantContext.getTenantId();
        Room room = roomRepository.findById(dto.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room", "id", dto.getRoomId()));

        Bed bed = Bed.builder()
                .room(room)
                .bedNumber(dto.getBedNumber())
                .status(dto.getStatus() != null ? dto.getStatus() : Bed.BedStatus.AVAILABLE)
                .build();
        bed.setTenantId(tenantId);

        Bed saved = bedRepository.save(bed);
        return mapToBedDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BedDto> getBedsByRoom(Long roomId) {
        String tenantId = TenantContext.getTenantId();
        return bedRepository.findByTenantIdAndRoomId(tenantId, roomId).stream()
                .map(this::mapToBedDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BedDto getBedById(Long id) {
        Bed bed = bedRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bed", "id", id));
        return mapToBedDto(bed);
    }

    @Override
    @Transactional
    public BedDto updateBedStatus(Long id, String status) {
        Bed bed = bedRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bed", "id", id));
        bed.setStatus(Bed.BedStatus.valueOf(status.toUpperCase()));
        return mapToBedDto(bedRepository.save(bed));
    }

    private BranchDto mapToBranchDto(Branch b) {
        String tenantName = tenantResolverService.getTenantName(b.getTenantId());
        return BranchDto.builder()
                .id(b.getId())
                .tenantId(b.getTenantId())
                .tenantName(tenantName)
                .branchName(b.getBranchName())
                .address(b.getAddress())
                .phone(b.getPhone())
                .email(b.getEmail())
                .active(b.isActive())
                .build();
    }

    private DepartmentDto mapToDepartmentDto(Department d) {
        String tenantName = tenantResolverService.getTenantName(d.getTenantId());
        return DepartmentDto.builder()
                .id(d.getId())
                .tenantId(d.getTenantId())
                .tenantName(tenantName)
                .branchId(d.getBranch().getId())
                .branchName(d.getBranch().getBranchName())
                .name(d.getName())
                .code(d.getCode())
                .departmentType(d.getDepartmentType())
                .active(d.isActive())
                .build();
    }

    private RoomDto mapToRoomDto(Room r) {
        return RoomDto.builder()
                .id(r.getId())
                .departmentId(r.getDepartment().getId())
                .departmentName(r.getDepartment().getName())
                .roomNumber(r.getRoomNumber())
                .roomType(r.getRoomType())
                .floorNumber(r.getFloorNumber())
                .active(r.isActive())
                .build();
    }

    private BedDto mapToBedDto(Bed b) {
        return BedDto.builder()
                .id(b.getId())
                .roomId(b.getRoom().getId())
                .roomNumber(b.getRoom().getRoomNumber())
                .bedNumber(b.getBedNumber())
                .status(b.getStatus())
                .build();
    }

    private BranchResponse mapToBranchResponse(Branch b) {
        BranchResponse.BranchAdminSummary adminSummary = null;
        Optional<User> adminOpt = userRepository.findAdminByTenantIdAndBranchId(
                b.getTenantId(), b.getId(), "ADMIN", User.UserStatus.ACTIVE
        );
        if (adminOpt.isPresent()) {
            User admin = adminOpt.get();
            adminSummary = BranchResponse.BranchAdminSummary.builder()
                    .id(admin.getId())
                    .username(admin.getUsername())
                    .email(admin.getEmail())
                    .fullName(admin.getFirstName() + " " + admin.getLastName())
                    .phoneNumber(admin.getPhoneNumber())
                    .build();
        }

        String tenantName = tenantResolverService.getTenantName(b.getTenantId());

        return BranchResponse.builder()
                .id(b.getId())
                .tenantId(b.getTenantId())
                .tenantName(tenantName)
                .branchName(b.getBranchName())
                .location(b.getAddress())
                .phone(b.getPhone())
                .email(b.getEmail())
                .active(b.isActive())
                .admin(adminSummary)
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .build();
    }
}
