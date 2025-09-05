package com.example.TaskSync.Sync;

import com.example.TaskSync.Common.ConflictResolver;
import com.example.TaskSync.Task.Task;
import com.example.TaskSync.Task.TaskRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class SyncQueueService {

    private final SyncQueueRepo repo;
    private final TaskRepo taskRepo;
    private final ConflictResolver conflictResolver;
    private final ObjectMapper objectMapper;

    public SyncQueueService(SyncQueueRepo repo,
                            TaskRepo taskRepo,
                            ConflictResolver conflictResolver,
                            ObjectMapper objectMapper) {
        this.repo = repo;
        this.taskRepo = taskRepo;
        this.conflictResolver = conflictResolver;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void processOperation(SyncQueue item) {
        if (item.getId() == null) item.setId(UUID.randomUUID());
        if (item.getCreatedAt() == null) item.setCreatedAt(Instant.now());
        item.setStatus("PENDING");
        repo.save(item);

        try {
            // Parse task snapshot from JSON
            Task incoming = objectMapper.readValue(item.getPayloadJson(), Task.class);

            // Ensure incoming has an ID if taskId provided on the queue
            if (incoming.getId() == null && item.getTaskId() != null) {
                incoming.setId(item.getTaskId());
            }

            // If incoming has no updatedAt, set it now so it will be considered recent.
            if (incoming.getUpdatedAt() == null) {
                incoming.setUpdatedAt(Instant.now());
            } else {
                // bump incoming updatedAt to now so offline changes win (this makes incoming the latest)
                incoming.setUpdatedAt(Instant.now());
            }

            switch (item.getOperation().toUpperCase()) {
                case "CREATE" -> handleCreate(incoming);
                case "UPDATE" -> handleUpdate(incoming);
                case "DELETE" -> handleDelete(incoming);
                default -> throw new IllegalArgumentException("Unknown operation: " + item.getOperation());
            }

            item.setStatus("SYNCED");
            item.setProcessedAt(Instant.now());
        } catch (Exception e) {
            item.setStatus("ERROR");
            item.setErrorMessage(e.getMessage());
        }

        repo.save(item);
    }

    // --- handlers ---

    private void handleCreate(Task incoming) {
        if (incoming.getId() == null) incoming.setId(UUID.randomUUID());
        if (incoming.getCreatedAt() == null) incoming.setCreatedAt(Instant.now());
        if (incoming.getUpdatedAt() == null) incoming.setUpdatedAt(incoming.getCreatedAt());
        incoming.setDeleted(false);
        taskRepo.save(incoming);
    }

    private void handleUpdate(Task incoming) {
        if (incoming.getId() == null) {
            throw new IllegalArgumentException("Update requires task id");
        }

        Task existing = taskRepo.findById(incoming.getId()).orElse(null);

        if (existing == null) {
            // If not present, treat as create
            handleCreate(incoming);
            return;
        }

        // Use conflict resolver with forceOverwrite = true so incoming overwrites existing fields
        Task merged = conflictResolver.applyLastWriteWins(existing, incoming, true);

        // keep createdAt and id from existing (resolver already preserved that)
        taskRepo.save(merged);
    }

    private void handleDelete(Task incoming) {
        if (incoming.getId() == null) {
            throw new IllegalArgumentException("Delete requires task id");
        }

        Task existing = taskRepo.findById(incoming.getId()).orElse(null);
        if (existing != null) {
            existing.setDeleted(true);
            existing.setUpdatedAt(incoming.getUpdatedAt() != null ? incoming.getUpdatedAt() : Instant.now());
            taskRepo.save(existing);
        }
    }

    // Convenience read method if you need it
    public List<SyncQueue> getUnprocessedOrFailed() {
        return repo.findByStatusInOrderByCreatedAtAsc(List.of("PENDING", "ERROR"));
    }
}