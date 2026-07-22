package demo.server.audit;

import demo.server.common.audit.AuditRecordCommand;
import demo.server.common.audit.AuditRecorder;
import demo.server.common.enums.AuditAction;
import demo.server.entity.audit.AuditLog;
import demo.server.repository.audit.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AuditRecorderTests {

    @Autowired
    AuditRecorder auditRecorder;

    @Autowired
    AuditLogRepository auditLogRepository;

    @Test
    void auditRecordMasksSensitiveData() {
        auditRecorder.record(new AuditRecordCommand(
                null,
                "SUPER_ADMIN",
                null,
                AuditAction.LOGIN,
                "AppUser",
                "user-1",
                Map.of("email", "admin@example.com", "password", "Secret123", "phone", "0900000001"),
                Map.of("refreshToken", "raw-refresh-token", "authorization", "Bearer abc.def.ghi"),
                "127.0.0.1",
                "Mozilla token=raw-token",
                "corr-1"));

        AuditLog auditLog = auditLogRepository.findAll().getFirst();

        assertThat(auditLog.getBeforeData()).doesNotContain("admin@example.com", "Secret123", "0900000001");
        assertThat(auditLog.getAfterData()).doesNotContain("raw-refresh-token", "abc.def.ghi");
        assertThat(auditLog.getUserAgent()).doesNotContain("raw-token");
        assertThat(auditLog.getCorrelationId()).isEqualTo("corr-1");
        assertThat(auditLog.getTimestamp()).isNotNull();
    }

    @Test
    void auditLogEntityIsAppendOnly() {
        auditRecorder.record(new AuditRecordCommand(
                null,
                "SUPER_ADMIN",
                null,
                AuditAction.LOCK_USER,
                "AppUser",
                "user-2",
                null,
                Map.of("status", "LOCKED"),
                "127.0.0.1",
                "JUnit",
                "corr-2"));
        AuditLog auditLog = auditLogRepository.findAll().getFirst();
        auditLog.setAfterData("{}");

        assertThatThrownBy(() -> auditLogRepository.saveAndFlush(auditLog))
                .hasMessageContaining("AuditLog is append-only");
    }
}
