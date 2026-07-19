package com.seibel.jobs.common.domain;

import com.seibel.jobs.common.enums.ActiveEnum;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor  // ✅ REQUIRED for subclass to compile with @NoArgsConstructor
public abstract class BaseDomain {
    private Long id;
    private String extid;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    private ActiveEnum active;
}
