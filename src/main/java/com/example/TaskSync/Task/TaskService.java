package com.example.TaskSync.Task;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class TaskService {

    private final TaskRepo repo;

    public TaskService(TaskRepo repo) {
        this.repo = repo;
    }

    // READ
    public List<Task> listActive() {
        return repo.findByDeletedFalseOrderByUpdatedAtDesc();
    }

    public Task get(UUID id) {
        return repo.findById(id).orElseThrow(() ->
                new IllegalArgumentException("Task not found: " + id)
        );
    }

    // CREATE
    @Transactional
    public Task create(Task task) {
        // ignore any client-sent id/createdAt/lastSyncedAt
        task.setId(null);
        task.setDeleted(false);
        task.setCreatedAt(Instant.now());
        task.setUpdatedAt(task.getCreatedAt());
        task.setLastSyncedAt(null);
        return repo.save(task);
    }

    // UPDATE
    @Transactional
    public Task update(UUID id, Task patch) {
        Task existing = get(id);
        // update mutable fields
        if (patch.getTitle() != null) existing.setTitle(patch.getTitle());
        existing.setDescription(patch.getDescription());
        existing.setCompleted(patch.isCompleted());
        // bump updatedAt (server-side)
        existing.setUpdatedAt(Instant.now());
        return repo.save(existing);
    }

    // SOFT DELETE
    @Transactional
    public void softDelete(UUID id) {
        Task t = get(id);
        if (!t.isDeleted()) {
            t.setDeleted(true);
            t.setUpdatedAt(Instant.now());
            repo.save(t);
        }
    }
}
