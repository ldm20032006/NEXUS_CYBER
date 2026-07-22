package demo.server.common.audit;

import demo.server.common.logging.CorrelationIdFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class AuditContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        AuditRequestContextHolder.set(new AuditRequestContext(
                clientIp(request),
                request.getHeader(HttpHeaders.USER_AGENT),
                MDC.get(CorrelationIdFilter.MDC_KEY)));
        try {
            filterChain.doFilter(request, response);
        } finally {
            AuditRequestContextHolder.clear();
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        return forwardedFor == null ? request.getRemoteAddr() : forwardedFor.split(",")[0].trim();
    }
}
