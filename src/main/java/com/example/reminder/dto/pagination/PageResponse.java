package com.example.reminder.dto.pagination;
import com.example.reminder.dto.reminder.ReminderResponse;
import org.springframework.data.domain.Page;


import java.util.List;

public record PageResponse<T>(
        long total,
        int current,
        int size,
        int pages,
        List<T> items
) {
    public static <T> PageResponse<T> from(Page<T> page){
        return new PageResponse<>(
                page.getTotalElements(),
                page.getNumber(),
                page.getSize(),
                page.getTotalPages(),
                page.getContent()
        );
    }

    public static <T> PageResponse<T> empty() {
        return new PageResponse<>(0,0,0,0,List.of());
    }
}
