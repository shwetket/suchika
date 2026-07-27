package com.suchika.shared.utils;

import com.suchika.shared.exception.BadRequestException;

import java.time.LocalDate;
import java.util.UUID;

public final class ResourceUtils {

    public static final int DEFAULT_PAGE_SIZE = 50;
    public static final int MAX_PAGE_SIZE = 200;

    private ResourceUtils() {
        // Utility class
    }

    /**
     * Rejects a request that omits profile_id instead of silently falling through to an
     * unfiltered, cross-profile query. Every resource method that accepts profile_id for
     * scoping must call this before using the value (v0.5.1 remediation).
     */
    public static UUID requireProfileId(UUID profileId) {
        if (profileId == null) {
            throw new BadRequestException("profile_id is required");
        }
        return profileId;
    }

    /**
     * Same guard as {@link #requireProfileId(UUID)}, for the profile domain's own scoping
     * parameter. Profile is the identity anchor domain — it has no profile_id of its own to
     * scope by, but its "tenant" concept is admin_id, and omitting it must 400 rather than
     * silently returning every profile across every admin (v0.5.1 remediation).
     */
    public static UUID requireAdminId(UUID adminId) {
        if (adminId == null) {
            throw new BadRequestException("admin_id is required");
        }
        return adminId;
    }

    public static int parsePage(Integer pageParam) {
        int page = pageParam != null ? pageParam : 0;
        if (page < 0) {
            throw new BadRequestException("page must be >= 0");
        }
        return page;
    }

    public static int parseSize(Integer sizeParam) {
        int size = sizeParam != null ? sizeParam : DEFAULT_PAGE_SIZE;
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        return size;
    }

    public static LocalDate parseDate(String value, String paramName) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value);
        } catch (Exception e) {
            throw new BadRequestException("Invalid " + paramName + " date: " + value + " (expected yyyy-MM-dd)");
        }
    }
}
