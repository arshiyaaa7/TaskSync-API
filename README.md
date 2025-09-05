# TaskSync API – Spring Boot Implementation

## 📌 Overview
This is a **task management application** with **offline → online sync support**.

- If the user is **online**, tasks are directly created/updated/deleted in the main `tasks` table.  
- If the user is **offline**, changes are stored in a `sync_queue` (temporary cache). When back online, the queue is processed and merged into the main table using **Last-Write-Wins conflict resolution**.  
- This ensures **no data is lost** and conflicts are always resolved deterministically.

Originally, the challenge repo was provided in **TypeScript/JavaScript**. I migrated the design to **Spring Boot + Java** because I am much more confident in this stack, and it provides a clean MVC architecture with JPA + H2 database for demo purposes.

---

## ⚙️ Tech Stack
- **Spring Boot** (REST APIs, dependency injection, lifecycle)  
- **Spring Data JPA** (database interaction)  
- **H2 Database** (in-memory DB for demo/testing)  
- **Lombok** (reduce boilerplate with annotations)  
- **Validation** (request validation with Jakarta Bean Validation)  
- **Jackson** (JSON serialization/deserialization)  

---

## 🏗 Architecture (MVC)
- **Entity** → Represents database tables (`Task`, `SyncQueue`).  
- **Repository** → Interfaces extending `JpaRepository` (for DB CRUD).  
- **Service** → Contains business logic (`TaskService`, `SyncQueueService`).  
- **Controller** → REST endpoints (`TaskController`, `SyncQueueController`).  
- **Common Utilities** → `ConflictResolver`, `GlobalExceptionHandler`.  

**Workflow:**

<img width="1317" height="211" alt="image" src="https://github.com/user-attachments/assets/00a2196d-7f76-4080-971d-cf61c1528167" />


- **Frontend** decides:  
  - Online → call `/api/tasks`  
  - Offline → call `/api/sync/sync-tasks`  

- **Service** applies business rules, especially in `SyncQueueService`.  
- **Repository** delegates to DB (JPA takes care of SQL).  
- **Database** has two tables: `tasks`, `sync_queue`.  

---

## 📂 Database Structure

### Task Table (`tasks`)
| Column         | Description                       |
|----------------|-----------------------------------|
| id (UUID)      | Unique identifier for task        |
| title          | Task title                        |
| description    | Task description                  |
| completed      | Boolean flag                      |
| deleted        | Boolean soft delete flag          |
| created_at     | Creation timestamp                |
| updated_at     | Last update timestamp (LWW logic) |
| last_synced_at | (Optional) last time synced       |

### Sync Queue Table (`sync_queue`)
| Column         | Description                                      |
|----------------|--------------------------------------------------|
| id (UUID)      | Unique sync operation id                         |
| task_id        | ID of task this operation belongs to             |
| operation      | CREATE / UPDATE / DELETE                         |
| payload_json   | Snapshot of task (as JSON string)                |
| status         | PENDING / SYNCED / ERROR                         |
| created_at     | When added to queue                              |
| processed_at   | When applied to DB                               |
| retries        | Retry count (simple error handling)              |
| error_message  | Error message if processing failed               |

---

## 🔑 Endpoints

### Tasks API (`/api/tasks`)
- `POST /api/tasks` → Create new task  
- `GET /api/tasks` → Get all active (non-deleted) tasks  
- `GET /api/tasks/{id}` → Get a task by ID  
- `PUT /api/tasks/{id}` → Update task (title, desc, completed)  
- `DELETE /api/tasks/{id}` → Soft delete task  

### Sync API (`/api/sync`)
- `POST /api/sync/sync-tasks` → Accepts list of offline operations  
  - Each item has: `taskId`, `operation`, `payloadJson` (Task snapshot JSON as string)  
  - Processes and merges into DB  
- `GET /api/sync/status` → Shows pending/failed sync queue entries  

---

## 🔄 Service Logic – SyncQueueService
This is the **heart of offline sync**.

1. **Receive operations** → Save them in `sync_queue` with status `PENDING`.  
2. **Parse payloadJson** → Convert string into `Task`.  
3. **Bump updatedAt to now** → Ensures offline changes are treated as latest.  
4. **Switch on operation**:  
   - `CREATE` → Insert task if new, else merge if ID already exists.  
   - `UPDATE` → Load task, merge with existing using `ConflictResolver`.  
   - `DELETE` → Soft delete task (`deleted=true`).  
5. **Mark queue entry** → Update status to `SYNCED` (or `ERROR` if failed).  

---

## 🔄 Conflict Resolver – Last Write Wins
The **ConflictResolver** ensures **consistent merges** when the same task is modified both offline and online.

### Logic:
- Compare `updatedAt` of incoming vs DB.  
- If incoming is newer → copy its fields (title, description, completed, deleted) into DB.  
- Keep `id` and `createdAt` unchanged.  
- Set DB `updatedAt` to incoming (or now).

---

## 🧪 Testing with Postman

### 1️⃣ Create Tasks Online
**Request**
```
POST /api/tasks
Content-Type: application/json
```
→ Copy the id from the response.

2. **Simulate offline sync update**
```
[
  {
    "taskId": "PUT-TASK-ID-HERE",
    "operation": "UPDATE",
    "payloadJson": "{\"id\":\"PUT-TASK-ID-HERE\",\"title\":\"Buy groceries\",\"description\":\"Milk, Eggs, Bread, Butter\",\"completed\":false,\"deleted\":false,\"updatedAt\":\"2025-09-05T12:00:00Z\"}"
  }
]
```

3. **Check merged tasks**
   ```GET /api/tasks```


→ The groceries task will now have “Butter” in description.  

---

## ✅ Key Features Achieved
- Clean **Spring Boot MVC** implementation.  
- **Two-table design** for tasks + offline sync queue.  
- **Conflict resolution** using Last-Write-Wins.  
- **No data loss** when offline.  
- **Simple APIs** for online and offline flows.  
- **H2 in-memory DB** for demo & easy testing.  

---

This project demonstrates **real-world sync handling**:  
- Strong MVC design,  
- Queue-based offline storage,  
- Deterministic conflict resolution.  

