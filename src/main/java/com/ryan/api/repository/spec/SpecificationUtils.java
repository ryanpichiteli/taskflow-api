package com.ryan.api.repository.spec;

import org.springframework.data.jpa.domain.Specification;

import java.util.Arrays;

public final class SpecificationUtils {

    private SpecificationUtils() {
    }

    @SafeVarargs
    public static <T> Specification<T> allOf(Specification<T>... specs) {
        return Arrays.stream(specs)
                .filter(java.util.Objects::nonNull)
                .reduce(Specification::and)
                .orElse((root, query, cb) -> cb.conjunction());
    }
}
