package com.hungrybox.common.security;

/** The caller identified by a verified JWT. */
public record AuthenticatedUser(long userId, String role) {

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_CUSTOMER = "CUSTOMER";

    public boolean isAdmin() {
        return ROLE_ADMIN.equals(role);
    }
}
