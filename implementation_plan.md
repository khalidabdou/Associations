# Implementation Plan - Database and UI Integration

## Goal
Integrate a SQLite database using SQLDelight and build a UI to manage Zones and Subscribers for a KMP project (Android & Desktop).

## Proposed Changes

### Database Layer
- [x] **Schema**: Create `AppDatabase.sq` in `commonMain/sqldelight`.
- [x] **Configuration**: Add SQLDelight plugin and dependencies.
- [x] **Drivers**: Implement `DatabaseDriverFactory` for Android and Desktop.
- [x] **Repository**: Create `AppRepository` to abstract database access.

### UI Layer
- [x] **ViewModel**: Create `AppViewModel` to expose data as `StateFlow`.
- [x] **Screens**: Update `App.kt` to show lists of Zones and Subscribers.
- [x] **Interactions**: Add dialogs for creating new entries.
- [x] **Entry Points**: Initialize database in `MainActivity` and `main.kt`.

## Verification Plan

### Manual Verification
- **Android**: Launch app, add a zone, add a subscriber linked to that zone. Verify list updates.
- **Desktop**: Launch app, perform same actions. Verify persistence (restart app).

### Automated Tests
- (Future) Add unit tests for `AppRepository` using `JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)`.
