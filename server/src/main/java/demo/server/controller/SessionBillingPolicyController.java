package demo.server.controller;

import demo.server.common.response.ApiResponse;
import demo.server.dto.session.SessionBillingPolicyRequest;
import demo.server.dto.session.SessionBillingPolicyResponse;
import demo.server.service.session.SessionBillingPolicyService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/session-billing-policies")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','BRANCH_ADMIN')")
public class SessionBillingPolicyController {

    private final SessionBillingPolicyService service;

    public SessionBillingPolicyController(SessionBillingPolicyService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<SessionBillingPolicyResponse> create(@Valid @RequestBody SessionBillingPolicyRequest request) {
        return ApiResponse.ok(service.create(request), "Session billing policy created");
    }

    @GetMapping
    public ApiResponse<List<SessionBillingPolicyResponse>> list(@RequestParam(required = false) UUID branchId) {
        return ApiResponse.ok(service.list(branchId));
    }

    @PutMapping("/{id}")
    public ApiResponse<SessionBillingPolicyResponse> update(@PathVariable UUID id,
                                                            @Valid @RequestBody SessionBillingPolicyRequest request) {
        return ApiResponse.ok(service.update(id, request), "Session billing policy updated");
    }
}
