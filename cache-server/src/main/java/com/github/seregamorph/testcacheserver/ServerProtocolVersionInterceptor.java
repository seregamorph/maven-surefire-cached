package com.github.seregamorph.testcacheserver;

import com.github.seregamorph.maven.test.common.ServerProtocolVersion;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * @author Sergey Chernov
 */
public class ServerProtocolVersionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        response.addHeader(ServerProtocolVersion.HEADER_SERVER_PROTOCOL_VERSION,
            Integer.toString(ServerProtocolVersion.SERVER_PROTOCOL_VERSION));
        return true;
    }
}
