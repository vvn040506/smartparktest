# Bugfix Requirements Document

## Introduction

The SmartPark application is experiencing critical database connection pool issues when deployed to Render.com with Supabase PostgreSQL. The connection pool is reusing database connections that contain stale prepared statements and aborted transactions, causing all database operations to fail. This renders the application non-functional in production, preventing staff login, account creation, and parking slot queries.

The bug manifests in two primary error patterns:
1. **Prepared Statement Conflicts**: PostgreSQL server-side prepared statements (e.g., "S_5", "S_6") persist across connection reuse, causing "already exists" errors
2. **Transaction Abort State**: Connections with aborted transactions are returned to the pool without proper rollback, causing subsequent operations to fail with "current transaction is aborted" errors

The root cause is that HikariCP connection pool is not properly cleaning connection state between reuse cycles, allowing PostgreSQL session-level state to persist.

## Bug Analysis

### Current Behavior (Defect)

1.1 WHEN a database connection is returned to the HikariCP pool after use THEN the system reuses the connection without clearing PostgreSQL server-side prepared statements, causing "ERROR: prepared statement 'S_X' already exists" on subsequent queries

1.2 WHEN a database transaction fails or is aborted THEN the system returns the connection to the pool without rolling back the transaction, causing "ERROR: current transaction is aborted, commands ignored until end of transaction block" on subsequent operations

1.3 WHEN the scheduled task `cleanupExpiredTokens()` executes with `@Transactional` annotation THEN the system may leave connections in an inconsistent state if exceptions occur during token/account deletion operations

1.4 WHEN staff login attempts are made after connection pool contamination THEN the system fails with "Unable to commit against JDBC Connection" error, preventing authentication

1.5 WHEN new staff accounts are created with email verification THEN the system fails with "Unable to commit against JDBC Connection" error, preventing account creation

1.6 WHEN parking slot queries are executed using contaminated connections THEN the system fails with prepared statement errors, preventing slot status retrieval

### Expected Behavior (Correct)

2.1 WHEN a database connection is returned to the HikariCP pool after use THEN the system SHALL execute a connection test query that clears all server-side prepared statements before the connection is reused

2.2 WHEN a database transaction fails or is aborted THEN the system SHALL automatically roll back the transaction and reset the connection state before returning it to the pool

2.3 WHEN the scheduled task `cleanupExpiredTokens()` executes THEN the system SHALL properly handle transaction boundaries with appropriate rollback on exceptions, ensuring connections are returned to the pool in a clean state

2.4 WHEN staff login attempts are made THEN the system SHALL successfully commit authentication transactions using clean database connections

2.5 WHEN new staff accounts are created with email verification THEN the system SHALL successfully commit account creation transactions using clean database connections

2.6 WHEN parking slot queries are executed THEN the system SHALL successfully execute queries without prepared statement conflicts using clean database connections

### Unchanged Behavior (Regression Prevention)

3.1 WHEN database operations succeed without errors THEN the system SHALL CONTINUE TO commit transactions normally and return connections to the pool

3.2 WHEN connection pool size limits are configured THEN the system SHALL CONTINUE TO respect maximum-pool-size and minimum-idle settings

3.3 WHEN connection timeout and lifetime settings are configured THEN the system SHALL CONTINUE TO enforce connection-timeout, idle-timeout, and max-lifetime parameters

3.4 WHEN JPA/Hibernate operations execute successfully THEN the system SHALL CONTINUE TO use the configured PostgreSQL dialect and batch settings

3.5 WHEN the application starts up THEN the system SHALL CONTINUE TO initialize the connection pool with the configured DATABASE_URL from environment variables

3.6 WHEN non-transactional read operations are performed THEN the system SHALL CONTINUE TO execute queries without requiring explicit transaction management
