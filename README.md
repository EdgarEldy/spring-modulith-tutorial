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

- [x] Initialize the project (Maven, Java 17, Spring Boot 4.1.x), **single module**
- [x] Dependencies: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`, `spring-boot-starter-actuator`, `spring-modulith-starter-core`, `spring-modulith-starter-jpa`, `flyway-core`, `postgresql`, `lombok`, `springdoc-openapi-starter-webmvc-ui` - all versions managed by the `spring-modulith-bom` (2.0.7) and the Spring Boot 4.1.x BOM
- [x] Test dependencies: `spring-boot-starter-test`, `spring-modulith-starter-test`, `testcontainers`
- [x] Package skeleton: `auth`, `catalog`, `customer`, `order`, `notification`, `common` as direct sub-packages of the main application package - created empty, but present, from this branch
- [x] `ModularityTests.java`: a single test, `ApplicationModules.of(SpringModulithTutorialApplication.class).verify()` - **this must pass on an empty skeleton before any module has real code in it**, establishing the discipline from day one rather than bolting it on later
- [x] `common` package: `ApiResponse<T>`, `PageResponse<T>`, `GlobalExceptionHandler`, base exceptions
- [x] Flyway script `V1__init_schema.sql` (all tables, plus Modulith's own `event_publication` table via `spring-modulith-starter-jpa`'s schema)
- [x] Actuator health check exposed at `/actuator/health`, including the database connection (`db` health indicator) so the container orchestrator can tell a genuinely unhealthy instance from one still starting up
- [x] `docker-compose.yml` (app + PostgreSQL), `.github/workflows/ci.yml` - the CI job runs `ModularityTests` on every push, so a boundary violation fails the build immediately, same as any other test

### Notes on what was built

- **Spring Boot 4 modular starters**: Boot 4 moved each auto-configuration into its own module, so Flyway is pulled in through `spring-boot-starter-flyway` (which brings `flyway-core`, plus `flyway-database-postgresql`); `flyway-core` alone would sit on the classpath without ever migrating. For the same reason the MockMvc test support comes from `spring-boot-starter-webmvc-test`, and Testcontainers 2.x is declared as `spring-boot-testcontainers`, `testcontainers-postgresql` and `testcontainers-junit-jupiter`.
- **Bean Validation**: `spring-boot-starter-validation` is added on top of the listed dependencies. `GlobalExceptionHandler` answers a validation failure with a 400 listing the invalid fields, and every module's request DTOs rely on `@Valid`.
- **Versions**: Spring Boot 4.1.1, Spring Modulith 2.0.7 (`spring-modulith-bom`), springdoc-openapi 3.1.1 (the only version held in a `pom.xml` property, since neither BOM manages it).
- **`common` is declared open** with `@ApplicationModule(type = ApplicationModule.Type.OPEN)` in its `package-info.java`; the business modules exist from this branch as packages holding only a `package-info.java`.
- **Error responses** also use `ApiResponse<T>` (`ApiResponse.error(...)`): 404 `ResourceNotFoundException`, 422 `BusinessRuleException`, 400 for validation (invalid fields as `data`) or an unreadable body, 403 for a failed role check, the real status for Spring MVC's own errors, and a generic 500 otherwise.
- **Schema**: `V1__init_schema.sql` creates every table, seeds the `ADMIN` and `USER` roles, and copies Modulith's v2 `event_publication` DDL. No foreign key crosses a module boundary: `orders.customer_id`/`orders.product_id` are plain columns.
- **Health**: `/actuator/health` lists its components (including `db`); the `readiness` probe group includes `db` while `liveness` does not, so losing the database takes an instance out of rotation without getting it restarted.
- **Security baseline**: a minimal `SecurityFilterChain` (stateless, health, Swagger and `/error` public, everything else authenticated, 401 for an anonymous caller) lives in `common` until `feature/auth-module` takes it over.
- **Local database port**: `docker-compose.yml` publishes PostgreSQL on host port `${DB_PORT:-5433}` so it never collides with a PostgreSQL already installed on 5432; `application-dev.yml` points at it (`docker compose up db`, then run with the `dev` profile).
- **CI**: `ci.yml` runs `ModularityTests` as its own first step, then `mvnw verify`; `pr-checks.yml` validates Conventional Commits on pull requests.

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

- [x] `User`, `Role` entities (with the `role_user` join) and their repositories/services - all package-private (no `public` modifier), living entirely inside `auth`
- [x] `ActivationToken`, `BlacklistedToken`, `PasswordResetToken` entities and repositories, package-private
- [x] `auth.api.AuthApi` (`@NamedInterface`): the only thing other modules are allowed to depend on - a small interface exposing only what's genuinely needed elsewhere (e.g. `boolean userExists(Long userId)`), never the `User` entity itself
- [x] `UserService`: registration (creates the user disabled, generates an `ActivationToken`, logs the activation link instead of emailing it, consistent with how outbound notifications are simulated elsewhere in this tutorial series), activation, login (issues a JWT), logout (blacklists the token's `jti`), forgot-password and reset-password
- [x] Public routes: register, activate-account, login, forgot-password and reset-password are reachable without a token; logout and me require an authenticated caller. A registered account gets the `USER` role and stays disabled until activated
- [x] `AdminBootstrap`: at startup, creates an enabled account with the `ADMIN` role from `APP_ADMIN_EMAIL`/`APP_ADMIN_PASSWORD` when both are set and the account does not exist yet - no default password anywhere in the code or the migrations, and nothing happens when the variables are absent
- [x] Enumeration protection: `/auth/forgot-password` returns the identical response regardless of whether the submitted email exists, so the endpoint can't be used to discover registered accounts
- [x] `AuthController`
- [x] `JwtService`: issues and validates JWTs, checking the token's `jti` against `BlacklistedToken` on every authenticated request
- [x] Authorization for this tutorial is deliberately simple, role-only: `hasRole('ADMIN')`/`hasRole('USER')` via Spring Security, no separate `Permission` entity or fine-grained resource/action model - the point of this project is module boundaries, not an authorization system, and a `Role`-only check keeps that focus without pretending the RBAC on top is more developed than it is
- [x] `@ApplicationModuleTest` for `auth`, booting only this module
- [x] Run `ModularityTests` again - still passes, since `auth` has no dependency on any other module yet

### Notes on what was built

- **Visibility**: entities, repositories, `UserService`, `JwtService` and the response records (`UserProfile`, `LoginResponse`) are `public` in Java because `impl/` and `web/` use them from sub-packages; Spring Modulith still keeps them internal to `auth`. `SecurityConfig`, `AdminBootstrap`, the entry point, the access denied handler, `UserServiceImpl`, `AuthApiImpl`, `AuthController` and the request records stay package-private.
- **JWT without an external library**: `spring-boot-starter-security-oauth2-resource-server` (the Boot 4 name of the resource server starter) brings the Nimbus `JwtEncoder`/`JwtDecoder`. `JwtService` signs HS256 tokens (subject = account id, `roles`, `email`, a random `jti`) and implements `JwtDecoder` itself, so the resource server calls it on every bearer token and the blacklist check runs after the signature, expiry and issuer checks.
- **Settings** live under `app.auth` in `application.yml` (`AuthProperties`): `APP_JWT_SECRET` overrides a development-only default key (at least 32 bytes, checked at startup), `APP_BASE_URL` sets the base of the logged activation link, activation links last 24h and reset tokens 15 minutes.
- **Security chain** moved from `common` to `auth`: stateless, CSRF off, health, Swagger, `/error` and the five public auth routes open, everything else authenticated. A 401 (no, invalid, expired or revoked token) and a 403 raised by the filters both carry an `ApiResponse` error body. The `roles` claim becomes `ROLE_` authorities; there is no role hierarchy, so `ADMIN` and `USER` are checked independently. `@EnableMethodSecurity` lives in `common/MethodSecurityConfig`, shared with the other modules.
- **Errors**: bad credentials, an inactive or a locked account answer 401 through a `ResponseStatusException` rendered by the existing `GlobalExceptionHandler`; the same message is used for an unknown email and a wrong password. A taken email and an invalid, expired or reused activation or reset token answer 422.
- **Tokens**: activation tokens are single-use through `validated_at`; a reset token is deleted once used, and a new forgot-password request deletes the pending ones. Tokens are 32 random bytes, URL-safe Base64. The activation link and the reset token are logged at `INFO` in place of an email.
- **Administrator**: `AdminBootstrap` (an `ApplicationRunner`) creates the `ADMIN` account from `APP_ADMIN_EMAIL`/`APP_ADMIN_PASSWORD` only when both are set; nothing is seeded otherwise.

## feature/catalog-module

### Endpoints

| Method | URL | Description | Access |
|---|---|---|---|
| GET | `/api/v1/categories` | Paginated list | `hasRole('USER')` |
| POST | `/api/v1/categories` | Create | `hasRole('ADMIN')` |
| GET | `/api/v1/products` | Paginated list, filterable by `categoryId` | `hasRole('USER')` |
| POST | `/api/v1/products` | Create | `hasRole('ADMIN')` |

### Tasks

- [x] `Category`, `Product` entities and their repositories/services, package-private
- [x] `catalog.api.CatalogApi` (`@NamedInterface`): exposes what `order` will need later (e.g. `Optional<ProductSummary> findProduct(Long id)`), never the `Product` entity
- [x] `CategoryController`, `ProductController`
- [x] Business rule: deleting a category that still has products is rejected
- [x] `@ApplicationModuleTest` for `catalog`
- [x] `ModularityTests` still passes

### Notes on what was built

- **Visibility**: `Category`, `Product`, their repositories and the `CategoryService`/`ProductService` interfaces are `public` in Java, because the implementations in `catalog.impl` and the controllers in `catalog.web` use them (see "Java visibility vs. module visibility" in Code conventions). The implementations (`CategoryServiceImpl`, `ProductServiceImpl`, `CatalogApiImpl`) and the controllers are package-private. Only `catalog.api` is a named interface (`@NamedInterface("api")`), so `verify()` rejects any other module touching the rest.
- **`CatalogApi`**: `Optional<ProductSummary> findProduct(Long id)` and `boolean productExists(Long id)`. `ProductSummary` is a record (`id`, `name`, `unitPrice`, `categoryId`); the `Product` entity never leaves the module.
- **Category deletion**: the rule lives in `CategoryService.delete(id)` (404 for an unknown category, `BusinessRuleException`, so 422, while products still belong to it) and is covered by unit and module tests. By decision, **no `DELETE` endpoint is exposed**: the API only has the four endpoints of the table.
- **Roles**: the endpoints use exactly `hasRole('USER')` for the lists and `hasRole('ADMIN')` for creation. There is no role hierarchy, so an `ADMIN` caller without the `USER` role gets a 403 on the lists. Method security is enabled once in `common/MethodSecurityConfig` (`@EnableMethodSecurity`).
- **Validation**: request bodies use Bean Validation (name mandatory and sized like its column, `unitPrice` strictly positive and within `NUMERIC(12, 2)`, `categoryId` mandatory). `page` (>= 0), `size` (1 to 100) and `categoryId` query parameters are checked by Spring MVC's built-in method validation, which answers a 400. `POST` answers 201 with the created resource. Lists are sorted by id.
- **Duplicate names** are checked by the service (422); the `UNIQUE` constraint of V1 remains the last line of defence.
- **Tests**: Mockito unit tests of the services and of `CatalogApiImpl`, repository tests on PostgreSQL (Testcontainers), HTTP tests with `MockMvcTester` and `@WithMockUser` (200/201, 400, 404, 422, 401 anonymous, 403 wrong role), and `CatalogModuleTest` (`@ApplicationModuleTest`, STANDALONE mode, own container). `spring-boot-starter-security-test` was added for `@WithMockUser`.

## feature/customer-module

### Endpoints

| Method | URL | Description | Access |
|---|---|---|---|
| GET | `/api/v1/customers/{id}` | Detail | `hasRole('USER')` |
| POST | `/api/v1/customers` | Create | `hasRole('ADMIN')` |

### Tasks

- [x] `Customer` entity, repository, service, package-private
- [x] `customer.api.CustomerApi` (`@NamedInterface`): exposes `Optional<CustomerSummary> findCustomer(Long id)`
- [x] `CustomerController`
- [x] `@ApplicationModuleTest` for `customer`
- [x] `ModularityTests` still passes

### Notes on what was built

- **Layout**: `Customer`, `CustomerRepository`, `CustomerService` and the `CreateCustomerRequest`/`CustomerResponse` records live in the module's base package; `impl/CustomerServiceImpl` and `impl/CustomerApiImpl` are package-private; `web/CustomerController` is package-private too. The entity and repository are `public` in Java only because `impl/` needs them.
- **Base package vs. named interface**: Spring Modulith puts every public type of a module's base package into the module's unnamed interface, even when an `api` named interface exists, so `verify()` alone does not stop another module from importing `customer.Customer`. The consumer closes that door by declaring `@ApplicationModule(allowedDependencies = "customer :: api")` in its `package-info.java`: `verify()` then rejects any type outside `customer.api` (checked by hand on this branch with a throwaway class in `order`).
- **Public API**: `customer.api` is `@NamedInterface("api")` and holds `CustomerApi` (`findCustomer(id)` returning an `Optional<CustomerSummary>`, and `customerExists(id)`) and the `CustomerSummary` record (id, names, email). The entity never leaves the module.
- **Rules**: emails are stored in lower case, so the unique constraint also catches a duplicate typed in another case; a duplicate is a 422 (`BusinessRuleException`), including when two concurrent requests race past the upfront check (the insert is flushed and the constraint violation translated). An unknown id is a 404.
- **HTTP**: `POST /api/v1/customers` answers **201 Created** with a `Location` header and the customer in `ApiResponse.data`; `GET /api/v1/customers/{id}` answers 200. Bean Validation checks the names, a telephone pattern, a valid email and the address, with the column lengths of V1 as size limits.
- **Roles**: `@PreAuthorize("hasRole('USER')")` on the read and `hasRole('ADMIN')` on the creation, exactly as in the table. There is no role hierarchy: a caller holding only `ADMIN` gets a 403 on the read endpoint. Method security is enabled once in `common/MethodSecurityConfig`, and `spring-boot-starter-security-test` provides `@WithMockUser` for the tests.
- **Schema**: the entity maps the existing `customers` table of V1 (no migration added, still no `user_id` column).
- **Tests**: Mockito tests of the service and of `CustomerApiImpl`; a repository test on PostgreSQL (Testcontainers) reusing the shared integration context instead of a JPA slice; HTTP tests with `MockMvcTester` and `@WithMockUser` (201, 200, 400, 404, 422, 401 anonymous, 403 wrong role, bodies of 401/403 not asserted since `feature/auth-module` replaces the filter chain); and an `@ApplicationModuleTest` booting the customer module alone.

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
- **Java visibility vs. module visibility**: Java has no notion of a sub-package, so a type used from `impl/` or `web/` of its own module (a service interface, a DTO) has to be `public` in Java. Spring Modulith hides every sub-package of a module that is not a named interface, but it treats the `public` types of a module's **base package** as part of that module's unnamed interface: a public entity such as `catalog.Product` would not be reported by `verify()` on its own. That is why every module that depends on others declares exactly what it may use in its `package-info.java`, e.g. `@ApplicationModule(allowedDependencies = {"catalog :: api", "customer :: api", "common"})` on `order`: `ApplicationModules.verify()` then fails as soon as that module touches anything else. Any type no other package of its module needs keeps the package-private default
- Test methods are named `_NN_Should<Outcome>_When<Condition>` (`NN` restarting at `_01_` in each class, in source order), except the `contextLoads()` smoke test generated with the project
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
