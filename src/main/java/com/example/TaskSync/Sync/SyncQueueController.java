package com.example.TaskSync.Sync;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;
import java.time.Instant;

@RestController
@RequestMapping("/api/sync")
public class SyncQueueController {

    public final SyncQueueService syncQService;

    public SyncQueueController(SyncQueueService syncQService) {
        this.syncQService = syncQService;
    }

    @PostMapping("/sync-tasks")
    public ResponseEntity<Map<String, Object>> SyncTasks(@RequestBody List<SyncQueue> operations) {
        int synced = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();

        for (SyncQueue op : operations) {
            try {
                // Ensure ID + createdAt set if client didn’t provide
                if (op.getId() == null) op.setId(UUID.randomUUID());
                if (op.getCreatedAt() == null) op.setCreatedAt(Instant.now());
                op.setStatus("PENDING");

                syncQService.processOperation(op);
                synced++;
            } catch (Exception e) {
                failed++;
                errors.add("TaskId " + op.getTaskId() + ": " + e.getMessage());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("synced", synced);
        result.put("failed", failed);
        result.put("errors", errors);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/status")
    public ResponseEntity<List<SyncQueue>> status() {
        return ResponseEntity.ok(syncQService.getUnprocessedOrFailed());
    }

}
