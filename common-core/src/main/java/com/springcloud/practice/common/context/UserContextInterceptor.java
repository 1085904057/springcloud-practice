package com.springcloud.practice.common.context;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

public class UserContextInterceptor implements HandlerInterceptor {
    public static final String HEADER_USER_ID = "X-User-ID";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String userIdStr = request.getHeader(HEADER_USER_ID);
        if (StringUtils.hasText(userIdStr)) {
            UserInfo userInfo = new UserInfo();
            userInfo.setUserId(Long.valueOf(userIdStr));
            UserContext.set(userInfo);
            // todo: 扩展用户角色权限
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}