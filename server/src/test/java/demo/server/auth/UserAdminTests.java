package demo.server.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import demo.server.common.enums.BranchStatus;
import demo.server.common.enums.RoleCode;
import demo.server.common.enums.UserStatus;
import demo.server.dto.admin.CreateStaffRequest;
import demo.server.entity.auth.AppUser;
import demo.server.entity.auth.Role;
import demo.server.entity.branch.Branch;
import demo.server.repository.auth.AppUserRepository;
import demo.server.repository.auth.RoleRepository;
import demo.server.repository.branch.BranchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UserAdminTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    BranchRepository branchRepository;

    @Autowired
    AppUserRepository appUserRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    Branch branch;

    @BeforeEach
    void setUp() {
        Arrays.stream(RoleCode.values()).forEach(code -> {
            Role role = new Role();
            role.setCode(code);
            role.setName(code.name());
            roleRepository.save(role);
        });
        branch = new Branch();
        branch.setCode("HN01");
        branch.setName("Ha Noi 01");
        branch.setStatus(BranchStatus.ACTIVE);
        branch = branchRepository.save(branch);
    }

    @Test
    void branchAdminCannotCreateSuperAdmin() throws Exception {
        String token = accessToken(admin("branch-admin@example.com", RoleCode.BRANCH_ADMIN));

        mockMvc.perform(post("/api/v1/admin/users/staff")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateStaffRequest(
                                "new-super@example.com",
                                null,
                                "Password123",
                                "New Super",
                                branch.getId(),
                                Set.of(RoleCode.SUPER_ADMIN)))))
                .andExpect(status().isForbidden());
    }

    @Test
    void branchAdminCanLockOnlyOwnBranchStaff() throws Exception {
        String token = accessToken(admin("branch-admin-lock@example.com", RoleCode.BRANCH_ADMIN));
        AppUser staff = admin("staff@example.com", RoleCode.STAFF_FNB);

        mockMvc.perform(patch("/api/v1/admin/users/{id}/lock", staff.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"policy violation"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void nonAdminCannotUseUserAdministration() throws Exception {
        String token = accessToken(admin("gamer-admin-denied@example.com", RoleCode.GAMER));

        mockMvc.perform(post("/api/v1/admin/users/staff")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateStaffRequest(
                                "staff-denied@example.com",
                                null,
                                "Password123",
                                "Denied",
                                branch.getId(),
                                Set.of(RoleCode.STAFF_FNB)))))
                .andExpect(status().isForbidden());
    }

    private AppUser admin(String email, RoleCode roleCode) {
        Role role = roleRepository.findByCode(roleCode).orElseThrow();
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("Password123"));
        user.setFullName("Admin");
        user.setStatus(UserStatus.ACTIVE);
        user.setBranch(branch);
        user.getRoles().add(role);
        return appUserRepository.save(user);
    }

    private String accessToken(AppUser user) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier":"%s","password":"Password123"}
                                """.formatted(user.getEmail())))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode node = objectMapper.readTree(response);
        return node.path("data").path("accessToken").asText();
    }
}
