package demo.server.repository.iot;

import demo.server.entity.iot.AlertHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AlertHistoryRepository extends JpaRepository<AlertHistory, UUID> {

    List<AlertHistory> findByAlertIdOrderByCreatedAtAsc(UUID alertId);
}
