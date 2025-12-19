package com.springcloud.practice.common.context;

public class UserContext {

    private static final ThreadLocal<UserInfo> USER_CONTEXT = new ThreadLocal<>();

    private UserContext() {}

    public static void set(UserInfo userInfo) {
        USER_CONTEXT.set(userInfo);
    }

    public static void clear() {
        USER_CONTEXT.remove();
    }

    public static Long getUserId() {
        UserInfo userInfo = USER_CONTEXT.get();
        return userInfo == null ? null : userInfo.getUserId();
    }

    public static UserInfo getUserInfo() {
        return USER_CONTEXT.get();
    }
}