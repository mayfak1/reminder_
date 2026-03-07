package com.example.reminder.domain.reminder;

import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

public class ReminderSpecifications {
    public static Specification<Reminder> belongsToUser(Long userId){
        return ((root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("user").get("id"),userId));
    }
    public static Specification<Reminder> search(String q){
        return ((root, query, criteriaBuilder) ->
        {
            if (q == null || q.isBlank()) {
                return null;
            }

            String pattern = "%" + q.toLowerCase() + "%";
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("title")),pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("description")),pattern)
            );
        });
    }

    public static Specification<Reminder> filterByDate(LocalDate date){
        return ((root, query, criteriaBuilder) -> {
            if (date==null){
                return null;
            }
            Instant from =date.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant to=date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            return criteriaBuilder.and(
                    criteriaBuilder.greaterThanOrEqualTo(root.get("remind"),from),
                    criteriaBuilder.lessThan(root.get("remind"),to)
            );

        });
    }
}
