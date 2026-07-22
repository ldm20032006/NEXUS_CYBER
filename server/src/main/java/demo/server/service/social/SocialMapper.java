package demo.server.service.social;

import demo.server.dto.gamer.PublicGamerProfileResponse;
import demo.server.dto.social.UserBlockResponse;
import demo.server.dto.social.UserReportResponse;
import demo.server.entity.auth.AppUser;
import demo.server.entity.social.UserBlock;
import demo.server.entity.social.UserReport;
import org.springframework.stereotype.Component;

@Component
public class SocialMapper {

    public UserBlockResponse toBlock(UserBlock block) {
        return new UserBlockResponse(block.getId(), block.getBlockedUser().getId(), block.getReason(), block.getBlockedAt());
    }

    public UserReportResponse toReport(UserReport report) {
        return new UserReportResponse(report.getId(), report.getReportedUser().getId(),
                report.getBranch() == null ? null : report.getBranch().getId(), report.getReason(), report.getContext(),
                report.getStatus(), report.getModerationAction(), report.getModerationNote(),
                report.getReportedAt(), report.getModeratedAt());
    }

    public PublicGamerProfileResponse toPublicUser(AppUser user) {
        String display = user.getDisplayName() != null ? user.getDisplayName() : user.getFullName();
        return new PublicGamerProfileResponse(user.getId(), display, user.getAvatarUrl(), null);
    }
}
