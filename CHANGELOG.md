# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- **Entity Graph Optimization** - Enhanced cartography and task repositories with entity graphs and optimized queries ([a952e8e](https://github.com/sitmun/sitmun-backend-core/commit/a952e8e))
- **Tree Type Validation Endpoint** - Added tree type validation endpoint to prevent client issues ([66b3ac0](https://github.com/sitmun/sitmun-backend-core/commit/66b3ac0))
- **CRS Support** - Added CRS support to ServiceDto and updated ProfileMapper ([0abf7ca](https://github.com/sitmun/sitmun-backend-core/commit/0abf7ca))
- **Application Name Field** - Added name field to ApplicationDto and ApplicationDtoLittle ([815f54e](https://github.com/sitmun/sitmun-backend-core/commit/815f54e))
- **Language Controller** - Added new LanguageController to manage language-related endpoints ([24f5351](https://github.com/sitmun/sitmun-backend-core/commit/24f5351ef3537687c791eded2ea2ba0ed4823008))
- **MBTiles Service Configuration** - Added MBTiles service URL configuration support for enhanced mapping capabilities ([5d9b27d](https://github.com/sitmun/sitmun-backend-core/commit/5d9b27d427d7b2d9de557e29b4b608d780fc19dc))
- **Tree Node View Mode** - Added new viewmode option for tree nodes in codelist configuration ([1a84404](https://github.com/sitmun/sitmun-backend-core/commit/1a8440424f92711923ec92c46f985578477846ed))
- **Enhanced User Management** - Implemented comprehensive user management and security system with position tracking and OTP password reset functionality ([2d14e43](https://github.com/sitmun/sitmun-backend-core/commit/2d14e438f172f50f5be97c717d352ba3d1f40dd8))
- **Test Coverage** - Added comprehensive tests for MBTiles and security-related changes ([d5fbb6e](https://github.com/sitmun/sitmun-backend-core/commit/d5fbb6ebd611051f5728d48b9c0370f674cdefc5))

### Changed
- **RFC 9457 Problem Details** - Implemented RFC 9457 Problem Details for error responses ([87578be](https://github.com/sitmun/sitmun-backend-core/commit/87578be))
- **Password Verification** - Improved password verification implementation with enhanced security measures ([fccf964](https://github.com/sitmun/sitmun-backend-core/commit/fccf964a85d56ca970ecf31a049d1115a8959fd6))
- **Language Endpoints** - Restructured language endpoints and improved password verification system ([965a86f](https://github.com/sitmun/sitmun-backend-core/commit/965a86f46b43b864fd5b40bd7d181e81b80172e9))
- **Spring Boot Tests** - Modernized Spring Boot test annotations and removed deprecated testing patterns ([046a7f4](https://github.com/sitmun/sitmun-backend-core/commit/046a7f4b8348df468a07b227dedbd5eac8cd7812))
- **Code Quality** - Applied comprehensive code formatting and cleanup across the codebase ([22b241b](https://github.com/sitmun/sitmun-backend-core/commit/22b241be8f023df26f4cb4dde773deb315758a90))

### Fixed
- **Oracle Schema** - Updated oracle schema for STM_USER and STM_TOKEN_USER tables ([f01d130](https://github.com/sitmun/sitmun-backend-core/commit/f01d130))
- **LazyInitializationException** - Fixed handling of LazyInitializationException during constraint violation handling ([6264fa3](https://github.com/sitmun/sitmun-backend-core/commit/6264fa3))
- **Database Column Size** - Increased STM_TSK_UI.TUI_NAME column size to 50 characters ([89346eb](https://github.com/sitmun/sitmun-backend-core/commit/89346eb))
- **Client Configuration i18n** - Prevented unnecessary internationalization updates on client configuration requests to improve performance ([f5d73d5](https://github.com/sitmun/sitmun-backend-core/commit/f5d73d5f7c89d7aaad42257d7599d9cd99f6cc4c))
- **Profile Security Updates** - Fixed profile update security issues with enhanced validation ([e393f61](https://github.com/sitmun/sitmun-backend-core/commit/e393f61459f68fc9dba410b8ce5d82f1c01caee0))
- **Password Reset Token Operations** - Fixed token password reset operation to ensure proper security handling ([5ef7488](https://github.com/sitmun/sitmun-backend-core/commit/5ef74887586a4fb38e954de415b66e27e2122018))
- **Profile Security** - Updated profile update mechanism for enhanced security measures ([6307d88](https://github.com/sitmun/sitmun-backend-core/commit/6307d887c2a203d29bb9eea1283779c7ae489b4d))
- **Tree Node View Mode Description** - Updated description for tree node viewmode in application configuration ([574f8b9](https://github.com/sitmun/sitmun-backend-core/commit/574f8b9e2fd67c0a66f60ea3d7df5b052920a308))
- **Code Formatting** - Improved code formatting and removed duplicate imports ([51f887a](https://github.com/sitmun/sitmun-backend-core/commit/51f887a117a2d5cc87520e907c08ea2d3d92c85c))

### Removed
- **Stale Test** - Removed stale test ([f37adeb](https://github.com/sitmun/sitmun-backend-core/commit/f37adeb))

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

[Unreleased]: https://github.com/sitmun/sitmun-backend-core/compare/sitmun-backend-core/v1.1.1...HEAD
[1.1.1]: https://github.com/sitmun/sitmun-backend-core/compare/sitmun-backend-core/v1.1.0...sitmun-backend-core/v1.1.1
[1.1.0]: https://github.com/sitmun/sitmun-backend-core/compare/sitmun-backend-core/1.0.0...sitmun-backend-core/1.1.0
[1.0.0]: https://github.com/sitmun/sitmun-backend-core/compare/sitmun-backend-core/1.0.0...sitmun-backend-core/1.0.0