package demo.server.common.audit;

import demo.server.common.enums.AuditAction;

import java.util.UUID;

public interface AuditRecorder {

    void record(AuditAction action, String entityType, UUID entityId, Object beforeData, Object afterData);

    void record(AuditRecordCommand command);
}
