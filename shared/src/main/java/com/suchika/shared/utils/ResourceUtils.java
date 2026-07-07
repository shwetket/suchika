package com.suchika.shared.utils;

import com.suchika.shared.exception.BadRequestException;

import java.time.LocalDate;

public final class ResourceUtils {

    public static final int DEFAULT_PAGE_SIZE = 50;
    public static final int MAX_PAGE_SIZE = 200;

    private ResourceUtils() {
        // Utility class
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
