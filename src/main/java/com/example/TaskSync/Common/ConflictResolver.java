package com.example.TaskSync.Common;

import com.example.TaskSync.Task.Task;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class ConflictResolver {

    /**
     * Original last-write-wins: incoming wins only if incoming.updatedAt > existing.updatedAt
     */
    public Task applyLastWriteWins(Task existing, Task incoming) {
        Instant in = incoming.getUpdatedAt();
        Instant ex = existing.getUpdatedAt();

        boolean incomingWins = ex == null || (in != null && in.isAfter(ex));
        if (incomingWins) {
            existing.setTitle(incoming.getTitle());
            existing.setDescription(incoming.getDescription());
            existing.setCompleted(incoming.isCompleted());
            existing.setDeleted(incoming.isDeleted());
            existing.setUpdatedAt(in != null ? in : Instant.now());
        }
        return existing;
    }

    /**
     * Overload with force flag.
     * If forceOverwrite is true -> always copy incoming fields and set updatedAt to incoming or now.
     * If false -> behave like standard LWW.
     */
    public Task applyLastWriteWins(Task existing, Task incoming, boolean forceOverwrite) {
        if (forceOverwrite) {
            // Always copy mutable fields from incoming (but keep id and createdAt from existing)
            existing.setTitle(incoming.getTitle());
            existing.setDescription(incoming.getDescription());
            existing.setCompleted(incoming.isCompleted());
            existing.setDeleted(incoming.isDeleted());
            Instant in = incoming.getUpdatedAt();
            existing.setUpdatedAt(in != null ? in : Instant.now());
            return existing;
        } else {
            return applyLastWriteWins(existing, incoming);
        }
    }
}