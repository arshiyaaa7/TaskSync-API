package com.example.TaskSync.Sync;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface SyncQueueRepo extends JpaRepository<SyncQueue, UUID> {
    List<SyncQueue> findByStatusOrderByCreatedAtAsc(String status);
    List<SyncQueue> findByStatusInOrderByCreatedAtAsc(List<String> statuses);
    List<SyncQueue> findByStatusIn(List<String> pending);
}
