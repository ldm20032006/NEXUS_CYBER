package demo.server.service.branch;

import demo.server.dto.branch.BranchResponse;
import demo.server.dto.branch.StationResponse;
import demo.server.dto.branch.ZoneResponse;
import demo.server.entity.branch.Branch;
import demo.server.entity.branch.Station;
import demo.server.entity.branch.Zone;
import org.springframework.stereotype.Component;

@Component
public class BranchMapper {

    public BranchResponse toBranchResponse(Branch branch) {
        return new BranchResponse(branch.getId(), branch.getCode(), branch.getName(), branch.getAddress(),
                branch.getTimezone(), branch.getStatus(), branch.isPaymentEnabled(), branch.getPaymentPolicy(),
                branch.getOperatingStartTime(), branch.getOperatingEndTime());
    }

    public ZoneResponse toZoneResponse(Zone zone) {
        return new ZoneResponse(zone.getId(), zone.getBranch().getId(), zone.getCode(), zone.getName(),
                zone.getZoneType(), zone.getStatus(), zone.getSortOrder());
    }

    public StationResponse toStationResponse(Station station) {
        return new StationResponse(station.getId(), station.getBranch().getId(),
                station.getZone() == null ? null : station.getZone().getId(),
                station.getStationNumber(), station.getCode(), station.getName(), station.getStatus(),
                station.getIpAddress(), station.getMacAddress(), station.getLastSeenAt());
    }
}
