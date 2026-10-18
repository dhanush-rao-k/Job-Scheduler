# Job Scheduler

A Spring Boot job scheduling service backed by PostgreSQL. The project focuses on reliable job execution, concurrent worker coordination, execution ownership, lease recovery, and automated testing.

## Tech Stack

- **Java 21**
- **Spring Boot**
- **Spring Data JPA / Hibernate**
- **PostgreSQL 16**
- **Apache Kafka 4**
- **Maven**
- **JUnit 5, Mockito, Spring Boot Test**
- **GitHub Actions**

## Features

- **Persistent jobs:** Store scheduled jobs in PostgreSQL.
- **Due-job polling:** Periodically look for pending jobs whose scheduled execution time has arrived.
- **Concurrent claiming:** Use PostgreSQL row locking with `FOR UPDATE SKIP LOCKED` to coordinate workers.
- **Bounded execution:** Limit concurrent execution and queue capacity.
- **Execution ownership:** Use execution tokens to prevent stale workers from overwriting newer job state.
- **Leases and recovery:** Renew execution leases and recover jobs whose leases expire.
- **Retries:** Retry failed executions with backoff, subject to the configured attempt limit.
- **Execution tracking:** Record execution attempts and outcomes.
- **Supported job types:** `DEFAULT`, `EMAIL`, `HTTP`, and `WEBHOOK`.
- **Automated verification:** Run tests through Maven and GitHub Actions.

## Architecture

The main execution flow is:

1. The scheduler periodically checks for expired leases and recovers eligible jobs.
2. It claims due jobs through the job service.
3. The claim operation uses PostgreSQL locking to coordinate concurrent workers.
4. The executor processes the claimed job using its execution token.
5. Lease renewal maintains ownership while execution is in progress.
6. Completion or failure updates the job only if the worker still owns that execution.
7. Failed jobs can be retried according to the configured retry policy.

PostgreSQL remains the source of truth for job state.

Job types are resolved by `JobActionResolver`. Unknown values, including
`DEMO`, fail as unsupported job types; use `DEFAULT` for a deterministic
demonstration job.

## Prerequisites

- JDK 21
- PostgreSQL 16 or a compatible PostgreSQL installation
- Docker Desktop with Docker Compose
- Git

## Configuration

The application reads its database settings from environment variables, with local-development defaults:

| Variable | Default | Purpose |
|---|---|---|
| `DB_USERNAME` | `postgres` | Database username |
| `DB_PASSWORD` | `postgres` | Database password |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka broker address |

The JDBC URL defaults to:

`jdbc:postgresql://localhost:5432/job_scheduler`

Create the `job_scheduler` database in PostgreSQL before starting the application.

For local development, the defaults work when your local PostgreSQL instance uses the same credentials. For other environments, set `DB_USERNAME` and `DB_PASSWORD` before launching the application.

The included Compose setup exposes Kafka to applications running on the host at
`localhost:9092`. If the application is later run as a Compose service, set
`KAFKA_BOOTSTRAP_SERVERS=kafka:29092` so it uses Kafka's internal listener.

**PowerShell example:**

```powershell
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "your-local-password"
```

Use appropriate credentials for your environment. Do not commit real secrets to version control.

## Running Locally

Clone the repository and enter the project directory:

```powershell
git clone https://github.com/dhanush-rao-k/Job-Scheduler.git
cd Job-Scheduler
```

Start PostgreSQL and Kafka with Docker Compose:

```powershell
docker compose up -d postgres kafka
```

Verify that Kafka is reachable:

```powershell
docker compose exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
```

Configure the database and Kafka environment variables if needed, then run:

```powershell
.\mvnw spring-boot:run
```

## Running Tests

Run the complete verification lifecycle:

```powershell
.\mvnw clean verify
```

The GitHub Actions workflow provisions a PostgreSQL 16 service and runs the same Maven verification command on pushes and pull requests targeting `main`.

## Project Status

The project is being developed incrementally, with an emphasis on correctness under concurrent execution, recovery after worker interruption, and reliable state transitions.

See the source code and tests for the current implementation details.

## Future Development

Potential extensions include distributed rate limiting, event-driven processing, transactional outbox patterns, and broader operational observability.
