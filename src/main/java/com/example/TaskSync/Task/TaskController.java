package com.example.TaskSync.Task;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    // GET all non-deleted
    @GetMapping
    public ResponseEntity<List<Task>> list() {
        return ResponseEntity.ok(service.listActive());
    }

    // GET by id
    @GetMapping("/{id}")
    public ResponseEntity<Task> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.get(id));
    }

    // CREATE
    @PostMapping
    public ResponseEntity<Task> create(@Valid @RequestBody Task task) {
        return ResponseEntity.ok(service.create(task));
    }

    // UPDATE (soft merge)
    @PutMapping("/{id}")
    public ResponseEntity<Task> update(@PathVariable UUID id, @RequestBody Task patch) {
        return ResponseEntity.ok(service.update(id, patch));
    }

    // SOFT DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
