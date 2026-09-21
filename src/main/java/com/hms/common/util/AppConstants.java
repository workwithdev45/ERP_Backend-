package com.hms.common.util;

public final class AppConstants {

    private AppConstants() {}

    public static final String DEFAULT_PAGE_NUMBER = "0";
    public static final String DEFAULT_PAGE_SIZE = "20";
    public static final String DEFAULT_SORT_BY = "id";
    public static final String DEFAULT_SORT_DIRECTION = "desc";

    public static final String TENANT_HEADER = "X-Tenant-ID";
    public static final String DEFAULT_TENANT_ID = "hms-main";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
}
