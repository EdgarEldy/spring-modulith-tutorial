## Branch

<!-- e.g. feature/core-architecture -->

## Task checklist

<!-- Copy the relevant section's checklist from README.md and check off each item. -->

- [ ]

## Commit summary

<!-- One line per group of commits, summarizing what changed and why. -->

## Test checklist

- [ ] `ModularityTests` (`ApplicationModules.verify()`) passes
- [ ] Unit, repository, controller and `@ApplicationModuleTest` tests pass
- [ ] `mvnw verify` is green on the whole project

## Code review checklist

- [ ] No module imports another module's entity, repository or internal class (only its `api/` named interface or an event)
- [ ] Entities and repositories are package-private wherever the package layout allows it
- [ ] Every endpoint answers with `ApiResponse<T>` (`PageResponse<T>` for lists), errors go through `GlobalExceptionHandler`
- [ ] Authorization is role-only (`hasRole('ADMIN')` / `hasRole('USER')`), no fine-grained permission model
- [ ] No business logic in controllers
- [ ] The database schema only changes through a Flyway migration, with no foreign key across modules
- [ ] Commits are atomic, follow Conventional Commits, no em dash
