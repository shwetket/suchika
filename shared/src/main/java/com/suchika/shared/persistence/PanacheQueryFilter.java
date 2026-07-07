package com.suchika.shared.persistence;

import java.util.List;

public record PanacheQueryFilter(String query, List<Object> params) {
}
