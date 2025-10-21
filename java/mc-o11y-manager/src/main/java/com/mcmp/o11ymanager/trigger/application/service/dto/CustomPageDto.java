package com.mcmp.o11ymanager.trigger.application.service.dto;

import java.util.List;
import java.util.function.Supplier;
import lombok.Builder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Builder
public record CustomPageDto<T>(
        List<T> content,
        Pageable pageable,
        long totalPages,
        long totalElements,
        long numberOfElements) {

    public static <T> CustomPageDto<T> of(Page<?> page, Supplier<List<T>> supplier) {
        return CustomPageDto.<T>builder()
                .content(supplier.get())
                .pageable(page.getPageable())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .numberOfElements(page.getNumberOfElements())
                .build();
    }

    public static <T> CustomPageDto<T> empty() {
        return CustomPageDto.<T>builder()
                .content(List.of())
                .pageable(Pageable.ofSize(1))
                .totalPages(1)
                .totalElements(0)
                .numberOfElements(0)
                .build();
    }
}
