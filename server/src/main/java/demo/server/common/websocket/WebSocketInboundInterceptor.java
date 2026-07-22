package demo.server.common.websocket;

import demo.server.common.resilience.RateLimitDecision;
import demo.server.common.resilience.RateLimitService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.time.Duration;
import java.util.Map;

@Component
public class WebSocketInboundInterceptor implements ChannelInterceptor {

    private static final String PRINCIPAL_SESSION_KEY = "nexusWsPrincipal";

    private final WebSocketAuthenticationService authenticationService;
    private final WebSocketSubscriptionAuthorizer subscriptionAuthorizer;
    private final RateLimitService rateLimitService;

    public WebSocketInboundInterceptor(WebSocketAuthenticationService authenticationService,
                                       WebSocketSubscriptionAuthorizer subscriptionAuthorizer,
                                       RateLimitService rateLimitService) {
        this.authenticationService = authenticationService;
        this.subscriptionAuthorizer = subscriptionAuthorizer;
        this.rateLimitService = rateLimitService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (accessor.getCommand() == StompCommand.CONNECT) {
            WebSocketPrincipal principal = authenticationService.authenticate(accessor);
            accessor.setUser(principal);
            session(accessor).put(PRINCIPAL_SESSION_KEY, principal);
            return message;
        }
        WebSocketPrincipal principal = principal(accessor);
        if (accessor.getCommand() == StompCommand.SUBSCRIBE) {
            enforceRateLimit("subscribe", principal);
            if (!subscriptionAuthorizer.canSubscribe(principal, accessor.getDestination())) {
                throw new AccessDeniedException("Subscription is not allowed");
            }
            accessor.setUser(principal);
        }
        if (accessor.getCommand() == StompCommand.SEND) {
            enforceRateLimit("send", principal);
            accessor.setUser(principal);
        }
        return message;
    }

    private WebSocketPrincipal principal(StompHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        if (user instanceof WebSocketPrincipal principal) {
            return principal;
        }
        Object sessionPrincipal = session(accessor).get(PRINCIPAL_SESSION_KEY);
        if (sessionPrincipal instanceof WebSocketPrincipal principal) {
            return principal;
        }
        throw new AccessDeniedException("WebSocket authentication is required");
    }

    private void enforceRateLimit(String action, WebSocketPrincipal principal) {
        String subject = principal == null ? "anonymous" : principal.name();
        RateLimitDecision decision = rateLimitService.consume("nexus:ws:" + action + ":" + subject, 60, Duration.ofMinutes(1));
        if (!decision.allowed()) {
            throw new AccessDeniedException("WebSocket rate limit exceeded");
        }
    }

    private Map<String, Object> session(StompHeaderAccessor accessor) {
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) {
            throw new AccessDeniedException("WebSocket session is missing");
        }
        return sessionAttributes;
    }
}
