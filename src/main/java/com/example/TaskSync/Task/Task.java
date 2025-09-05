package com.example.TaskSync.Task;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tasks")
public class Task {

    @Id
    private UUID id;

    @NotBlank
    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private boolean completed = false;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;   // soft delete

    @Column(nullable = false, name = "created_at")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Instant createdAt;

    @Column(nullable = false, name = "updated_at")
    private Instant updatedAt;         // used for LWW

    @Column(name = "last_synced_at")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Instant lastSyncedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = createdAt;
    }
}
