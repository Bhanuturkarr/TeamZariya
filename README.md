# AI Tutor backend

Spring Boot 4.1.2 REST API using Java 25, PostgreSQL 16 with pgvector, Flyway, Spring Security HTTP Basic, and Springdoc OpenAPI. AI calls use the deterministic mock provider by default; configure an OpenAI key and `AI_PROVIDER=openai` to use the live provider.

## Run locally

1. Install Java 25 and Docker Desktop. From this folder, create your local settings file by copying `.env.example` to `.env`, then set a PostgreSQL password.
2. Start PostgreSQL with `docker compose up -d database`.
3. In PowerShell, load the local database password, set a private password for the legacy `demo` account, and start the API:

   ```powershell
   $envSettings = Get-Content -LiteralPath '.env' | ConvertFrom-StringData
   $env:SPRING_DATASOURCE_PASSWORD = $envSettings.POSTGRES_PASSWORD
   $secureDemoPassword = Read-Host -AsSecureString 'Set the demo account password'
   $env:AITUTOR_BOOTSTRAP_PASSWORD = [System.Net.NetworkCredential]::new('', $secureDemoPassword).Password
   $env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-25.0.5.8-hotspot'
   $env:Path = "$env:JAVA_HOME\bin;$env:Path"
   .\mvnw.cmd spring-boot:run
   ```

   Keep this PowerShell window open while using the API. The demo account owns projects created before Phase 7. It is recreated with the password above each time the API starts.

4. In a browser, open [Swagger UI](http://localhost:8080/swagger-ui.html). Choose **Authorize**, enter `demo` and the password set above, then try the endpoints. The health endpoint is public.

For a second learner account, register once with `POST /api/v1/auth/register` from Swagger. All project, material, assessment, plan, lesson, exercise, and report routes require Basic authentication. Each account only sees its own projects; an inaccessible resource returns 404.

## API routes

| Area | Routes |
|---|---|
| Status and accounts | `GET /api/v1/health`, `POST /api/v1/auth/register` |
| Projects | `POST /api/v1/projects`, `GET /api/v1/projects`, `GET /api/v1/projects/{projectId}`, `DELETE /api/v1/projects/{projectId}` |
| PDF materials | `POST /api/v1/projects/{projectId}/materials`, `GET /api/v1/projects/{projectId}/materials`, `GET /api/v1/materials/{materialId}`, `GET /api/v1/materials/{materialId}/topics`, `POST /api/v1/materials/{materialId}/reprocess` |
| AI help | `POST /api/v1/materials/{materialId}/ai/answer`, `/ai/questions`, `/ai/topics` |
| Diagnostic assessment | `POST /api/v1/projects/{projectId}/assessments/diagnostic`, `GET /api/v1/assessments/{assessmentId}`, `POST /api/v1/assessments/{assessmentId}/submit`, `GET /api/v1/assessments/{assessmentId}/results` |
| Personalized learning | `POST /api/v1/projects/{projectId}/learning-plans`, `GET /api/v1/learning-plans/{planId}`, `GET /api/v1/learning-plans/{planId}/lessons`, `GET /api/v1/lessons/{lessonId}`, `POST /api/v1/lessons/{lessonId}/exercises`, `POST /api/v1/exercises/{exerciseId}/submit` |
| Progress reports | `GET /api/v1/projects/{projectId}/progress`, `/mastery`, `/recommendations`, `/activity` |

`GET /v3/api-docs` returns the OpenAPI JSON definition. Swagger UI includes Basic authentication for protected operations.

## Phase 7 reports

- **Progress** summarizes tracked topics, average mastery, mastered/review topics, plan completion, assessment and exercise attempts, and latest activity time.
- **Mastery** lists per-topic score, answer totals, and the configured review/mastery flags.
- **Recommendations** returns the next incomplete learning-plan item, or the next setup action when there is no assessment or plan.
- **Activity** combines assessment and practice attempts in newest-first order.

The account bootstrap is for local development. Change its password before exposing this service beyond localhost, and serve authenticated traffic over HTTPS. User registration creates independent accounts; there is no password reset or email verification flow in this MVP.

## Configuration

See `.env.example` for PostgreSQL, AI provider, mastery thresholds, and the bootstrap password. The API expects `SPRING_DATASOURCE_PASSWORD` in the process environment. `AI_PROVIDER=mock` works without an external AI key. Uploaded PDFs are stored under `data/materials`; do not commit that directory or `.env`.

## Verify

Run the integration suite from PowerShell after loading `.env` and Java 25 as above:

```powershell
.\mvnw.cmd test
```

The suite uses the configured local PostgreSQL database and checks project/material workflows, assessment scoring, lesson and exercise feedback, reports, Basic authentication, user registration, and project ownership isolation.
