package com.example.TaskSync.Sync;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sync_queue")
public class SyncQueue {

    @Id
    private UUID id;

    @Column(name = "task_id")
    private UUID taskId;   // reference to Task

    @Column(nullable = false, length = 20)
    private String operation;   // "CREATE" / "UPDATE" / "DELETE"

    @Column(nullable = false, name = "created_at")
    private Instant createdAt;  // when the entry was added

    @Column(name = "processed_at")
    private Instant processedAt; // when synced

    @Lob
    @Column(nullable = false, name = "payload_json")
    private String payloadJson; // snapshot JSON

    @Column(nullable = false, length = 20)
    private String status = "PENDING"; // "PENDING" / "SYNCED" / "ERROR"

    @Column(nullable = false)
    private int retries = 0;

    @Column(name = "error_message")
    private String errorMessage;

    // Auto-generate id and createdAt if missing
    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }
}