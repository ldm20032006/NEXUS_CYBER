package demo.server.service.social;

import demo.server.entity.auth.AppUser;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SocialGuard {

    boolean isBlockedBetween(UUID firstUserId, UUID secondUserId);

    boolean canSendInvitation(UUID senderId, UUID receiverId);

    boolean canSendSocialNotification(UUID senderId, UUID receiverId);

    List<AppUser> filterVisibleTo(UUID viewerId, Collection<AppUser> candidates);
}
