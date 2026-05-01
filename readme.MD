# Student Complaint Management System

A full-stack campus operations platform that lets students raise complaints with image evidence, routes them through AI-based severity classification and department assignment, and tracks resolution via email acknowledgement and a 7-day SLA workflow.

**Stack:** Spring Boot 3 · PostgreSQL · React (Vite) · Tailwind · shadcn/ui · TanStack Query · React Router

---

## Features

- Student and admin roles with JWT + BCrypt authentication
- Complaint submission with image uploads (backend-mediated via imgbb)
- AI/VLM-based severity classification (`LOW` / `MEDIUM` / `HIGH` / `CRITICAL`) and optional department suggestion
- Automatic department assignment when AI suggests a match; manual admin assignment otherwise
- Email sent to department authority on assignment, with a secure tokenized acknowledgement link
- 7-day SLA tracking — students can escalate to the Director if unacknowledged or unresolved
- Scheduled job flags overdue complaints and appends a timeline event
- Full complaint audit trail (`complaint_events`)
- Department CRUD, admin complaint queue with filters and pagination
- Swagger UI for all backend endpoints

---

## Project Structure

```
scms/
├── backend/          # Spring Boot API
├── frontend/         # React application (Vite)
```

---

## Backend Setup

### Prerequisites

- Java 17+
- Maven 3.9+
- PostgreSQL 14+
- SMTP credentials (outbound email)
- imgbb API key
- External AI/VLM API endpoint (a fallback of `MEDIUM` severity with no department is used on failure)

### 1. Create the database

```sql
CREATE DATABASE scms;
```

### 2. Configure `application.yml`

Edit `backend/src/main/resources/application.yml` and fill in:

| Key | Description |
|---|---|
| `spring.datasource.*` | PostgreSQL connection |
| `app.jwt.secret` | Secret key (32+ bytes) |
| `spring.mail.*` | SMTP host, port, credentials |
| `app.imgbb.api-key` | imgbb upload key |
| `app.ai.base-url` / `endpoint` / `api-key` | External AI classifier |
| `app.public-urls.backend-base-url` | e.g. `http://localhost:8080` |
| `app.public-urls.frontend-base-url` | e.g. `http://localhost:5173` |

### 3. Run

```bash
cd backend
mvn spring-boot:run
```

### 4. API docs

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/api-docs

---

## Frontend Setup

### Prerequisites

- Node.js 18+
- npm 9+

### 1. Configure environment

Create `frontend/.env`:

```env
VITE_API_BASE_URL=http://localhost:8080
```

### 2. Install and run

```bash
cd frontend
npm install
npm run dev
```

### 3. Open the app

http://localhost:5173

---

## Database Schema

### Enums

| Enum | Values |
|---|---|
| `user_role` | `STUDENT`, `ADMIN` |
| `complaint_severity` | `LOW`, `MEDIUM`, `HIGH`, `CRITICAL` |
| `complaint_status` | `NEW` → `AI_CLASSIFIED` → `PENDING_ADMIN_ASSIGNMENT` / `ASSIGNED_TO_DEPARTMENT` → `EMAIL_SENT` → `ACK_RECEIVED` → `ACTION_TAKEN` → `RESOLVED_BY_STUDENT` / `ESCALATED_TO_DIRECTOR` → `CLOSED` |
| `complaint_event_type` | `STATUS_CHANGE`, `EMAIL_SENT`, `ACK_RECEIVED`, `ADMIN_ASSIGNED`, `ESCALATED`, `COMMENT`, `AI_CLASSIFIED` |
| `event_actor_type` | `STUDENT`, `ADMIN`, `SYSTEM` |

### Core Tables

**`users`** — `id`, `name`, `email`, `password_hash`, `role`, `created_at`, `updated_at`

**`departments`** — `id`, `name`, `authority_email`, `created_at`, `updated_at`

**`complaints`** — `id`, `student_id`, `title`, `description`, `area`, `complaint_date`, `ai_severity`, `ai_department_id`, `assigned_department_id`, `assigned_by_admin_id`, `status`, `email_sent_at`, `sla_due_at`, `ack_received_at`, `student_resolved_at`, `escalated_at`, `ai_raw_response_json`, `created_at`, `updated_at`

**`complaint_images`** — `id`, `complaint_id`, `image_url`, `delete_hash`, `created_at`

**`complaint_events`** — `id`, `complaint_id`, `event_type`, `message`, `created_by`, `created_at`

---

## Complaint Workflow

```
Student submits complaint
        │
        ▼
  AI classifies severity
        │
   ┌────┴────┐
   │         │
AI suggests  AI fails / no suggestion
department        │
   │         ▼
   │    PENDING_ADMIN_ASSIGNMENT
   │         │
   │    Admin assigns department
   │         │
   └────┬────┘
        ▼
  Email sent to department
  SLA starts (7-day window)
        │
        ▼
  Department acknowledges via secure link
        │
   ┌────┴────┐
   │         │
Student   No ack / unsatisfied
satisfied      │
   │       Student escalates
   ▼       to Director
CLOSED
```

---

## API Reference

### Auth

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/auth/signup` | Public | Register student or admin |
| `POST` | `/api/auth/login` | Public | Login, returns JWT |
| `GET` | `/api/auth/me` | JWT | Current user info |

### Uploads

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/uploads/images` | JWT | Upload images (`multipart/form-data`, field: `files[]`) |

### Student Routes

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/student/complaints` | Student JWT | Create a complaint |
| `GET` | `/api/student/complaints` | Student JWT | List own complaints |
| `GET` | `/api/student/complaints/{id}` | Student JWT | Get complaint detail |
| `POST` | `/api/student/complaints/{id}/resolve` | Student JWT | Mark as resolved |
| `POST` | `/api/student/complaints/{id}/escalate` | Student JWT | Escalate to Director |

### Admin Routes

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/api/admin/complaints` | Admin JWT | List all complaints (filterable) |
| `GET` | `/api/admin/complaints/{id}` | Admin JWT | Get complaint detail |
| `POST` | `/api/admin/complaints/{id}/assign-department` | Admin JWT | Manually assign department |
| `POST` | `/api/admin/complaints/{id}/override-ai` | Admin JWT | Override AI classification |
| `POST` | `/api/admin/complaints/{id}/resend-email` | Admin JWT | Resend department email |
| `POST` | `/api/admin/complaints/{id}/acknowledge-manual` | Admin JWT | Manually mark as acknowledged |
| `POST` | `/api/admin/complaints/{id}/action-taken` | Admin JWT | Mark action taken |
| `POST` | `/api/admin/complaints/{id}/internal-note` | Admin JWT | Add internal note |
| `GET` | `/api/admin/complaints/overdue` | Admin JWT | List overdue complaints |
| `GET` | `/api/admin/dashboard/kpis` | Admin JWT | Dashboard KPIs |
| `POST` | `/api/admin/departments` | Admin JWT | Create department |
| `GET` | `/api/admin/departments` | Admin JWT | List departments |
| `GET` | `/api/admin/departments/{id}` | Admin JWT | Get department |
| `PUT` | `/api/admin/departments/{id}` | Admin JWT | Update department |
| `DELETE` | `/api/admin/departments/{id}` | Admin JWT | Delete department |

### Department Acknowledgement (Public)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/complaints/{id}/acknowledge` | Token in body | Department acknowledges via secure tokenized link |

---

## Example Requests & Responses

### Signup

```json
POST /api/auth/signup
{
  "name": "Aarav Sharma",
  "email": "aarav.sharma@college.edu",
  "password": "StrongPass123!",
  "role": "STUDENT"
}
```

```json
{
  "success": true,
  "message": "Signup successful",
  "data": {
    "accessToken": "<jwt>",
    "user": {
      "id": "b33e9afe-47c0-4d9e-8d48-6f43798fe971",
      "name": "Aarav Sharma",
      "email": "aarav.sharma@college.edu",
      "role": "STUDENT",
      "createdAt": "2026-02-26T17:22:31.523Z"
    }
  }
}
```

### Upload Images

```
POST /api/uploads/images
Content-Type: multipart/form-data
Field: files[]
```

```json
{
  "success": true,
  "data": {
    "imageUrls": ["https://i.ibb.co/abcd1234/leak1.jpg"],
    "images": [{ "url": "https://i.ibb.co/abcd1234/leak1.jpg", "deleteHash": "R4nd0mHashOne" }]
  }
}
```

### Create Complaint

```json
POST /api/student/complaints
Authorization: Bearer <jwt>
{
  "title": "Water leakage in hostel corridor",
  "description": "Continuous leakage near room 213 causing slippery floor and smell.",
  "area": "Hostel A - 2nd Floor",
  "complaintDate": "2026-02-26",
  "images": [{ "url": "https://i.ibb.co/abcd1234/leak1.jpg", "deleteHash": "R4nd0mHashOne" }]
}
```

Response includes `aiSeverity`, `aiSuggestedDepartment`, `assignedDepartment`, `status`, `slaDueAt`, `timeline`, and full complaint detail.

### Department Acknowledgement

```json
POST /api/complaints/{id}/acknowledge
{
  "token": "<ack-jwt-token>",
  "message": "Complaint received and work order created."
}
```

```json
{
  "success": true,
  "data": {
    "status": "ACK_RECEIVED",
    "ackReceivedAt": "2026-02-27T10:03:17.220Z",
    "overdue": false
  }
}
```

---

## Frontend Routes

| Path | Role | Description |
|---|---|---|
| `/login` | Public | Login page |
| `/signup` | Public | Signup page |
| `/student/dashboard` | Student | Overview and recent complaints |
| `/student/complaints/new` | Student | Submit a new complaint |
| `/student/complaints` | Student | List own complaints |
| `/student/complaints/:id` | Student | Complaint detail and timeline |
| `/admin/dashboard` | Admin | KPIs and queue summary |
| `/admin/complaints` | Admin | Filtered complaint queue |
| `/admin/complaints/:id` | Admin | Detail, assign, note, escalate |
| `/admin/departments` | Admin | Department management |

---

## Future Scope

The codebase is structured with hooks in place for:

- **Chatbot module** — FAQ + complaint tracking via conversational UI
- **Department portal role** — `DEPARTMENT_AUTHORITY` role with its own views, without breaking the existing workflow
- **Notifications subsystem** — `notification_events` table ready for push/email/in-app
- **Event-driven integrations** — all state changes logged to `complaint_events`, ready for webhooks or message queues