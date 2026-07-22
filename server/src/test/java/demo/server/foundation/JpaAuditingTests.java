package demo.server.foundation;

import demo.server.common.time.ClockProvider;
import demo.server.entity.branch.Branch;
import demo.server.repository.branch.BranchRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class JpaAuditingTests {

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private ClockProvider clockProvider;

    @Test
    void auditingPopulatesUtcTimestampsAndVersion() {
        Branch branch = new Branch();
        branch.setCode("TST");
        branch.setName("Test Branch");
        branch.setTimezone("UTC");

        Branch saved = branchRepository.saveAndFlush(branch);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.isDeleted()).isFalse();
        assertThat(saved.getVersion()).isNotNull();
        assertThat(clockProvider.clock().getZone()).isEqualTo(ZoneOffset.UTC);
    }
}
