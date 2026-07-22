package demo.server.jobs;

import demo.server.common.enums.BranchStatus;
import demo.server.common.enums.DeviceStatus;
import demo.server.common.enums.DeviceType;
import demo.server.common.enums.InvitationStatus;
import demo.server.common.enums.LfgSignalStatus;
import demo.server.common.enums.QrLoginSessionStatus;
import demo.server.common.enums.SessionStatus;
import demo.server.common.enums.StationStatus;
import demo.server.entity.auth.AppUser;
import demo.server.entity.auth.PasswordResetToken;
import demo.server.entity.auth.RefreshToken;
import demo.server.entity.branch.Branch;
import demo.server.entity.branch.Station;
import demo.server.entity.game.Game;
import demo.server.entity.iot.IotDevice;
import demo.server.entity.lfg.LfgSignal;
import demo.server.entity.lfg.TeamInvitation;
import demo.server.entity.session.PlaySession;
import demo.server.entity.session.QrLoginSession;
import demo.server.repository.auth.AppUserRepository;
import demo.server.repository.auth.PasswordResetTokenRepository;
import demo.server.repository.auth.RefreshTokenRepository;
import demo.server.repository.branch.BranchRepository;
import demo.server.repository.branch.StationRepository;
import demo.server.repository.game.GameRepository;
import demo.server.repository.iot.DeviceAlertRepository;
import demo.server.repository.iot.IotDeviceRepository;
import demo.server.repository.lfg.LfgSignalRepository;
import demo.server.repository.lfg.TeamInvitationRepository;
import demo.server.repository.notification.NotificationRepository;
import demo.server.repository.session.PlaySessionRepository;
import demo.server.repository.session.QrLoginSessionRepository;
import demo.server.service.jobs.BackgroundJobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = "nexus.jobs.enabled=false")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BackgroundJobServiceTests {

    @Autowired
    BackgroundJobService jobService;

    @Autowired
    BranchRepository branchRepository;

    @Autowired
    StationRepository stationRepository;

    @Autowired
    AppUserRepository userRepository;

    @Autowired
    QrLoginSessionRepository qrRepository;

    @Autowired
    TeamInvitationRepository invitationRepository;

    @Autowired
    LfgSignalRepository lfgRepository;

    @Autowired
    GameRepository gameRepository;

    @Autowired
    IotDeviceRepository deviceRepository;

    @Autowired
    DeviceAlertRepository alertRepository;

    @Autowired
    RefreshTokenRepository refreshTokenRepository;

    @Autowired
    PasswordResetTokenRepository resetTokenRepository;

    @Autowired
    PlaySessionRepository sessionRepository;

    @Autowired
    NotificationRepository notificationRepository;

    Branch branch;
    Station station;
    AppUser user;

    @BeforeEach
    void setUp() {
        branch = branch("JOB01");
        station = station(branch, "PC01");
        user = user("job-user@example.com", branch);
    }

    @Test
    void expireJobsAreIdempotentOnReplay() {
        QrLoginSession qr = new QrLoginSession();
        qr.setStation(station);
        qr.setNonce("nonce-job");
        qr.setQrPayload("{}");
        qr.setStatus(QrLoginSessionStatus.PENDING);
        qr.setExpiresAt(Instant.now().minusSeconds(5));
        qrRepository.save(qr);

        TeamInvitation invitation = new TeamInvitation();
        invitation.setSender(user);
        invitation.setReceiver(user("receiver@example.com", branch));
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setExpiresAt(Instant.now().minusSeconds(5));
        invitationRepository.save(invitation);

        Game game = new Game();
        game.setSlug("jobgame");
        game.setName("Job Game");
        gameRepository.save(game);
        LfgSignal lfg = new LfgSignal();
        lfg.setUser(user);
        lfg.setBranch(branch);
        lfg.setGame(game);
        lfg.setTargetMembers(2);
        lfg.setStatus(LfgSignalStatus.ACTIVE);
        lfg.setExpiresAt(Instant.now().minusSeconds(5));
        lfgRepository.save(lfg);

        assertThat(jobService.expireQr()).isEqualTo(1);
        assertThat(jobService.expireInvitations()).isEqualTo(1);
        assertThat(jobService.expireLfg()).isEqualTo(1);
        assertThat(jobService.expireQr()).isZero();
        assertThat(jobService.expireInvitations()).isZero();
        assertThat(jobService.expireLfg()).isZero();

        assertThat(qrRepository.findById(qr.getId()).orElseThrow().getStatus()).isEqualTo(QrLoginSessionStatus.EXPIRED);
        assertThat(invitationRepository.findById(invitation.getId()).orElseThrow().getStatus()).isEqualTo(InvitationStatus.EXPIRED);
        assertThat(lfgRepository.findById(lfg.getId()).orElseThrow().getStatus()).isEqualTo(LfgSignalStatus.EXPIRED);
    }

    @Test
    void heartbeatTimeoutReplayDoesNotDuplicateDeviceAlert() {
        station.setLastSeenAt(Instant.now().minusSeconds(600));
        station.setStatus(StationStatus.AVAILABLE);
        stationRepository.save(station);
        IotDevice device = new IotDevice();
        device.setBranch(branch);
        device.setStation(station);
        device.setDeviceType(DeviceType.IOT_GATEWAY);
        device.setSerialNumber("JOB-DEVICE-1");
        device.setStatus(DeviceStatus.ONLINE);
        device.setLastHeartbeatAt(Instant.now().minusSeconds(600));
        deviceRepository.save(device);

        assertThat(jobService.stationHeartbeatTimeouts()).isEqualTo(1);
        assertThat(jobService.deviceHeartbeatTimeouts()).isEqualTo(1);
        assertThat(jobService.stationHeartbeatTimeouts()).isZero();
        assertThat(jobService.deviceHeartbeatTimeouts()).isZero();

        assertThat(stationRepository.findById(station.getId()).orElseThrow().getStatus()).isEqualTo(StationStatus.OFFLINE);
        assertThat(deviceRepository.findById(device.getId()).orElseThrow().getStatus()).isEqualTo(DeviceStatus.OFFLINE);
        assertThat(alertRepository.findAll()).hasSize(1);
        assertThat(alertRepository.findAll().getFirst().getAlertCode()).isEqualTo("HEARTBEAT_TIMEOUT");
    }

    @Test
    void cleanupAndSessionWarningAreIdempotent() {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash("refresh-expired");
        refreshToken.setFamilyId(UUID.randomUUID());
        refreshToken.setIssuedAt(Instant.now().minusSeconds(1000));
        refreshToken.setExpiresAt(Instant.now().minusSeconds(10));
        refreshTokenRepository.save(refreshToken);
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setTokenHash("reset-expired");
        resetToken.setIssuedAt(Instant.now().minusSeconds(1000));
        resetToken.setExpiresAt(Instant.now().minusSeconds(10));
        resetTokenRepository.save(resetToken);
        PlaySession session = new PlaySession();
        session.setUser(user);
        session.setStation(station);
        session.setStatus(SessionStatus.ACTIVE);
        session.setStartedAt(Instant.now().minusSeconds(1200));
        sessionRepository.save(session);

        assertThat(jobService.cleanupTokens()).isEqualTo(2);
        assertThat(jobService.cleanupTokens()).isZero();
        assertThat(jobService.sessionEndingWarnings()).isEqualTo(1);
        assertThat(jobService.sessionEndingWarnings()).isZero();
        assertThat(notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId())).hasSize(1);
    }

    private Branch branch(String code) {
        Branch branch = new Branch();
        branch.setCode(code);
        branch.setName(code + " Branch");
        branch.setStatus(BranchStatus.ACTIVE);
        branch.setTimezone("Asia/Ho_Chi_Minh");
        branch.setPaymentPolicy("PREPAID_OR_WALLET");
        branch.setOperatingStartTime(LocalTime.of(8, 0));
        branch.setOperatingEndTime(LocalTime.of(23, 0));
        return branchRepository.save(branch);
    }

    private Station station(Branch branch, String code) {
        Station station = new Station();
        station.setBranch(branch);
        station.setCode(code);
        station.setName(code);
        station.setStationNumber(1);
        station.setStatus(StationStatus.AVAILABLE);
        return stationRepository.save(station);
    }

    private AppUser user(String email, Branch branch) {
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash("Password123");
        user.setFullName("Job User");
        user.setStatus(demo.server.common.enums.UserStatus.ACTIVE);
        user.setBranch(branch);
        return userRepository.save(user);
    }
}
