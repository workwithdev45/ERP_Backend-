package com.hms.hospital.service.impl;

import com.hms.accesscontrol.entity.Permission;
import com.hms.accesscontrol.entity.Role;
import com.hms.accesscontrol.repository.PermissionRepository;
import com.hms.accesscontrol.repository.RoleRepository;
import com.hms.hospital.entity.Bed;
import com.hms.hospital.entity.Branch;
import com.hms.hospital.entity.Department;
import com.hms.hospital.entity.Room;
import com.hms.hospital.entity.StaffHospitalMap;
import com.hms.hospital.repository.BedRepository;
import com.hms.hospital.repository.BranchRepository;
import com.hms.hospital.repository.DepartmentRepository;
import com.hms.hospital.repository.RoomRepository;
import com.hms.hospital.repository.StaffHospitalMapRepository;
import com.hms.hospital.service.HospitalProvisioningService;
import com.hms.tenant.entity.Tenant;
import com.hms.tenant.repository.TenantRepository;
import com.hms.user.entity.User;
import com.hms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HospitalProvisioningServiceImpl implements HospitalProvisioningService {

    private final TenantRepository tenantRepository;
    private final BranchRepository branchRepository;
    private final DepartmentRepository departmentRepository;
    private final RoomRepository roomRepository;
    private final BedRepository bedRepository;
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final StaffHospitalMapRepository staffHospitalMapRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public Tenant provisionHospital(String adminEmail, String portalId, boolean startBlank) {
        String cleanPortalId = portalId.toLowerCase().trim();
        String displayName = formatDisplayName(cleanPortalId);
        String tenantUuid = UUID.randomUUID().toString();

        log.info("Provisioning hospital tenant: {} (UUID: {}, startBlank: {}) for admin: {}", cleanPortalId, tenantUuid, startBlank, adminEmail);

        // 1. Create Tenant Record
        Tenant tenant = Tenant.builder()
                .id(tenantUuid)
                .name(displayName)
                .displayName(displayName)
                .subdomain(cleanPortalId)
                .portalId(cleanPortalId)
                .databaseName("tenant_" + cleanPortalId.replace("-", "_"))
                .adminEmail(adminEmail)
                .subscriptionTier("Standard")
                .timezone("Asia/Kolkata")
                .locale("en_IN")
                .currency("INR")
                .firstLoginPending(true)
                .active(true)
                .build();

        Tenant savedTenant = tenantRepository.save(tenant);

        // 2. Create Default Main Branch
        String branchUuid = UUID.randomUUID().toString();
        Branch mainBranch = Branch.builder()
                .id(branchUuid)
                .branchName("Main Branch")
                .address("Hospital Main Campus")
                .phone("+91-0000000000")
                .email(adminEmail)
                .active(true)
                .build();
        mainBranch.setTenantId(tenantUuid);
        Branch savedBranch = branchRepository.save(mainBranch);

        // 3. Ensure Standard Permissions Exist in Central DB
        List<Permission> allPermissions = ensureStandardPermissions();

        // 4. Seed Standard Roles with Permissions for this Tenant
        Role superAdminRole = createRole(tenantUuid, "SUPER_ADMIN", "Hospital Super Administrator with full platform ownership", new HashSet<>(allPermissions));
        Role adminRole = createRole(tenantUuid, "ADMIN", "Hospital Administrator with operational access", new HashSet<>(allPermissions));

        Set<Permission> doctorPerms = filterPermissions(allPermissions, "PATIENT_", "APPOINTMENT_", "OPD_", "IPD_", "LAB_", "PRESCRIPTION_", "VITALS_");
        createRole(tenantUuid, "DOCTOR", "Clinical & consultation access", doctorPerms);

        Set<Permission> nursePerms = filterPermissions(allPermissions, "PATIENT_READ", "IPD_", "BED_", "VITALS_");
        createRole(tenantUuid, "NURSE", "Nursing care and vitals management", nursePerms);

        Set<Permission> receptionistPerms = filterPermissions(allPermissions, "PATIENT_", "APPOINTMENT_", "BILL_VIEW");
        createRole(tenantUuid, "RECEPTIONIST", "Front desk reception and appointments", receptionistPerms);

        Set<Permission> pharmacistPerms = filterPermissions(allPermissions, "PHARMACY_", "MEDICINE_", "PRESCRIPTION_READ");
        createRole(tenantUuid, "PHARMACIST", "Pharmacy dispensing and inventory", pharmacistPerms);

        Set<Permission> billingPerms = filterPermissions(allPermissions, "BILL_", "PAYMENT_", "PATIENT_READ");
        createRole(tenantUuid, "BILLING", "Billing and cashier operations", billingPerms);

        Set<Permission> labPerms = filterPermissions(allPermissions, "LAB_", "PATIENT_READ");
        createRole(tenantUuid, "LAB_TECHNICIAN", "Pathology and laboratory testing", labPerms);

        // 5. Seed Departments (and sample rooms/beds if not startBlank)
        seedHospitalStructure(savedBranch, tenantUuid, startBlank);

        // 6. Create Initial Super Admin User (password placeholder, activated on screen 5)
        String placeholderPassword = passwordEncoder.encode(UUID.randomUUID().toString());
        String defaultUsername = adminEmail.split("@")[0];

        User adminUser = User.builder()
                .username(defaultUsername)
                .email(adminEmail)
                .password(placeholderPassword)
                .firstName("Super")
                .lastName("Admin")
                .phoneNumber(null)
                .status(User.UserStatus.PENDING_VERIFICATION)
                .branch(savedBranch)
                .roles(Set.of(superAdminRole))
                .build();
        adminUser.setTenantId(tenantUuid);
        userRepository.save(adminUser);

        // 7. Update Central StaffHospitalMap
        updateStaffHospitalMap(adminEmail, cleanPortalId);

        log.info("Successfully provisioned hospital portal: {} (tenantId: {})", cleanPortalId, tenantUuid);
        return savedTenant;
    }

    private Role createRole(String tenantId, String roleName, String description, Set<Permission> permissions) {
        Role role = Role.builder()
                .name(roleName)
                .description(description)
                .systemRole(true)
                .permissions(permissions)
                .build();
        role.setTenantId(tenantId);
        return roleRepository.save(role);
    }

    private Set<Permission> filterPermissions(List<Permission> permissions, String... prefixes) {
        return permissions.stream()
                .filter(p -> {
                    for (String prefix : prefixes) {
                        if (p.getName().startsWith(prefix)) {
                            return true;
                        }
                    }
                    return false;
                })
                .collect(Collectors.toSet());
    }

    private List<Permission> ensureStandardPermissions() {
        List<Permission> existing = permissionRepository.findAll();
        if (!existing.isEmpty()) {
            return existing;
        }

        List<Permission> permissionsToCreate = List.of(
                // Patient
                Permission.builder().name("PATIENT_READ").module("PATIENT").description("View patient profiles").build(),
                Permission.builder().name("PATIENT_WRITE").module("PATIENT").description("Create and edit patient profiles").build(),
                Permission.builder().name("PATIENT_DELETE").module("PATIENT").description("Delete patient profiles").build(),
                // Appointment
                Permission.builder().name("APPOINTMENT_READ").module("APPOINTMENT").description("View appointments").build(),
                Permission.builder().name("APPOINTMENT_WRITE").module("APPOINTMENT").description("Book and reschedule appointments").build(),
                Permission.builder().name("APPOINTMENT_CANCEL").module("APPOINTMENT").description("Cancel appointments").build(),
                // OPD
                Permission.builder().name("OPD_READ").module("OPD").description("View OPD queues and consultations").build(),
                Permission.builder().name("OPD_WRITE").module("OPD").description("Manage OPD records").build(),
                // IPD
                Permission.builder().name("IPD_READ").module("IPD").description("View IPD admissions and care").build(),
                Permission.builder().name("IPD_WRITE").module("IPD").description("Manage IPD admissions and discharges").build(),
                // Billing
                Permission.builder().name("BILL_VIEW").module("BILLING").description("View invoices and billing").build(),
                Permission.builder().name("BILL_MANAGE").module("BILLING").description("Generate and adjust bills").build(),
                Permission.builder().name("PAYMENT_PROCESS").module("BILLING").description("Collect payments").build(),
                // Pharmacy
                Permission.builder().name("PHARMACY_READ").module("PHARMACY").description("View pharmacy items").build(),
                Permission.builder().name("PHARMACY_MANAGE").module("PHARMACY").description("Manage pharmacy stock and dispensing").build(),
                Permission.builder().name("MEDICINE_READ").module("PHARMACY").description("View medicines").build(),
                Permission.builder().name("MEDICINE_MANAGE").module("PHARMACY").description("Manage medicine inventory").build(),
                // Prescription
                Permission.builder().name("PRESCRIPTION_READ").module("PRESCRIPTION").description("View prescriptions").build(),
                Permission.builder().name("PRESCRIPTION_WRITE").module("PRESCRIPTION").description("Issue prescriptions").build(),
                // Lab
                Permission.builder().name("LAB_READ").module("LAB").description("View lab tests").build(),
                Permission.builder().name("LAB_MANAGE").module("LAB").description("Manage lab tests and results").build(),
                // Bed
                Permission.builder().name("BED_READ").module("BED").description("View beds and wards").build(),
                Permission.builder().name("BED_MANAGE").module("BED").description("Allocate and manage beds").build(),
                // Vitals
                Permission.builder().name("VITALS_READ").module("VITALS").description("View patient vitals").build(),
                Permission.builder().name("VITALS_WRITE").module("VITALS").description("Record patient vitals").build(),
                // Admin
                Permission.builder().name("USER_MANAGE").module("ADMIN").description("Manage staff and user accounts").build(),
                Permission.builder().name("ROLE_MANAGE").module("ADMIN").description("Manage access roles and permissions").build(),
                Permission.builder().name("SETTINGS_MANAGE").module("ADMIN").description("Manage hospital settings").build()
        );

        return permissionRepository.saveAll(permissionsToCreate);
    }

    private void seedHospitalStructure(Branch branch, String tenantId, boolean startBlank) {
        if (startBlank) {
            createDepartment(branch, tenantId, "General Medicine", "GEN-MED", Department.DepartmentType.CLINICAL);
            createDepartment(branch, tenantId, "Outpatient Department", "OPD", Department.DepartmentType.CLINICAL);
            createDepartment(branch, tenantId, "Emergency", "EMER", Department.DepartmentType.EMERGENCY);
            createDepartment(branch, tenantId, "Pharmacy", "PHARM", Department.DepartmentType.NON_CLINICAL);
            return;
        }

        // Demo Seeded Structure
        Department genMed = createDepartment(branch, tenantId, "General Medicine", "GEN-MED", Department.DepartmentType.CLINICAL);
        Department cardiology = createDepartment(branch, tenantId, "Cardiology", "CARD", Department.DepartmentType.CLINICAL);
        Department opd = createDepartment(branch, tenantId, "Outpatient Department", "OPD", Department.DepartmentType.CLINICAL);
        Department ipd = createDepartment(branch, tenantId, "Inpatient Department", "IPD", Department.DepartmentType.CLINICAL);
        Department emergency = createDepartment(branch, tenantId, "Emergency & Trauma", "EMER", Department.DepartmentType.EMERGENCY);
        Department radiology = createDepartment(branch, tenantId, "Radiology & Imaging", "RAD", Department.DepartmentType.DIAGNOSTIC);
        Department lab = createDepartment(branch, tenantId, "Pathology & Laboratory", "LAB", Department.DepartmentType.DIAGNOSTIC);
        Department pharmacy = createDepartment(branch, tenantId, "Pharmacy & Therapeutics", "PHARM", Department.DepartmentType.NON_CLINICAL);
        Department pediatrics = createDepartment(branch, tenantId, "Pediatrics", "PED", Department.DepartmentType.CLINICAL);
        Department icu = createDepartment(branch, tenantId, "Intensive Care Unit", "ICU", Department.DepartmentType.CLINICAL);

        // Seed Sample Rooms & Beds
        Room ipdRoom101 = createRoom(ipd, tenantId, "Room 101", Room.RoomType.GENERAL, 1);
        createBed(ipdRoom101, tenantId, "Bed 101-A");
        createBed(ipdRoom101, tenantId, "Bed 101-B");

        Room ipdRoom102 = createRoom(ipd, tenantId, "Room 102 (Deluxe)", Room.RoomType.PRIVATE, 1);
        createBed(ipdRoom102, tenantId, "Bed 102-A");

        Room icuUnit = createRoom(icu, tenantId, "ICU Unit A", Room.RoomType.ICU, 2);
        createBed(icuUnit, tenantId, "ICU-Bed-01");
        createBed(icuUnit, tenantId, "ICU-Bed-02");

        Room erRoom = createRoom(emergency, tenantId, "ER Triage Bay", Room.RoomType.EMERGENCY, 0);
        createBed(erRoom, tenantId, "ER-Bay-1");
        createBed(erRoom, tenantId, "ER-Bay-2");
    }

    private Department createDepartment(Branch branch, String tenantId, String name, String code, Department.DepartmentType type) {
        Department dept = Department.builder()
                .branch(branch)
                .name(name)
                .code(code)
                .departmentType(type)
                .active(true)
                .build();
        dept.setTenantId(tenantId);
        return departmentRepository.save(dept);
    }

    private Room createRoom(Department department, String tenantId, String roomNumber, Room.RoomType type, int floor) {
        Room room = Room.builder()
                .department(department)
                .roomNumber(roomNumber)
                .roomType(type)
                .floorNumber(floor)
                .active(true)
                .build();
        room.setTenantId(tenantId);
        return roomRepository.save(room);
    }

    private Bed createBed(Room room, String tenantId, String bedNumber) {
        Bed bed = Bed.builder()
                .room(room)
                .bedNumber(bedNumber)
                .status(Bed.BedStatus.AVAILABLE)
                .build();
        bed.setTenantId(tenantId);
        return bedRepository.save(bed);
    }

    private void updateStaffHospitalMap(String email, String portalId) {
        StaffHospitalMap map = staffHospitalMapRepository.findByEmail(email)
                .orElse(StaffHospitalMap.builder()
                        .email(email)
                        .hospitalIds(portalId)
                        .build());

        if (map.getId() != null) {
            Set<String> set = new HashSet<>(Arrays.asList(map.getHospitalIds().split(",")));
            set.add(portalId);
            map.setHospitalIds(String.join(",", set));
        }
        staffHospitalMapRepository.save(map);
    }

    private String formatDisplayName(String portalId) {
        String[] parts = portalId.split("-");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1).toLowerCase())
                        .append(" ");
            }
        }
        return sb.toString().trim();
    }
}
