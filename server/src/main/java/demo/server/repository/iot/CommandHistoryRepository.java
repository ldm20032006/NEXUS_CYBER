package demo.server.repository.iot;

import demo.server.entity.iot.CommandHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CommandHistoryRepository extends JpaRepository<CommandHistory, UUID> {

    List<CommandHistory> findByCommandIdOrderByCreatedAtAsc(UUID commandId);
}
