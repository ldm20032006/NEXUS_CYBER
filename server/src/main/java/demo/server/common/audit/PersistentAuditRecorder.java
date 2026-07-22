package demo.server.common.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import demo.server.common.enums.AuditAction;
import demo.server.common.enums.RoleCode;
import demo.server.common.logging.CorrelationIdFilter;
import demo.server.common.logging.LogMasker;
import demo.server.common.security.AuthenticatedUser;
import demo.server.entity.audit.AuditLog;
import demo.server.entity.auth.AppUser;
import demo.server.entity.branch.Branch;
import demo.server.repository.audit.AuditLogRepository;
import jakarta.persistence.EntityManager;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
public class PersistentAuditRecorder implements AuditRecorder {

    private final AuditLogRepository auditLogRepository;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;

    public PersistentAuditRecorder(AuditLogRepository auditLogRepository, EntityManager entityManager, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.entityManager = entityManager;
        this.objectMapper = objectMapper;
    }

    @Override
    public void record(AuditAction action, String entityType, UUID entityId, Object beforeData, Object afterData) {
        AuthenticatedUser actor = currentActor();
        AuditRequestContext context = AuditRequestContextHolder.current()
                .orElse(new AuditRequestContext(null, null, MDC.get(CorrelationIdFilter.MDC_KEY)));
        record(new AuditRecordCommand(
                actor == null ? null : actor.id(),
                actor == null ? null : primaryRole(actor),
                actor == null ? null : actor.branchId(),
                action,
                entityType,
                entityId == null ? null : entityId.toString(),
                beforeData,
                afterData,
                context.ipAddress(),
                context.userAgent(),
                context.correlationId()));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditRecordCommand command) {
        AuditLog auditLog = new AuditLog();
        if (command.actorId() != null) {
            auditLog.setActor(entityManager.getReference(AppUser.class, command.actorId()));
        }
        if (command.branchId() != null) {
            auditLog.setBranch(entityManager.getReference(Branch.class, command.branchId()));
        }
        auditLog.setActorRole(command.actorRole());
        auditLog.setAction(command.action());
        auditLog.setResourceType(command.resourceType());
        auditLog.setResourceId(command.resourceId());
        auditLog.setBeforeData(maskedJson(command.beforeData()));
        auditLog.setAfterData(maskedJson(command.afterData()));
        auditLog.setIpAddress(command.ipAddress());
        auditLog.setUserAgent(LogMasker.mask(command.userAgent()));
        auditLog.setCorrelationId(command.correlationId());
        auditLog.setTimestamp(Instant.now());
        auditLogRepository.save(auditLog);
    }

    private AuthenticatedUser currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            return null;
        }
        return user;
    }

    private String primaryRole(AuthenticatedUser actor) {
        return actor.roles().stream().map(RoleCode::name).findFirst().orElse(null);
    }

    private String maskedJson(Object data) {
        if (data == null) {
            return null;
        }
        try {
            return LogMasker.mask(objectMapper.writeValueAsString(data));
        } catch (JsonProcessingException ex) {
            return LogMasker.mask(String.valueOf(data));
        }
    }
}
