package com.crm.config;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RateLimitingFilter implements Filter {
    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    private final Map<String, RateLimiter> rateLimiters = new HashMap<>();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String clientIp = request.getRemoteAddr();

        rateLimiters.putIfAbsent(clientIp, new RateLimiter(MAX_REQUESTS_PER_MINUTE));
        RateLimiter rateLimiter = rateLimiters.get(clientIp);

        if (rateLimiter.allowRequest()) {
            chain.doFilter(request, response);
        } else {
            httpResponse.setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);
            httpResponse.getWriter().write("Too many requests, please try again later.");
        }
    }

    @Override
    public void destroy() {}

    private static class RateLimiter {
        private final int maxRequests;
        private int requestCount;
        private long lastRequestTime;

        public RateLimiter(int maxRequests) {
            this.maxRequests = maxRequests;
            this.requestCount = 0;
            this.lastRequestTime = System.currentTimeMillis();
        }

        public synchronized boolean allowRequest() {
            long currentTime = System.currentTimeMillis();
            if (TimeUnit.MILLISECONDS.toMinutes(currentTime - lastRequestTime) >= 1) {
                requestCount = 0;
                lastRequestTime = currentTime;
            }
            if (requestCount < maxRequests) {
                requestCount++;
                return true;
            }
            return false;
        }
    }
}
