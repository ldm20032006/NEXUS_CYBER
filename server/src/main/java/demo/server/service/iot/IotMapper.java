package demo.server.service.iot;

import demo.server.dto.iot.AlertHistoryResponse;
import demo.server.dto.iot.DeviceAlertResponse;
import demo.server.dto.iot.DeviceTelemetryResponse;
import demo.server.dto.iot.IotDeviceResponse;
import demo.server.entity.iot.AlertHistory;
import demo.server.entity.iot.DeviceAlert;
import demo.server.entity.iot.DeviceTelemetry;
import demo.server.entity.iot.IotDevice;
import org.springframework.stereotype.Component;

@Component
public class IotMapper {

    public IotDeviceResponse toDevice(IotDevice device) {
        return new IotDeviceResponse(
                device.getId(),
                device.getBranch().getId(),
                device.getStation() == null ? null : device.getStation().getId(),
                device.getDeviceType(),
                device.getSerialNumber(),
                device.getName(),
                device.getFirmwareVersion(),
                device.getCapabilities(),
                device.getStatus(),
                device.getLastHeartbeatAt(),
                device.getMissedHeartbeatCount(),
                device.isMechanicalCommandLocked(),
                device.getIpAddress(),
                device.isDeleted());
    }

    public DeviceTelemetryResponse toTelemetry(DeviceTelemetry telemetry) {
        return new DeviceTelemetryResponse(
                telemetry.getId(),
                telemetry.getDevice().getId(),
                telemetry.getBranch().getId(),
                telemetry.getStation() == null ? null : telemetry.getStation().getId(),
                telemetry.getReceivedAt(),
                telemetry.getOnline(),
                telemetry.getBatteryLevel(),
                telemetry.getSignalStrength(),
                telemetry.getErrorCode(),
                telemetry.getFirmwareVersion(),
                telemetry.getMetricKey(),
                telemetry.getMetricValue(),
                telemetry.getPayloadJson());
    }

    public DeviceAlertResponse toAlert(DeviceAlert alert) {
        return new DeviceAlertResponse(
                alert.getId(),
                alert.getDevice().getId(),
                alert.getBranch() == null ? null : alert.getBranch().getId(),
                alert.getStation() == null ? null : alert.getStation().getId(),
                alert.getAlertCode(),
                alert.getTitle(),
                alert.getDescription(),
                alert.getSeverity(),
                alert.getStatus(),
                alert.getAssignedStaff() == null ? null : alert.getAssignedStaff().getId(),
                alert.getAcknowledgedBy() == null ? null : alert.getAcknowledgedBy().getId(),
                alert.getAcknowledgedAt(),
                alert.getResolvedBy() == null ? null : alert.getResolvedBy().getId(),
                alert.getResolvedAt(),
                alert.getClosedAt(),
                alert.isCriticalMechanicalLock(),
                alert.getNote());
    }

    public AlertHistoryResponse toHistory(AlertHistory history) {
        return new AlertHistoryResponse(
                history.getId(),
                history.getAlert().getId(),
                history.getActor() == null ? null : history.getActor().getId(),
                history.getFromStatus(),
                history.getToStatus(),
                history.getAction(),
                history.getNote(),
                history.getCreatedAt());
    }
}
