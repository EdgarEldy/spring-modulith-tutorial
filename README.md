# spring-modulith-tutorial

A complete tutorial for structuring a **modular monolith** with **Spring Modulith**, on top of **Spring Boot 4.1.x** (Spring Framework 7, Java 17): one deployable application, organized into explicit modules whose boundaries are verified automatically, communicating through a durable, broker-free event mechanism.

This document is the **complete specification** of the project: it is meant to be followed step by step to implement each branch.

## Table of contents

- [Spring Modulith vs. multi-module Maven vs. microservices](#spring-modulith-vs-multi-module-maven-vs-microservices)
- [Core Spring Modulith concepts used in this tutorial](#core-spring-modulith-concepts-used-in-this-tutorial)
- [Tech stack](#tech-stack)
- [Data model](#data-model)
- [Branching strategy](#branching-strategy)
- [Project structure](#project-structure)
- [Standard response format](#standard-response-format)
- [feature/core-architecture](#featurecore-architecture)
- [feature/auth-module](#featureauth-module)
- [feature/catalog-module](#featurecatalog-module)
- [feature/customer-module](#featurecustomer-module)
- [feature/order-module](#featureorder-module)
- [feature/notification-module](#featurenotification-module)
- [feature/module-documentation](#featuremodule-documentation)
- [Order of work](#order-of-work)
- [Code conventions](#code-conventions)
- [Concepts covered](#concepts-covered)
- [How to follow this tutorial](#how-to-follow-this-tutorial)

## Spring Modulith vs. multi-module Maven vs. microservices

Three ways to keep a codebase from turning into a single tangle of classes that all depend on each other, at three different levels of enforcement:

- **Multi-module Maven**: boundaries are *physical* - each module is a separate Maven artifact with its own `pom.xml` and dependency list. The compiler itself refuses to let one module import a class from another unless it's declared as a dependency. Strongest enforcement, but each module builds separately and the whole codebase is a set of JARs wired together.
- **Microservices**: boundaries are physical *and* run as separate processes, usually separate deployables, sometimes separate teams/repos. Strongest isolation of all, at the cost of network calls between services, distributed data, and everything that comes with distribution (retries, eventual consistency, service discovery).
- **Spring Modulith**: boundaries are *logical*, enforced by a **test**, not the compiler. Every module is a plain Java package. Nothing physically stops a class in `order` from importing a class in `catalog.internal` - but `ApplicationModules.of(Application.class).verify()` fails the build if it does. One JAR, one process, one database, one deployment - but the same discipline about "who is allowed to call what" that multi-module and microservices enforce more strongly and more expensively.

The practical reason to reach for Modulith specifically: you get most of the *design* benefit of clear module boundaries (and a credible path to extracting a module into its own microservice later, since the boundary is already explicit) without paying the *operational* cost of distribution before you actually need it.

Concretely, extracting `catalog` later would mean: turning `CatalogApi`'s implementation from a direct Spring bean call into an HTTP or messaging client (a Feign client, a `RestClient`, or a Kafka consumer, depending on whether `order` needs it synchronously or not) that happens to implement the same interface, moving the `catalog` package into its own Spring Boot application with its own database, and replacing `order`'s in-process dependency on `catalog`'s `@NamedInterface` with a call to that new client. The point of `@NamedInterface` is that `order` never notices the difference on its own side - it already only ever called `CatalogApi`, never `catalog`'s internal classes.

## Core Spring Modulith concepts used in this tutorial

- **Application Module**: a direct sub-package of the main application package (`com.edgareldy.springmodulithtutorial.catalog`, `.order`, ...). Everything inside it is package-private by default from Modulith's point of view - only what's explicitly exposed is a module's public API.
- **Named Interface**: a sub-package explicitly marked (`@NamedInterface`) as part of a module's public API, so other modules may depend on it. Anything not marked this way is internal, and `verify()` treats reaching into it as a violation.
- **Event Publication Registry**: when a module publishes a Spring `ApplicationEvent`, Modulith persists a record of that publication (JPA-backed in this tutorial) before invoking listeners, and only marks it complete once every listener has processed it successfully. If the application crashes mid-way, unprocessed events are retried on restart. This is the tutorial's central point: **at-least-once delivery between modules, with no message broker involved** - the guarantee usually associated with Kafka or RabbitMQ, provided here by the application's own database.
- **`@ApplicationModuleListener`**: an event listener that Modulith automatically runs asynchronously, after the publishing transaction commits, and wires into the Event Publication Registry - the in-process equivalent of a Kafka consumer, without the broker.
- **`@ApplicationModuleTest`**: boots only the module under test (and whichever other modules it legitimately depends on), instead of the whole application context - a module can be tested in isolation, closer to a unit test than a full `@SpringBootTest`.
- **`Documenter`**: generates a PlantUML component diagram and per-module "canvas" documentation directly from the verified module structure - the diagram can never drift from the code, since it's derived from the same model `verify()` checks.

## Tech stack

| Component | Choice |
|---|---|
| Framework | Spring Boot 4.1.x (Spring Framework 7) |
| Language | Java 17 (LTS) |
| Build | Maven - **a single module**, not multi-module; Spring Modulith's boundaries are packages, not Maven artifacts |
| Modularity | Spring Modulith 2.0.7 (`spring-modulith-bom`), `spring-modulith-starter-core`, `spring-modulith-starter-jpa` (Event Publication Registry), `spring-modulith-starter-test`, `spring-modulith-docs` |
| Database | PostgreSQL 16 (via Docker Compose) |
| ORM | Spring Data JPA / Hibernate |
| Migrations | Flyway |
| Security | Spring Security 7 + JWT |
| API documentation | springdoc-openapi (Swagger UI) |
| Monitoring | Spring Boot Actuator |
| Tests | JUnit 5, Mockito, `@ApplicationModuleTest`, Testcontainers |
| CI/CD | GitHub Actions, including a dedicated step that fails the build on a module-boundary violation |
| Containerization | Docker, docker-compose |

## Data model

Both domains, organized as modules rather than as separate services - one shared database, one schema, no service boundary, but real module boundaries enforced at the code level.

```
auth module:
  users (id, first_name, last_name, email, password, enabled, account_locked)
      │ N──N (via role_user)
  roles (id, role_name)
  activation_tokens (id, user_id, token, created_at, expires_at, validated_at)
  blacklisted_tokens (id, user_id, token, jti, blacklisted_at, created_at, expires_at, validated_at)
  password_reset_tokens (id, user_id, token, type, expiry_date)

catalog module:
  categories (id, category_name)
  products (id, category_id, product_name, unit_price)

customer module:
  customers (id, first_name, last_name, telephone, email, address)

order module:
  orders (id, customer_id, product_id, quantity, total)
  event_publication (Modulith's own table - the Event Publication Registry)
```

`order` depends on `catalog` and `customer` through their public APIs only (to validate a product/customer exist when creating an order); it never queries their tables directly, even though everything lives in the same database.

**Why `customers` has no `user_id` column, even in one shared database**: it would be technically trivial to add, since there's no physical separation stopping it. It's left out on purpose - Modulith's entire premise is that a module boundary should be treated with the same discipline whether or not it happens to be physically enforced. A `customer.user_id` column pointing at `auth`'s `users` table would be a real foreign key crossing a module boundary, exactly the kind of shortcut that only stays tempting because the two tables happen to live in the same database today. If `customer` ever needs to know something about a user, it asks `AuthApi`, the same way it always would once the modules are actually separate services - never a join.

Authorization in this tutorial is deliberately simple and role-only (`hasRole('ADMIN')`/`hasRole('USER')` via Spring Security). There is no separate `Permission` entity and no fine-grained resource/action model - the point of this project is module boundaries, not an authorization system, and a `Role`-only check keeps that focus without pretending the access-control layer on top is more developed than it is.

## Branching strategy

| Branch | Role |
|---|---|
| `master` | Stable, production-ready code. No direct commits, only merges from `develop`. |
| `develop` | Integration branch. |
| `feature/core-architecture` | Project skeleton, package-by-module layout, `ApplicationModules.verify()` wired in from the start. |
| `feature/auth-module` | Identity/RBAC module. |
| `feature/catalog-module` | Category/product module. |
| `feature/customer-module` | Customer module. |
| `feature/order-module` | Order module, depends on `catalog`/`customer` public APIs, publishes `OrderPlacedEvent`. |
| `feature/notification-module` | Listens for `OrderPlacedEvent` via the Event Publication Registry, no message broker. |
| `feature/module-documentation` | PlantUML component diagram and module canvases, generated from the verified structure. |

## Project structure

```
spring-modulith-tutorial/
├── src/
│   ├── main/
│   │   ├── java/com/edgareldy/springmodulithtutorial/
│   │   │   ├── SpringModulithTutorialApplication.java
│   │   │   ├── auth/
│   │   │   │   ├── User.java, Role.java                                   (package-private)
│   │   │   │   ├── ActivationToken.java, BlacklistedToken.java, PasswordResetToken.java
│   │   │   │   ├── UserRepository.java, RoleRepository.java               (package-private)
│   │   │   │   ├── UserService.java, impl/UserServiceImpl.java            (package-private)
│   │   │   │   ├── api/                                                    (@NamedInterface - this module's public API)
│   │   │   │   │   └── AuthApi.java                                        (e.g. currentUserId(), userExists(id))
│   │   │   │   └── web/
│   │   │   │       └── AuthController.java
│   │   │   ├── catalog/
│   │   │   │   ├── Category.java, Product.java                            (package-private)
│   │   │   │   ├── CategoryRepository.java, ProductRepository.java        (package-private)
│   │   │   │   ├── CategoryService.java, ProductService.java, impl/...    (package-private)
│   │   │   │   ├── api/
│   │   │   │   │   └── CatalogApi.java                                     (e.g. findProduct(id), productExists(id))
│   │   │   │   └── web/
│   │   │   │       ├── CategoryController.java
│   │   │   │       └── ProductController.java
│   │   │   ├── customer/
│   │   │   │   ├── Customer.java                                          (package-private)
│   │   │   │   ├── CustomerRepository.java                                (package-private)
│   │   │   │   ├── CustomerService.java, impl/CustomerServiceImpl.java    (package-private)
│   │   │   │   ├── api/
│   │   │   │   │   └── CustomerApi.java                                    (e.g. findCustomer(id), customerExists(id))
│   │   │   │   └── web/
│   │   │   │       └── CustomerController.java
│   │   │   ├── order/
│   │   │   │   ├── Order.java                                             (package-private)
│   │   │   │   ├── OrderRepository.java                                   (package-private)
│   │   │   │   ├── OrderService.java, impl/OrderServiceImpl.java          (package-private, depends on CatalogApi/CustomerApi)
│   │   │   │   ├── OrderPlacedEvent.java                                  (public - this is what crosses the module boundary)
│   │   │   │   └── web/
│   │   │   │       └── OrderController.java
│   │   │   ├── notification/
│   │   │   │   └── OrderPlacedEventListener.java                          (@ApplicationModuleListener, public - the whole module is its own API surface)
│   │   │   └── common/
│   │   │       ├── ApiResponse.java, PageResponse.java                    (shared, open module - see Code conventions)
│   │   │       ├── ResourceNotFoundException.java, BusinessRuleException.java
│   │   │       └── GlobalExceptionHandler.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       └── db/migration/
│   │           └── V1__init_schema.sql
│   └── test/
│       └── java/com/edgareldy/springmodulithtutorial/
│           ├── ModularityTests.java                                      (ApplicationModules.verify() + Documenter)
│           ├── auth/          (@ApplicationModuleTest)
│           ├── catalog/       (@ApplicationModuleTest)
│           ├── customer/      (@ApplicationModuleTest)
│           ├── order/         (@ApplicationModuleTest)
│           └── notification/  (@ApplicationModuleTest)
├── docker-compose.yml
├── Dockerfile
├── .github/workflows/ci.yml
├── pom.xml
└── README.md
```

## Standard response format

Every HTTP-facing endpoint, across every module, returns the same generic `ApiResponse<T>`.

```java
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        Instant timestamp
) {
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, message, data, Instant.now());
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, Instant.now());
    }
}
```

`ApiResponse<T>` lives in the `common` package, which Modulith treats as an **open module** (declared via `ApplicationModule.Type.OPEN` or simply excluded from `verify()`'s strict checks) - every other module may depend on it freely, since it carries no business logic or state of its own, the same reasoning `common-lib` follows in this tutorial's multi-service counterparts, just as a package here instead of a separate artifact.

## feature/core-architecture

### Tasks

- [ ] Initialize the project (Maven, Java 17, Spring Boot 4.1.x), **single module**
- [ ] Dependencies: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`, `spring-boot-starter-actuator`, `spring-modulith-starter-core`, `spring-modulith-starter-jpa`, `flyway-core`, `postgresql`, `lombok`, `springdoc-openapi-starter-webmvc-ui` - all versions managed by the `spring-modulith-bom` (2.0.7) and the Spring Boot 4.1.x BOM
- [ ] Test dependencies: `spring-boot-starter-test`, `spring-modulith-starter-test`, `testcontainers`
- [ ] Package skeleton: `auth`, `catalog`, `customer`, `order`, `notification`, `common` as direct sub-packages of the main application package - created empty, but present, from this branch
- [ ] `ModularityTests.java`: a single test, `ApplicationModules.of(SpringModulithTutorialApplication.class).verify()` - **this must pass on an empty skeleton before any module has real code in it**, establishing the discipline from day one rather than bolting it on later
- [ ] `common` package: `ApiResponse<T>`, `PageResponse<T>`, `GlobalExceptionHandler`, base exceptions
- [ ] Flyway script `V1__init_schema.sql` (all tables, plus Modulith's own `event_publication` table via `spring-modulith-starter-jpa`'s schema)
- [ ] Actuator health check exposed at `/actuator/health`, including the database connection (`db` health indicator) so the container orchestrator can tell a genuinely unhealthy instance from one still starting up
- [ ] `docker-compose.yml` (app + PostgreSQL), `.github/workflows/ci.yml` - the CI job runs `ModularityTests` on every push, so a boundary violation fails the build immediately, same as any other test

## feature/auth-module

### Endpoints

| Method | URL | Description |
|---|---|---|
| POST | `/api/v1/auth/register` | Register |
| GET | `/api/v1/auth/activate-account` | Activate an account from the emailed (logged) confirmation link |
| POST | `/api/v1/auth/login` | Returns a JWT |
| POST | `/api/v1/auth/logout` | Blacklists the current token (by `jti`) |
| GET | `/api/v1/auth/me` | Current user profile |
| POST | `/api/v1/auth/forgot-password` | Always returns the same response, whether or not the email exists |
| POST | `/api/v1/auth/reset-password` | Consumes a password-reset token |

### Tasks

- [ ] `User`, `Role` entities (with the `role_user` join) and their repositories/services - all package-private (no `public` modifier), living entirely inside `auth`
- [ ] `ActivationToken`, `BlacklistedToken`, `PasswordResetToken` entities and repositories, package-private
- [ ] `auth.api.AuthApi` (`@NamedInterface`): the only thing other modules are allowed to depend on - a small interface exposing only what's genuinely needed elsewhere (e.g. `boolean userExists(Long userId)`), never the `User` entity itself
- [ ] `UserService`: registration (creates the user disabled, generates an `ActivationToken`, logs the activation link instead of emailing it, consistent with how outbound notifications are simulated elsewhere in this tutorial series), activation, login (issues a JWT), logout (blacklists the token's `jti`), forgot-password and reset-password
- [ ] Enumeration protection: `/auth/forgot-password` returns the identical response regardless of whether the submitted email exists, so the endpoint can't be used to discover registered accounts
- [ ] `AuthController`
- [ ] `JwtService`: issues and validates JWTs, checking the token's `jti` against `BlacklistedToken` on every authenticated request
- [ ] Authorization for this tutorial is deliberately simple, role-only: `hasRole('ADMIN')`/`hasRole('USER')` via Spring Security, no separate `Permission` entity or fine-grained resource/action model - the point of this project is module boundaries, not an authorization system, and a `Role`-only check keeps that focus without pretending the RBAC on top is more developed than it is
- [ ] `@ApplicationModuleTest` for `auth`, booting only this module
- [ ] Run `ModularityTests` again - still passes, since `auth` has no dependency on any other module yet

## feature/catalog-module

### Endpoints

| Method | URL | Description | Access |
|---|---|---|---|
| GET | `/api/v1/categories` | Paginated list | `hasRole('USER')` |
| POST | `/api/v1/categories` | Create | `hasRole('ADMIN')` |
| GET | `/api/v1/products` | Paginated list, filterable by `categoryId` | `hasRole('USER')` |
| POST | `/api/v1/products` | Create | `hasRole('ADMIN')` |

### Tasks

- [ ] `Category`, `Product` entities and their repositories/services, package-private
- [ ] `catalog.api.CatalogApi` (`@NamedInterface`): exposes what `order` will need later (e.g. `Optional<ProductSummary> findProduct(Long id)`), never the `Product` entity
- [ ] `CategoryController`, `ProductController`
- [ ] Business rule: deleting a category that still has products is rejected
- [ ] `@ApplicationModuleTest` for `catalog`
- [ ] `ModularityTests` still passes

## feature/customer-module

### Endpoints

| Method | URL | Description | Access |
|---|---|---|---|
| GET | `/api/v1/customers/{id}` | Detail | `hasRole('USER')` |
| POST | `/api/v1/customers` | Create | `hasRole('ADMIN')` |

### Tasks

- [ ] `Customer` entity, repository, service, package-private
- [ ] `customer.api.CustomerApi` (`@NamedInterface`): exposes `Optional<CustomerSummary> findCustomer(Long id)`
- [ ] `CustomerController`
- [ ] `@ApplicationModuleTest` for `customer`
- [ ] `ModularityTests` still passes

## feature/order-module

The first module that actually depends on others - this is where a boundary violation becomes possible for the first time.

### Endpoints

| Method | URL | Description | Access |
|---|---|---|---|
| GET | `/api/v1/orders/{id}` | Detail | `hasRole('USER')` |
| POST | `/api/v1/orders` | Create (computes `total`) | `hasRole('USER')` |
| GET | `/api/v1/orders` | Paginated list | `hasRole('ADMIN')` |

### Tasks

- [ ] `Order` entity, repository, package-private
- [ ] `OrderServiceImpl`: depends on `CatalogApi`/`CustomerApi` (injected as the named-interface types, never the internal entities or repositories of those modules) to validate a product/customer exist and to read the product's price. `Order.total` is a **snapshot** taken at order-creation time (`quantity * product.unitPrice` as returned by `CatalogApi` at that moment), stored on the `orders` row itself rather than recalculated later - so a subsequent price change on the product never alters the total of an order already placed
- [ ] `OrderPlacedEvent` (public - an event is precisely the kind of thing meant to cross a module boundary): published via `ApplicationEventPublisher` after the order is persisted
- [ ] `OrderController`
- [ ] A deliberate violation, introduced then immediately removed, as a learning exercise: temporarily import `catalog.Product` directly into `OrderServiceImpl` instead of using `CatalogApi`, run `ModularityTests`, observe it fail with a clear violation message, then revert to the correct dependency - this is meant to be done once, by hand, so the failure message is seen at least once before trusting the test going forward
- [ ] `@ApplicationModuleTest` for `order`, which by Modulith's own rules will also boot `catalog` and `customer` (its declared dependencies) but not `notification`
- [ ] `ModularityTests` passes with the real dependency graph: `order → catalog`, `order → customer`

## feature/notification-module

### Tasks

- [ ] `OrderPlacedEventListener` (`@ApplicationModuleListener`): reacts to `OrderPlacedEvent`, logs a message standing in for an email/notification (consistent with how outbound notifications are simulated elsewhere in this tutorial series)
- [ ] Verify the **Event Publication Registry** behavior directly, not just the happy path: with `spring-modulith-starter-jpa` in place, inspect the `event_publication` table after placing an order - a row should exist, marked completed once the listener runs successfully
- [ ] A deliberately failing listener test: throw from `OrderPlacedEventListener`, restart the application context, and verify the event is retried automatically from the registry rather than lost - this is the concrete demonstration of "at-least-once delivery without a message broker"
- [ ] `@ApplicationModuleTest` for `notification`
- [ ] `ModularityTests` passes with `notification`'s dependency on `order` (for `OrderPlacedEvent` only - `notification` never depends on any of `order`'s internal classes)

## feature/module-documentation

### Tasks

- [ ] `ModularityTests` extended: alongside `verify()`, call `new Documenter(modules).writeModulesAsPlantUml().writeIndividualModulesAsPlantUml()`, generating diagrams into `target/spring-modulith-docs/` on every test run
- [ ] A Maven profile or CI step that copies the generated PlantUML output into a committed `docs/` folder on `develop`, so the architecture diagram is always current with the actual verified module graph - never hand-drawn, never manually kept in sync
- [ ] Per-module "canvas" documentation (`Documenter`'s module canvas output): each module's public API, its dependencies, and the events it publishes/listens to, generated the same way
- [ ] A short section added to this README (or a linked `ARCHITECTURE.md`) explaining how to regenerate the docs locally (`mvn test -Dtest=ModularityTests`) after adding a new module

## Order of work

1. `feature/core-architecture` → Pull Request to `develop`
2. `feature/auth-module` (depends on `core-architecture`) → Pull Request to `develop`
3. `feature/catalog-module` (depends on `core-architecture`) → Pull Request to `develop`
4. `feature/customer-module` (depends on `core-architecture`) → Pull Request to `develop`
5. `feature/order-module` (depends on `catalog-module`, `customer-module`) → Pull Request to `develop`
6. `feature/notification-module` (depends on `order-module`) → Pull Request to `develop`
7. `feature/module-documentation` (depends on every module existing) → Pull Request to `develop`
8. `develop` → `master`

## Code conventions

- Root package: `com.edgareldy.springmodulithtutorial`, one direct sub-package per module (`auth`, `catalog`, `customer`, `order`, `notification`), plus `common` as an open, shared package
- Every entity, repository, and service implementation is **package-private** by default; only what's placed under a module's `api/` sub-package (marked `@NamedInterface`) or is itself an event is visible to other modules
- A module never imports another module's entity or repository directly - always through that module's named interface, or through an event
- `ModularityTests` (`ApplicationModules.verify()`) runs in CI on every push - a module-boundary violation is a build failure, not a code-review comment
- Events crossing a module boundary are named in the past tense (`OrderPlacedEvent`), same convention as the message-driven and messaging-based tutorials in this series
- `ApiResponse<T>` is the only thing every module is allowed to share unconditionally, via the open `common` package

## Concepts covered

- Application Modules as the unit of modularity (package-by-module, not package-by-layer)
- Named Interfaces (`@NamedInterface`) as the mechanism for exposing a controlled public API from a module
- Automated module-boundary verification (`ApplicationModules.verify()`) as a build-breaking test, not documentation
- The Event Publication Registry: durable, at-least-once event delivery between modules without a message broker
- `@ApplicationModuleListener` as the in-process equivalent of an asynchronous message consumer
- `@ApplicationModuleTest`: booting a module (and only its declared dependencies) in isolation for testing
- Architecture documentation generated directly from verified code (`Documenter`, PlantUML), never hand-maintained
- The trade-offs between logical (Modulith), physical (multi-module Maven), and process-level (microservices) boundaries
- Containerization (Docker, docker-compose)
- Continuous integration (GitHub Actions), with a dedicated architectural-verification step

## How to follow this tutorial

1. Clone the repository (`https://github.com/EdgarEldy/spring-modulith-tutorial.git`) and check out `develop`
2. Follow the branches in order: `feature/core-architecture` → `feature/auth-module` → `feature/catalog-module` → `feature/customer-module` → `feature/order-module` → `feature/notification-module` → `feature/module-documentation`
3. After each module branch, run `mvn test -Dtest=ModularityTests` and confirm it still passes before moving on
4. Run `docker-compose up`, then place an order and inspect the `event_publication` table to see the Event Publication Registry in action
