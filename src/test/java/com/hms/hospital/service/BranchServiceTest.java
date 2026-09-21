package com.hms.hospital.service;

import com.hms.accesscontrol.entity.Role;
import com.hms.accesscontrol.repository.RoleRepository;
import com.hms.common.exception.BadRequestException;
import com.hms.hospital.dto.BranchAdminCreateRequest;
import com.hms.hospital.dto.BranchCreateRequest;
import com.hms.hospital.dto.BranchResponse;
import com.hms.hospital.entity.Branch;
import com.hms.hospital.entity.StaffHospitalMap;
import com.hms.hospital.repository.BedRepository;
import com.hms.hospital.repository.BranchRepository;
import com.hms.hospital.repository.DepartmentRepository;
import com.hms.hospital.repository.RoomRepository;
import com.hms.hospital.repository.StaffHospitalMapRepository;
import com.hms.hospital.service.impl.HospitalStructureServiceImpl;
import com.hms.tenant.context.TenantContext;
import com.hms.tenant.service.TenantResolverService;
import com.hms.user.dto.UserResponse;
import com.hms.user.entity.User;
import com.hms.user.repository.UserRepository;
import com.hms.user.service.impl.UserServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BranchServiceTest {

    @Mock
    private BranchRepository branchRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private BedRepository bedRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private StaffHospitalMapRepository staffHospitalMapRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TenantResolverService tenantResolverService;

    @InjectMocks
    private HospitalStructureServiceImpl hospitalStructureService;

    @InjectMocks
    private UserServiceImpl userService;

    private static final String TENANT_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String TENANT_NAME = "Apollo Delhi";

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void testCreateBranch_Success() {
        BranchCreateRequest request = BranchCreateRequest.builder()
                .branchName("South Campus")
                .location("Saket, New Delhi")
                .phone("+91-9876543210")
                .email("south@apollo.com")
                .build();

        Branch savedBranch = Branch.builder()
                .id("branch-uuid-10")
                .branchName("South Campus")
                .address("Saket, New Delhi")
                .phone("+91-9876543210")
                .email("south@apollo.com")
                .active(true)
                .build();
        savedBranch.setTenantId(TENANT_ID);

        when(branchRepository.existsByTenantIdAndBranchName(eq(TENANT_ID), eq("South Campus"))).thenReturn(false);
        when(branchRepository.save(any(Branch.class))).thenReturn(savedBranch);
        when(userRepository.findAdminByTenantIdAndBranchId(eq(TENANT_ID), eq("branch-uuid-10"), eq("ADMIN"), eq(User.UserStatus.ACTIVE)))
                .thenReturn(Optional.empty());
        when(tenantResolverService.getTenantName(TENANT_ID)).thenReturn(TENANT_NAME);

        BranchResponse response = hospitalStructureService.createBranch(request);

        assertNotNull(response);
        assertEquals("branch-uuid-10", response.getId());
        assertEquals("South Campus", response.getBranchName());
        assertEquals("Saket, New Delhi", response.getLocation());
        assertEquals(TENANT_ID, response.getTenantId());
        assertEquals(TENANT_NAME, response.getTenantName());
        verify(branchRepository).save(any(Branch.class));
    }

    @Test
    void testGetAllBranches_ReturnsListWithAdmin() {
        Branch branch = Branch.builder()
                .id("branch-uuid-1")
                .branchName("Main Branch")
                .address("Hospital Main Campus")
                .active(true)
                .build();
        branch.setTenantId(TENANT_ID);

        User branchAdmin = User.builder()
                .username("dr_rajesh")
                .email("rajesh@apollo.com")
                .firstName("Rajesh")
                .lastName("Kumar")
                .status(User.UserStatus.ACTIVE)
                .build();
        branchAdmin.setId(5L);

        when(branchRepository.findByTenantId(TENANT_ID)).thenReturn(List.of(branch));
        when(userRepository.findAdminByTenantIdAndBranchId(TENANT_ID, "branch-uuid-1", "ADMIN", User.UserStatus.ACTIVE))
                .thenReturn(Optional.of(branchAdmin));
        when(tenantResolverService.getTenantName(TENANT_ID)).thenReturn(TENANT_NAME);

        List<BranchResponse> branches = hospitalStructureService.getAllBranchResponses();

        assertNotNull(branches);
        assertEquals(1, branches.size());
        assertEquals("Main Branch", branches.get(0).getBranchName());
        assertEquals("branch-uuid-1", branches.get(0).getId());
        assertEquals(TENANT_NAME, branches.get(0).getTenantName());
        assertNotNull(branches.get(0).getAdmin());
        assertEquals("dr_rajesh", branches.get(0).getAdmin().getUsername());
        assertEquals("Rajesh Kumar", branches.get(0).getAdmin().getFullName());
    }

    @Test
    void testCreateBranchAdmin_Success() {
        Branch branch = Branch.builder()
                .id("branch-uuid-2")
                .branchName("South Campus")
                .active(true)
                .build();
        branch.setTenantId(TENANT_ID);

        BranchAdminCreateRequest request = BranchAdminCreateRequest.builder()
                .branchId("branch-uuid-2")
                .username("dr_sunita")
                .email("sunita@apollo.com")
                .password("Password@123")
                .firstName("Sunita")
                .lastName("Sharma")
                .phoneNumber("+91-9999988888")
                .build();

        Role adminRole = Role.builder()
                .name("ADMIN")
                .description("Hospital Administrator")
                .build();
        adminRole.setId(100L);
        adminRole.setTenantId(TENANT_ID);

        when(branchRepository.findByTenantIdAndId(TENANT_ID, "branch-uuid-2")).thenReturn(Optional.of(branch));
        when(userRepository.existsAdminByTenantIdAndBranchId(TENANT_ID, "branch-uuid-2", "ADMIN", User.UserStatus.ACTIVE)).thenReturn(false);
        when(userRepository.existsByTenantIdAndUsername(TENANT_ID, "dr_sunita")).thenReturn(false);
        when(userRepository.existsByTenantIdAndEmail(TENANT_ID, "sunita@apollo.com")).thenReturn(false);
        when(roleRepository.findByTenantIdAndName(TENANT_ID, "ADMIN")).thenReturn(Optional.of(adminRole));
        when(passwordEncoder.encode("Password@123")).thenReturn("encodedPassword");
        when(staffHospitalMapRepository.findByEmail("sunita@apollo.com")).thenReturn(Optional.empty());
        when(tenantResolverService.getTenantName(TENANT_ID)).thenReturn(TENANT_NAME);

        User savedUser = User.builder()
                .username("dr_sunita")
                .email("sunita@apollo.com")
                .password("encodedPassword")
                .firstName("Sunita")
                .lastName("Sharma")
                .phoneNumber("+91-9999988888")
                .status(User.UserStatus.ACTIVE)
                .branch(branch)
                .roles(Set.of(adminRole))
                .build();
        savedUser.setId(50L);
        savedUser.setTenantId(TENANT_ID);

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserResponse response = userService.createBranchAdmin(request);

        assertNotNull(response);
        assertEquals("dr_sunita", response.getUsername());
        assertEquals("sunita@apollo.com", response.getEmail());
        assertEquals("branch-uuid-2", response.getBranchId());
        assertEquals("South Campus", response.getBranchName());
        assertEquals(TENANT_NAME, response.getTenantName());
        verify(userRepository).save(any(User.class));
        verify(staffHospitalMapRepository).save(any(StaffHospitalMap.class));
    }

    @Test
    void testCreateBranchAdmin_DuplicateAdmin_ThrowsBadRequestException() {
        Branch branch = Branch.builder()
                .id("branch-uuid-2")
                .branchName("South Campus")
                .active(true)
                .build();
        branch.setTenantId(TENANT_ID);

        BranchAdminCreateRequest request = BranchAdminCreateRequest.builder()
                .branchId("branch-uuid-2")
                .username("dr_newadmin")
                .email("newadmin@apollo.com")
                .password("Password@123")
                .firstName("New")
                .lastName("Admin")
                .build();

        User existingAdmin = User.builder()
                .username("dr_sunita")
                .email("sunita@apollo.com")
                .firstName("Sunita")
                .lastName("Sharma")
                .status(User.UserStatus.ACTIVE)
                .build();

        when(branchRepository.findByTenantIdAndId(TENANT_ID, "branch-uuid-2")).thenReturn(Optional.of(branch));
        when(userRepository.existsAdminByTenantIdAndBranchId(TENANT_ID, "branch-uuid-2", "ADMIN", User.UserStatus.ACTIVE)).thenReturn(true);
        when(userRepository.findAdminByTenantIdAndBranchId(TENANT_ID, "branch-uuid-2", "ADMIN", User.UserStatus.ACTIVE)).thenReturn(Optional.of(existingAdmin));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> userService.createBranchAdmin(request));
        assertEquals("Branch 'South Campus' already has an assigned Admin: Sunita Sharma (sunita@apollo.com)", ex.getMessage());
    }
}
