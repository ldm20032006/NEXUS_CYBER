package demo.server.service.session;

import demo.server.dto.session.PlaySessionResponse;
import demo.server.dto.session.QrLoginSessionResponse;
import demo.server.entity.session.PlaySession;
import demo.server.entity.session.QrLoginSession;
import org.springframework.stereotype.Component;

@Component
public class SessionMapper {

    public QrLoginSessionResponse toQrResponse(QrLoginSession session) {
        return new QrLoginSessionResponse(session.getId(), session.getStation().getId(), session.getNonce(),
                session.getQrPayload(), session.getExpiresAt(), session.getStatus());
    }

    public PlaySessionResponse toPlaySession(PlaySession session) {
        return new PlaySessionResponse(session.getId(), session.getUser().getId(), session.getStation().getId(),
                session.getQrLoginSession() == null ? null : session.getQrLoginSession().getId(), session.getStatus(),
                session.getStartedAt(), session.getEndedAt(), session.getDurationMinutes(), session.getEstimatedCost(),
                session.getActualCost(), session.getStartBalance(), session.getEndBalance(), session.getEndedReason());
    }
}
