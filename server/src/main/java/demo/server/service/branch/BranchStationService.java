package demo.server.service.branch;

import demo.server.common.audit.AuditRecorder;
import demo.server.common.enums.AuditAction;
import demo.server.common.enums.BranchStatus;
import demo.server.common.enums.StationStatus;
import demo.server.common.enums.ZoneStatus;
import demo.server.common.response.PageResponse;
import demo.server.common.security.SecureTokenGenerator;
import demo.server.common.security.TokenHashService;
import demo.server.dto.branch.BranchRequest;
import demo.server.dto.branch.BranchResponse;
import demo.server.dto.branch.StationCredentialResponse;
import demo.server.dto.branch.StationHeartbeatResponse;
import demo.server.dto.branch.StationRequest;
import demo.server.dto.branch.StationResponse;
import demo.server.dto.branch.ZoneRequest;
import demo.server.dto.branch.ZoneResponse;
import demo.server.entity.branch.Branch;
import demo.server.entity.branch.Station;
import demo.server.entity.branch.StationCredential;
import demo.server.entity.branch.Zone;
import demo.server.exception.BusinessRuleException;
import demo.server.exception.DuplicateResourceException;
import demo.server.exception.ForbiddenException;
import demo.server.exception.ResourceNotFoundException;
import demo.server.exception.UnauthorizedException;
import demo.server.repository.branch.BranchRepository;
import demo.server.repository.branch.StationCredentialRepository;
import demo.server.repository.branch.StationRepository;
import demo.server.repository.branch.ZoneRepository;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.UUID;

@Service
public class BranchStationService {

    private final BranchRepository branchRepository;
    private final ZoneRepository zoneRepository;
    private final StationRepository stationRepository;
    private final StationCredentialRepository credentialRepository;
    private final TokenHashService tokenHashService;
    private final SecureTokenGenerator tokenGenerator;
    private final BranchMapper mapper;
    private final BranchScope branchScope;
    private final AuditRecorder auditRecorder;

    public BranchStationService(
            BranchRepository branchRepository,
            ZoneRepository zoneRepository,
            StationRepository stationRepository,
            StationCredentialRepository credentialRepository,
            TokenHashService tokenHashService,
            SecureTokenGenerator tokenGenerator,
            BranchMapper mapper,
            BranchScope branchScope,
            AuditRecorder auditRecorder
    ) {
        this.branchRepository = branchRepository;
        this.zoneRepository = zoneRepository;
        this.stationRepository = stationRepository;
        this.credentialRepository = credentialRepository;
        this.tokenHashService = tokenHashService;
        this.tokenGenerator = tokenGenerator;
        this.mapper = mapper;
        this.branchScope = branchScope;
        this.auditRecorder = auditRecorder;
    }

    @Transactional
    public BranchResponse createBranch(BranchRequest request) {
        branchScope.requireSuperAdmin();
        if (branchRepository.existsByCode(normalize(request.code()))) {
            throw new DuplicateResourceException("Branch code already exists");
        }
        Branch branch = new Branch();
        apply(branch, request);
        Branch saved = branchRepository.save(branch);
        auditRecorder.record(AuditAction.CREATE_BRANCH, "Branch", saved.getId(), null, mapper.toBranchResponse(saved));
        return mapper.toBranchResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<BranchResponse> listBranches(String code, BranchStatus status, int page, int size) {
        UUID scopedBranchId = branchScope.requireScopedBranch(null);
        Specification<Branch> spec = notDeleted();
        if (scopedBranchId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("id"), scopedBranchId));
        }
        if (StringUtils.hasText(code)) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("code")), "%" + code.toLowerCase() + "%"));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        Page<BranchResponse> result = branchRepository.findAll(spec, pageable(page, size, "code")).map(mapper::toBranchResponse);
        return PageResponse.from(result);
    }

    @Transactional
    public BranchResponse updateBranch(UUID id, BranchRequest request) {
        branchScope.assertBranchAllowed(id);
        Branch branch = branch(id);
        BranchResponse before = mapper.toBranchResponse(branch);
        apply(branch, request);
        auditRecorder.record(AuditAction.UPDATE_BRANCH, "Branch", branch.getId(), before, mapper.toBranchResponse(branch));
        return mapper.toBranchResponse(branch);
    }

    @Transactional
    public void deleteBranch(UUID id) {
        branchScope.requireSuperAdmin();
        branchScope.assertBranchAllowed(id);
        Branch branch = branch(id);
        branch.softDelete();
        auditRecorder.record(AuditAction.UPDATE_BRANCH, "Branch", branch.getId(), mapper.toBranchResponse(branch), "SOFT_DELETED");
    }

    @Transactional
    public ZoneResponse createZone(ZoneRequest request) {
        UUID branchId = branchScope.requireScopedBranch(request.branchId());
        if (zoneRepository.existsByBranch_IdAndCode(branchId, normalize(request.code()))) {
            throw new DuplicateResourceException("Zone code already exists in branch");
        }
        Zone zone = new Zone();
        zone.setBranch(branch(branchId));
        apply(zone, request);
        Zone saved = zoneRepository.save(zone);
        auditRecorder.record(AuditAction.CREATE_ZONE, "Zone", saved.getId(), null, mapper.toZoneResponse(saved));
        return mapper.toZoneResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<ZoneResponse> listZones(UUID branchId, ZoneStatus status, int page, int size) {
        UUID scopedBranchId = branchScope.requireScopedBranch(branchId);
        Specification<Zone> spec = (root, query, cb) -> cb.isFalse(root.get("deleted"));
        if (scopedBranchId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("branch").get("id"), scopedBranchId));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        return PageResponse.from(zoneRepository.findAll(spec, pageable(page, size, "code")).map(mapper::toZoneResponse));
    }

    @Transactional
    public ZoneResponse updateZone(UUID id, ZoneRequest request) {
        Zone zone = zone(id);
        branchScope.assertBranchAllowed(zone.getBranch().getId());
        if (!zone.getBranch().getId().equals(request.branchId())) {
            throw new BusinessRuleException("Zone branch cannot be changed");
        }
        ZoneResponse before = mapper.toZoneResponse(zone);
        apply(zone, request);
        auditRecorder.record(AuditAction.UPDATE_ZONE, "Zone", zone.getId(), before, mapper.toZoneResponse(zone));
        return mapper.toZoneResponse(zone);
    }

    @Transactional
    public void deleteZone(UUID id) {
        Zone zone = zone(id);
        branchScope.assertBranchAllowed(zone.getBranch().getId());
        zone.softDelete();
        auditRecorder.record(AuditAction.DELETE_ZONE, "Zone", zone.getId(), mapper.toZoneResponse(zone), "SOFT_DELETED");
    }

    @Transactional
    public StationResponse createStation(StationRequest request) {
        UUID branchId = branchScope.requireScopedBranch(request.branchId());
        ensureStationUnique(branchId, request.code(), request.stationNumber());
        Station station = new Station();
        station.setBranch(branch(branchId));
        apply(station, request);
        Station saved = stationRepository.save(station);
        auditRecorder.record(AuditAction.CREATE_STATION, "Station", saved.getId(), null, mapper.toStationResponse(saved));
        return mapper.toStationResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<StationResponse> listStations(UUID branchId, UUID zoneId, StationStatus status, String code, int page, int size) {
        UUID scopedBranchId = branchScope.requireScopedBranch(branchId);
        Specification<Station> spec = (root, query, cb) -> {
            root.fetch("branch", JoinType.LEFT);
            root.fetch("zone", JoinType.LEFT);
            return cb.isFalse(root.get("deleted"));
        };
        if (scopedBranchId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("branch").get("id"), scopedBranchId));
        }
        if (zoneId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("zone").get("id"), zoneId));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (StringUtils.hasText(code)) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("code")), "%" + code.toLowerCase() + "%"));
        }
        return PageResponse.from(stationRepository.findAll(spec, pageable(page, size, "code")).map(mapper::toStationResponse));
    }

    @Transactional
    public StationResponse updateStation(UUID id, StationRequest request) {
        Station station = station(id);
        branchScope.assertBranchAllowed(station.getBranch().getId());
        if (!station.getBranch().getId().equals(request.branchId())) {
            throw new BusinessRuleException("Station branch cannot be changed");
        }
        StationResponse before = mapper.toStationResponse(station);
        apply(station, request);
        auditRecorder.record(AuditAction.UPDATE_STATION, "Station", station.getId(), before, mapper.toStationResponse(station));
        return mapper.toStationResponse(station);
    }

    @Transactional
    public void deleteStation(UUID id) {
        Station station = station(id);
        branchScope.assertBranchAllowed(station.getBranch().getId());
        station.softDelete();
        station.setStatus(StationStatus.DISABLED);
        auditRecorder.record(AuditAction.DELETE_STATION, "Station", station.getId(), mapper.toStationResponse(station), "SOFT_DELETED");
    }

    @Transactional
    public StationCredentialResponse createCredential(UUID stationId) {
        Station station = station(stationId);
        branchScope.assertBranchAllowed(station.getBranch().getId());
        revokeActiveCredentials(stationId, "Replaced by new credential");
        StationCredential credential = new StationCredential();
        String secret = tokenGenerator.generate();
        credential.setStation(station);
        credential.setSecretHash(tokenHashService.hash(secret));
        credential.setIssuedAt(Instant.now());
        StationCredential saved = credentialRepository.save(credential);
        auditRecorder.record(AuditAction.CREATE_STATION_CREDENTIAL, "StationCredential", saved.getId(), null, station.getId());
        return new StationCredentialResponse(saved.getId(), station.getId(), secret, saved.getIssuedAt(), saved.getExpiresAt());
    }

    @Transactional
    public StationCredentialResponse rotateCredential(UUID stationId) {
        StationCredentialResponse response = createCredential(stationId);
        auditRecorder.record(AuditAction.ROTATE_STATION_CREDENTIAL, "Station", stationId, null, stationId);
        return response;
    }

    @Transactional
    public void revokeCredential(UUID stationId) {
        Station station = station(stationId);
        branchScope.assertBranchAllowed(station.getBranch().getId());
        revokeActiveCredentials(stationId, "Revoked by administrator");
        auditRecorder.record(AuditAction.REVOKE_STATION_CREDENTIAL, "Station", stationId, null, stationId);
    }

    @Transactional
    public StationHeartbeatResponse heartbeat(UUID stationId, String rawSecret) {
        Station station = station(stationId);
        if (!StringUtils.hasText(rawSecret)) {
            throw new UnauthorizedException("Station credential is required");
        }
        StationCredential credential = credentialRepository.findFirstByStation_IdAndRevokedAtIsNullOrderByIssuedAtDesc(stationId)
                .orElseThrow(() -> new UnauthorizedException("Station credential is invalid"));
        if (!tokenHashService.hash(rawSecret).equals(credential.getSecretHash())) {
            throw new UnauthorizedException("Station credential is invalid");
        }
        if (credential.getExpiresAt() != null && credential.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Station credential has expired");
        }
        credential.setLastUsedAt(Instant.now());
        station.setLastSeenAt(Instant.now());
        if (station.getStatus() == StationStatus.OFFLINE) {
            station.setStatus(StationStatus.AVAILABLE);
        }
        auditRecorder.record(AuditAction.STATION_HEARTBEAT, "Station", station.getId(), null, mapper.toStationResponse(station));
        return new StationHeartbeatResponse(station.getId(), station.getStatus(), station.getLastSeenAt());
    }

    private void apply(Branch branch, BranchRequest request) {
        branch.setCode(normalize(request.code()));
        branch.setName(request.name());
        branch.setAddress(request.address());
        branch.setTimezone(request.timezone());
        branch.setStatus(request.status() == null ? BranchStatus.ACTIVE : request.status());
        branch.setPaymentEnabled(request.paymentEnabled());
        branch.setPaymentPolicy(request.paymentPolicy());
        branch.setOperatingStartTime(request.operatingStartTime());
        branch.setOperatingEndTime(request.operatingEndTime());
    }

    private void apply(Zone zone, ZoneRequest request) {
        zone.setCode(normalize(request.code()));
        zone.setName(request.name());
        zone.setZoneType(request.zoneType());
        zone.setStatus(request.status() == null ? ZoneStatus.ACTIVE : request.status());
        zone.setSortOrder(request.sortOrder());
    }

    private void apply(Station station, StationRequest request) {
        Zone zone = request.zoneId() == null ? null : zone(request.zoneId());
        if (zone != null && !zone.getBranch().getId().equals(station.getBranch().getId())) {
            throw new BusinessRuleException("Zone must belong to station branch");
        }
        station.setZone(zone);
        station.setStationNumber(request.stationNumber());
        station.setCode(normalize(request.code()));
        station.setName(request.name());
        station.setStatus(request.status() == null ? StationStatus.AVAILABLE : request.status());
        station.setIpAddress(request.ipAddress());
        station.setMacAddress(request.macAddress());
    }

    private void ensureStationUnique(UUID branchId, String code, Integer stationNumber) {
        if (stationRepository.existsByBranch_IdAndCode(branchId, normalize(code))) {
            throw new DuplicateResourceException("Station code already exists in branch");
        }
        if (stationRepository.existsByBranch_IdAndStationNumber(branchId, stationNumber)) {
            throw new DuplicateResourceException("Station number already exists in branch");
        }
    }

    private void revokeActiveCredentials(UUID stationId, String reason) {
        Instant now = Instant.now();
        credentialRepository.findAllByStation_IdAndRevokedAtIsNull(stationId).forEach(credential -> {
            credential.setRevokedAt(now);
            credential.setRevokeReason(reason);
        });
    }

    private Branch branch(UUID id) {
        return branchRepository.findById(id).filter(branch -> !branch.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
    }

    private Zone zone(UUID id) {
        return zoneRepository.findById(id).filter(zone -> !zone.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found"));
    }

    private Station station(UUID id) {
        return stationRepository.findById(id).filter(station -> !station.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Station not found"));
    }

    private Specification<Branch> notDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("deleted"));
    }

    private PageRequest pageable(int page, int size, String sort) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return PageRequest.of(safePage, safeSize, Sort.by(sort).ascending());
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }
}
