# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- **Tests**: integration coverage for dashboard keyword search beyond the first unfiltered page.

### Fixed

- **Dashboard API**: `GET /api/config/client/dashboard/applications` accepts optional `keywords` and returns keyword-filtered pages with full `DashboardApplicationDto` enrichment (`territoryCount`, `singleTerritoryId`, `hasTerritories`).
- **Dashboard API**: `/dashboard/suggestions` keyword search queries authorized applications/territories in the database instead of filtering only the first page in memory.

## [1.2.7] - 2026-06-05

### Added

- **Dashboard API**: added paginated `GET /api/config/client/dashboard/applications` and suggestions support, including `DashboardApplicationDto`, `DashboardSuggestionDto`, `DashboardMapper`, and controller tests.
- **Validation**: aligned `UserDTO` length constraints (`firstName`, `lastName`, `email`) with DB limits.
- **Startup invariants**: added `UserBuiltInStartupValidator` checks for built-in `admin`/`public` users and aligned dev seed data so built-ins do not carry `UserPosition` rows.
- Application contact (profile DTO): `ApplicationMapper` now publishes institutional email in `ApplicationDto.creator` (instead of username) for profile payloads.
- **Territory view**: `Territory.getComputedView()` now drives profile `ApplicationDto.initialExtent`; includes coverage for null/legacy/offset-center edge cases.
- **Client profile tasks**: added locator task type mapping (`TaskLocatorService`) with proxy URL wiring and linked query-task execution scope handling.
- **Client profile metadata**: profile `ApplicationDto` now includes territory metadata fields (`territoryCode`, `territoryName`, authority fields, and territory type name).
- **Tests**: expanded focused coverage for account/security guards, built-in-user invariants, creator-email mapping, locator-task mapping, and profile integration fixtures.

### Security

- **Account API**: `GET /api/account/{id}` is now self-or-admin only and `GET /api/account/all` is admin-only.
- **JWT filter**: `JsonWebTokenFilter` now rejects blocked accounts with HTTP 401 and clears `access_token` using centralized cookie-expiry behavior.
- **Auth endpoints**: `POST /api/authenticate/proxy` now supports `ROLE_USER`; `POST /api/authenticate/logout` is `permitAll` for stale/anonymous cleanup.
- **Proxy RBAC**: `ProxyConfigurationService.validateUserAccess` now blocks built-in `public` and blocked principals on protected/private app contexts (unless explicit bypass flag is enabled).
- **Client config RBAC**: `/api/config/client/**` now applies blocked/public access rules consistently (401 for anonymous public principal, 403 for authenticated blocked users).

### Changed

- **MoreInfoTaskResolver**: `resolveOrSelf` delegates locator tasks to linked `query-task` relations (same as more-info tasks).
- **User positions**: multiple `UserPosition` rows per `(user, territory)` are allowed; JPA unique constraint removed and schema changelogs no longer create `(POS_USERID, POS_TERID)` unique keys.
- **User warnings**: tightened warning conditions (`position-without-details` requires only missing `name`/`organization`; `no-password` applies to non-built-in users).
- **User password**: `UserEventHandler` now rejects empty-string password assignment on create/update (`null` update still preserves existing hash).

### Fixed

- **Point of contact (little DTO)**: `ApplicationDtoLittle` now exposes `pointOfContact` (renamed from `creator`) as institutional email only, fixing viewer-side contact resolution ([sitmun-viewer-app#159](https://github.com/sitmun/sitmun-viewer-app/issues/159)).
- **Auth compatibility**: fixed role restrictions that blocked viewer proxy refresh/logout flows by enabling `ROLE_USER` proxy-token refresh and `permitAll` logout cleanup ([#256](https://github.com/sitmun/sitmun-viewer-app/issues/256)).
- **Database Schema**: Oracle bootstrap no longer uses `DEFAULT FALSE` for `STM_TREE_NOD.TNO_LOAD_DATA` and `STM_TREE_NOD.TNO_FILTERABLE`; both defaults are now `DEFAULT 0` for Oracle 24 compatibility ([#43](https://github.com/sitmun/sitmun-application-stack/issues/43)). Existing Oracle databases that already executed changeset `sitmun:1` may require checksum reconciliation before applying upgrades.

## [1.2.6] - 2026-05-08

### Added

- **Task Query Services**: `TaskQueryUrlService` mapper for `external-link` scope query tasks; builds `TaskDto` with `scope=URL`, direct URL from command, and viewer-compatible parameter DTOs. Expands task type coverage to all five canonical query scopes (SQL, API, RESOURCE, URL, cartography-query).
- **Task Domain Helpers**: `DomainConstants.Tasks.isUrlQueryTask(Task)` predicate for identifying URL query tasks (type=Query, scope=external-link, no connection/cartography FK).
- **Task Validation**: `ParameterValidator.containsSystemVariablesInParameters(List<Map>)` and `.containsSystemVariablesInMapValues(Map)` helpers for detecting `#{...}` patterns in task parameter lists and generic maps (headers, queryParams).
- **Task Scope Normalization**: `TaskScopeNormalizer.normalizeExecutionScope(Map)` utility class centralizes admin-to-viewer scope mapping logic (sql-query→SQL, web-api-query→API, web-api-query-no-proxy→RESOURCE/URL, external-link→URL, preserving cartography-query as-is). Extracted from `TaskMoreInfoService` for reuse across all query mappers.
- **Test / seed data**: Profile integration coverage for `web-api-query` (proxied, `scope=API`, middleware URL) vs `web-api-query-no-proxy` (`scope=URL`, direct `command` URL), with both `template` and `query` parameter types (`STM_TASK`, `STM_ROL_TSK`, `STM_AVAIL_TSK`; tasks 37–39); `sql-query` tasks with `scope=SQL`, JDBC proxy URL, and **`query`** (task 34) vs **`template`** (task 40).
- **Accept Rule Tests**: `DomainConstantsTaskAcceptTest` unit tests verify tightened `isSqlQueryTask`, `isCartographyQueryTask`, `isWebApiQuery`, `isUrlQueryTask` predicates enforce scope + FK consistency (scope-aware when set, legacy FK-based fallback otherwise).
- **Parameter Shape Contract Tests**: `TaskParameterShapeContractTest` documents and validates the intentional distinction between query-task parameter DTOs (minimal `{type, required}` for execution contract) and more-info parameter DTOs (field-forwarding `{name, label, value, type?, required?}` for feature data extraction). Prevents unintended convergence without explicit audit approval.
- **Client configuration profile** (`GET /api/config/client/profile/{appId}/{terrId}`): each layer may include `minScaleDenominator` and `maxScaleDenominator` mapped from cartography minimum/maximum scale (positive values only; omitted when unset or non-positive); each layer may include `transparency` (0–100, where 0 is fully opaque) mapped from `Cartography.transparency`; omitted when unset; each layer may include `order` mapped from `Cartography.order` (`GEO_ORDER`); omitted when null. Viewer reads it as the SITNA `zIndex` when adding the layer to the working layers (default 0 when absent); each layer may include `metadataURL` and `datasetURL` mapped from cartography; omitted when unset (OGC WMS Layer `MetadataURL` / `DataURL` hrefs); each layer may include `description` (i18n-translated abstract) mapped from `Cartography.description` (`GEO_ABSTRACT`); omitted when unset. Viewer merges this onto matched real WMS GetCapabilities layers as the OGC `Abstract`; each tree node may include `metadataURL` and `datasetURL` mapped from `TreeNode`; omitted when unset (folder-level URLs alongside cartography on leaves).
- **Authentication**: `POST /api/authenticate/proxy` returns a short-lived `proxy_token` in JSON for proxy middleware; validity from `sitmun.proxy-middleware.token-validity-in-milliseconds`. `POST /api/authenticate/logout` clears the `access_token` session cookie.
- `RequestCoordinates` (user, territory, application) for proxy configuration context.
- **Parameter expansion**: `HttpUserParametrizationDecorator` and `SqlUserParametrizationDecorator` for client-driven HTTP URI template expansion and JDBC `${var}` / WHERE augmentation.
- **Better SQL dialect**: `JdbcSqlDialect` for dialect-aware SQL `LIMIT`/`OFFSET` in `QueryPaginationDecorator`.
- **Better HTTP payloads**: `HttpPayloadDto` base for HTTP-like proxy payloads; `HttpSecurityDto.describeForLog()` for safe debug summaries (no passwords or header values).
- **Data masking**: `SensitiveDataMasking` for log redaction of secrets.
- **Tests**: proxy URI template integration, decorator and pagination regression coverage; unit tests for `JsonWebTokenFilter`, `ProxyTokenFilter`, and `CookieService`.
- **Connections**: Microsoft SQL Server JDBC driver on the classpath and `databaseConnection.driver` startup codelist (`com.microsoft.sqlserver.jdbc.SQLServerDriver`); proxy middleware ships the same driver for JDBC tasks ([#251](https://github.com/sitmun/sitmun-backend-core/issues/251)).

### Changed

- **Task Parameter Typing**: Added comprehensive JSON snapshot tests for task parameter DTOs in client configuration profiles. Introduced strongly-typed records for task parameter DTOs in client configuration profiles. Introduced `ClientRequestParameters`, `Pagination`, and `EffectiveParameters` value objects to replace raw `Map<String, String>` in proxy parameter pipeline. Introduced `BasicParameterValueType` enum and `BasicParameterValueConverter` component to consolidate type-based conversion logic. Migrated `ParameterValidator` methods (`hasProvidedVariables`, `containsSystemVariablesInParameters`, `validateNoProvidedVariables`) to operate on typed `List<TaskParameter>` instead of raw `List<Map<String, Object>>`.
- **Task Query Web Service Proxying**: `TaskQueryWebService.map(...)` proxy decision now based solely on scope (`web-api-query` → always proxied; `web-api-query-no-proxy` → always direct), fixing bug where `#{...}` in command URL was ignored for proxy routing if no `provided=true` parameter existed.
- **Client profile (`web-api-query`, proxied)**: Configuration profile omit parameters with `type=template`; the viewer calls the middleware proxy URL (`/proxy/{app}/{terr}/API/{taskId}`) only—URI path placeholders are resolved server-side, so exposing them to the client was unnecessary. Slots with `type=query` and other non-template types remain exposed. Tasks with `web-api-query-no-proxy` still expose `template` placeholders because the client applies the upstream URL locally.
- **Task Query Validator Enforcement**: `TaskQueryValidator.validate(...)` extended with strict `validateDirectExecutionScope(...)` method for `web-api-query-no-proxy` and `external-link` tasks. Rejects direct-execution tasks that contain: provided parameters, `#{...}` in command/parameters/headers/queryParams, non-empty headers/queryParams maps, or `authenticationMode` other than `null`/"None". Prevents misconfiguration that would fail at runtime.
- **Task Accept Rules Tightened**: `DomainConstants.Tasks.isSqlQueryTask(Task)` and `.isCartographyQueryTask(Task)` now validate `properties.scope` matches expected value (`sql-query`, `cartography-query`) when scope is explicitly set, in addition to FK presence. Legacy behavior (FK-only check) preserved when scope is absent. Prevents scope/FK mismatch errors.
- **Task Scope Population**: All query mappers (`TaskQuerySqlService`, `TaskQueryWebService`, `TaskQueryCartographyService`, `TaskQueryUrlService`) now explicitly populate `TaskDto.scope` with canonical viewer values (SQL, API, RESOURCE, URL) per task type. `TaskMoreInfoService` delegates to `TaskScopeNormalizer` for consistent scope normalization across execution scopes.
- **Task Parameter Processor Documentation**: `TaskParameterProcessor.toFeatureInfoParameter(...)` javadoc clarified to specify the method is for more-info/feature-info field-forwarding DTOs (`{name, label, value, type?, required?}` where `value` is feature field name), NOT for default query-task parameter configuration (`{type, required}`). Includes explicit warning against migrating direct query mappers to this method without audit approval.
- **Task Query Cartography Service**: `TaskQueryCartographyService.map(...)` now sets `TaskDto.cartographyId` to `String.valueOf(cartography.getId())` for viewer consumption. Aligns cartography query mapper with more-info task behaviour and viewer API expectations; `cartographyId` is direct field (not parameter).
- `ClientConfigurationProfileControllerTest`: removed `MockMvcResultHandlers.print()` from tests that only assert JSON (less noisy console output during `./gradlew test`).
- **Authentication**: `POST /api/authenticate` (database/LDAP) issues the session JWT in an HttpOnly `access_token` cookie; cookie attributes use `sitmun.authentication.http-only-cookie` and `sitmun.authentication.same-site-cookie` (see `CookieService`).
- **OGC/WMS proxy build**: `SystemVariableResolver` resolves `#{...}` in service URL and fixed (non-VARY) service parameter values using `RequestCoordinates`.
- **HTTP API tasks**: optional Basic auth or API-key-style security via `HttpSecurityDto` `type` and `headers` map.
- **`applyDecorators`**: strips `LIMIT`/`OFFSET` case-insensitively from incoming parameters before pagination; applies `SqlUserParametrizationDecorator` then `HttpUserParametrizationDecorator`.
- **`SystemVariableResolver`** resolves templates with `RequestCoordinates` (nullable coordinates when no entity bindings).
- **Refactoring**: Task parameter processing centralized into `TaskParameterProcessor` service with unified parsing, classification (`TaskParameterType` enum), filtering, and effective-value computation. Removes ~9 helper methods from `ProxyConfigurationService`. Parameter precedence rules (locked > provided > client > literal > empty) and security checks now handled uniformly across proxy and client-facing endpoints. All TaskMapper implementations (`TaskQuerySqlService`, `TaskQueryWebService`, `TaskQueryCartographyService`, `TaskEditCartographyService`, `TaskMoreInfoService`, `TaskBasicService`) now use `TaskParameterProcessor` for consistent parameter parsing and classification, eliminating duplicated filtering logic while maintaining backward-compatible DTO formats. Parameter-to-DTO conversion centralized into `TaskParameterProcessor` with three per-parameter converter methods (`toQueryParameter`, `toServiceParameter`, `toFeatureInfoParameter`). Eliminates ~30-40 lines of duplicated DTO-building code across five TaskMapper services while preserving service-specific control over loop/filter/null-return behavior and default type values. Renamed `QueryVaryFiltersDecorator` to `SqlUserParametrizationDecorator`.

### Removed

- **Tests**: `TaskRelationAuditTest` removed (never failed; scope/FK hygiene is enforced via seeds and `DomainConstants` predicates). `queryTask.scope=SQL` codelist entry removed from `STM_CODELIST.csv` (config and test resources). Replaced by kebab-case `sql-query`. Existing deployments with `scope=SQL` inside `TAS_PARAMS` JSON require a manual or separate migration outside this seeded changelog if still present.
- `SqlTemplateExpander` and `QueryFixedFiltersDecorator` (logic replaced by the new JDBC/HTTP decorators).
- **Task Parameter Util**: `TaskParameterUtil` class and its test file removed. The `getParameterVariable()` method's variable-name-label fallback logic was inlined directly into `TaskParameterProcessor.parse()`, eliminating the indirection. No other code depended on this utility.

### Fixed

- **Security**: `JsonWebTokenFilter` continues the filter chain when the JWT username has no matching persisted user (previously returned without delegating).
- Improved app configuration loading times by replacing `@EntityGraph` with `@BatchSize` to avoid Cartesian product when fetching members and roles in `CartographyPermission` ([#250](https://github.com/sitmun/sitmun-backend-core/pull/250)).
- **Basic parameter conversion**: `BasicParameterValueConverter.convert(...)` now treats a `null` raw value uniformly — `STRING` yields `""` and every other type yields `null`. Previously, `NUMBER`, `ARRAY`, and `OBJECT` raised an unchecked `NullPointerException` (escaping the documented "null on failure" contract) and `BOOLEAN` silently returned `false`. New `BasicParameterValueConverterTest` covers the null-input, valid-input, and parse-error paths for both `convert(...)` and `validate(...)`.

## [1.2.5] - 2026-03-11

### Changed

- Release alignment to `1.2.5` across build metadata, API docs versions, and documentation badges.

## [1.2.4] - 2026-03-04

### Added

- `SitmunConstants`: application-wide keys for default language (`language.default`) and proxy config (`proxy`).
- `messageCode` on validation Problem Detail `FieldError` (set in exception handlers) for client-side i18n of validation messages.
- **Proxy setup**: proxy middleware URL is configured via `sitmun.proxy-middleware.url` (optional `sitmun.proxy-middleware.force`) and exposed to the client in profile `global.proxy`; per-service proxy URLs still use the same property for the base. Replaces previous DB-based `STM_CONF` proxy entry.

### Changed

- Unified tree node type constants (`CodeListsConstants`) and application configuration for node type handling.
- OIDC: added provider/client constants (`AuthProviderIds`, `OidcClientTypes`), improved tests and documentation.
- Locale resolution: use `SitmunConstants.LANGUAGE_DEFAULT_CONF_KEY` for DB default language; `sitmun.language` default set to `en`.

### Removed

- Proxy URL from seed config `STM_CONF` (proxy URL now from Spring property only).

## [1.2.3] - 2026-02-26

### Added

- More Information task type support with backend services, validators, and template expanders.
- System variable resolution for More Information execution.

### Changed

- Consolidated legacy tree-node type codelists (`treenode.folder.type`, `treenode.leaf.type`) into `treenode.node.type` and updated translations.
- Updated Liquibase scripts and backend test coverage for More Information task data/model changes.
- Parameterized SQL vary-filter handling for prepared statement scenarios.

### Fixed

- Sequence update ordering in More Information Liquibase changesets.
- JSON and properties issues in More Information Liquibase configuration.
- More Information task service regressions during integration.

### Removed

- Legacy QueryVaryFiltersDecorator test assumption that no longer matched WMS payload behavior.

## [1.2.2] - 2026-02-16

### Added

- Request-scoped translation cache and database-driven locale resolution for i18n lookups.
- Health endpoint reports healthy only after startup completes.

### Changed

- Refactored Liquibase configuration and removed legacy Heroku-related setup.
- Lowered translation application logs from info to debug level to reduce noise in normal operation.
- Updated README structure and formatting for consistency.

### Fixed

- Corrected tree node codelist naming (`code-list-name`) handling.
- Stabilized test execution for parallel runs and database-specific scenarios (PostgreSQL/Oracle/WebMvcTest).

## [1.2.1] - 2026-02-06

### Added

- Multi-provider OIDC authentication support alongside existing database/LDAP authentication options ([ec87515](https://github.com/sitmun/sitmun-backend-core/commit/ec87515b))
- Multi-client frontend redirect URLs based on query parameter appended to OIDC auth requests ([e7dff74](https://github.com/sitmun/sitmun-backend-core/commit/e7dff74e))
- Integration tests for redirect service and complete OIDC authentication flow ([6185f61](https://github.com/sitmun/sitmun-backend-core/commit/6185f610), [eed01bb](https://github.com/sitmun/sitmun-backend-core/commit/eed01bbe))
- Unit tests for OIDC authentication ([f63c848](https://github.com/sitmun/sitmun-backend-core/commit/f63c8481))

### Changed

- Centralized redirect logic and removed redundant attributes ([e7dff74](https://github.com/sitmun/sitmun-backend-core/commit/e7dff74e))

### Fixed

- Consistency mismatch between success and failure handlers ([00adec6](https://github.com/sitmun/sitmun-backend-core/commit/00adec63), [f3bfdeb](https://github.com/sitmun/sitmun-backend-core/commit/f3bfdedb))

## [1.2.0] - 2026-01-27

### Added

- Parametrizable build output: Support for JAR or WAR packaging via `-Ppackaging` property ([6736c21](https://github.com/sitmun/sitmun-backend-core/commit/6736c21))
- ServletInitializer for WAR deployment to external servlet containers (Tomcat, WildFly, WebSphere)
- Entity graphs for cartography and task repositories ([a952e8e](https://github.com/sitmun/sitmun-backend-core/commit/a952e8e))
- Tree type validation endpoint ([66b3ac0](https://github.com/sitmun/sitmun-backend-core/commit/66b3ac0))
- CRS support in service profile mapping ([0abf7ca](https://github.com/sitmun/sitmun-backend-core/commit/0abf7ca))
- Application name field in DTOs ([815f54e](https://github.com/sitmun/sitmun-backend-core/commit/815f54e))
- Language controller endpoints ([24f5351](https://github.com/sitmun/sitmun-backend-core/commit/24f5351ef3537687c791eded2ea2ba0ed4823008))
- MBTiles service URL configuration ([5d9b27d](https://github.com/sitmun/sitmun-backend-core/commit/5d9b27d427d7b2d9de557e29b4b608d780fc19dc))
- Tree node viewmode option for codelists ([1a84404](https://github.com/sitmun/sitmun-backend-core/commit/1a8440424f92711923ec92c46f985578477846ed))
- User management updates with position tracking and OTP reset ([2d14e43](https://github.com/sitmun/sitmun-backend-core/commit/2d14e438f172f50f5be97c717d352ba3d1f40dd8))
- Tests for MBTiles and security changes ([d5fbb6e](https://github.com/sitmun/sitmun-backend-core/commit/d5fbb6e))

### Changed

- RFC 9457 problem details for error responses ([87578be](https://github.com/sitmun/sitmun-backend-core/commit/87578be))
- Password verification flow hardening ([fccf964](https://github.com/sitmun/sitmun-backend-core/commit/fccf964a85d56ca970ecf31a049d1115a8959fd6))
- Language endpoint restructuring ([965a86f](https://github.com/sitmun/sitmun-backend-core/commit/965a86f46b43b864fd5b40bd7d181e81b80172e9))
- Spring Boot test annotation modernization ([046a7f4](https://github.com/sitmun/sitmun-backend-core/commit/046a7f4b8348df468a07b227dedbd5eac8cd7812))
- Code formatting and cleanup pass ([22b241b](https://github.com/sitmun/sitmun-backend-core/commit/22b241b))

### Fixed

- Oracle schema updates for STM_USER and STM_TOKEN_USER ([f01d130](https://github.com/sitmun/sitmun-backend-core/commit/f01d130))
- LazyInitializationException handling during constraint violations ([6264fa3](https://github.com/sitmun/sitmun-backend-core/commit/6264fa3))
- STM_TSK_UI.TUI_NAME column size to 50 chars ([89346eb](https://github.com/sitmun/sitmun-backend-core/commit/89346eb))
- Avoid redundant client configuration i18n updates ([f5d73d5](https://github.com/sitmun/sitmun-backend-core/commit/f5d73d5f7c89d7aaad42257d7599d9cd99f6cc4c))
- Profile update validation hardening ([e393f61](https://github.com/sitmun/sitmun-backend-core/commit/e393f61))
- Password reset token handling ([5ef7488](https://github.com/sitmun/sitmun-backend-core/commit/5ef7488))
- Profile update security handling ([6307d88](https://github.com/sitmun/sitmun-backend-core/commit/6307d88))
- Tree node viewmode description ([574f8b9](https://github.com/sitmun/sitmun-backend-core/commit/574f8b9))
- Remove duplicate imports during formatting ([51f887a](https://github.com/sitmun/sitmun-backend-core/commit/51f887a))

### Removed

- Stale test removal ([f37adeb](https://github.com/sitmun/sitmun-backend-core/commit/f37adeb))

### Notes

- Docker and Heroku deployments only support JAR format (default)
- WAR builds intended for deployment to external application servers (Tomcat, WildFly, WebSphere)

## [1.1.1] - 2025-08-28

### Added

- Default header parameters configuration for SITMUN applications
- Comprehensive test coverage for authorization components
- Test coverage for application default values functionality

### Changed

- Reorganized authorization package structure into client and proxy subpackages
- Enhanced SQL generation robustness in QueryVaryFiltersDecorator

### Fixed

- QueryVaryFiltersDecorator: prevent mutation of input target map
- HashMapConverter: add null safety to prevent NPE
- Filter out null tree nodes in profile's tree list
- Update application version to use project.version variable

## [1.1.0] - 2025-08-03

### Added

- User validation service with warning system for data integrity
- Application privacy controls for restricted and public applications
- Extended multilingual support for client configuration endpoints
- Territory prefered SRS has priority for configuring the map viewer
- Support for touristic applications and touristic trees
- Informative messages in tests

### Changed

- Migrated to Spring Boot 3.5.4 & Java 17
- Migrated dependencies to Version Catalog
- Updated to Oracle JDBC 11

### Fixed

- Modernized SITMUN backend core configuration and deployment structure
- Improved database connection validation error handling
- Oracle CLOB and PostgreSQL TEXT handling
- Minor fixes
- Fix test data

### Removed

- Java 7 and Java 8 legacy code
- Code deprecated in version 1.0.0
- Legacy modules: heroku-dev-full, preprod, heroku-dev-lite, cli
- Legacy database tables: STM_PAR_TSK, STM_DOWNLOAD, STM_THEMATIC, STM_THE_RANK, STM_QUERY

## [1.0.0] - 2024-11-12

### Added

- Initial stable release of SITMUN Backend Core
- Spring Boot application with JPA/Hibernate
- REST API with Spring Data REST
- Spring Security implementation
- Liquibase database migration
- Multi-database support (H2, PostgreSQL, Oracle)
- User management and authentication
- Territory and application management
- Cartography and service management
- Task management system
- Tree and node management
- Background and parameter management
- Role-based access control
- LDAP integration
- Mail functionality
- OpenAPI/Swagger documentation
- Health monitoring endpoints
- Docker support
- Comprehensive test suite

### Changed

- Modernized from legacy Spring Boot versions
- Implemented proper dependency management
- Enhanced code quality and maintainability

### Fixed

- Various bug fixes and improvements from development phase

[Unreleased]: https://github.com/sitmun/sitmun-backend-core/compare/sitmun-backend-core/1.2.7...HEAD
[1.2.7]: https://github.com/sitmun/sitmun-backend-core/compare/sitmun-backend-core/1.2.6...sitmun-backend-core/1.2.7
[1.2.6]: https://github.com/sitmun/sitmun-backend-core/compare/sitmun-backend-core/1.2.5...sitmun-backend-core/1.2.6
[1.2.5]: https://github.com/sitmun/sitmun-backend-core/compare/sitmun-backend-core/1.2.4...sitmun-backend-core/1.2.5
[1.2.4]: https://github.com/sitmun/sitmun-backend-core/compare/sitmun-backend-core/1.2.3...sitmun-backend-core/1.2.4
[1.2.3]: https://github.com/sitmun/sitmun-backend-core/compare/sitmun-backend-core/1.2.2...sitmun-backend-core/1.2.3
[1.2.2]: https://github.com/sitmun/sitmun-backend-core/compare/sitmun-backend-core/1.2.1...sitmun-backend-core/1.2.2
[1.2.1]: https://github.com/sitmun/sitmun-backend-core/compare/sitmun-backend-core/1.2.0...sitmun-backend-core/1.2.1
[1.2.0]: https://github.com/sitmun/sitmun-backend-core/compare/sitmun-backend-core/1.1.1...sitmun-backend-core/1.2.0
[1.1.1]: https://github.com/sitmun/sitmun-backend-core/compare/sitmun-backend-core/v1.1.0...sitmun-backend-core/v1.1.1
[1.1.0]: https://github.com/sitmun/sitmun-backend-core/compare/sitmun-backend-core/1.0.0...sitmun-backend-core/1.1.0
[1.0.0]: https://github.com/sitmun/sitmun-backend-core/compare/sitmun-backend-core/1.0.0...sitmun-backend-core/1.0.0
