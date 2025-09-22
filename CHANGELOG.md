# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- User position management system with automatic creation from user configurations
- Enhanced password reset functionality with OTP (One-Time Password) codes
- JWT token validation with password change tracking for improved security
- Test mail service for password reset testing scenarios
- User position update endpoint in client configuration controller

### Changed
- Modified STM_TOKEN_USER table constraints to allow single token per user
- Enhanced password reset flow with attempt counter and expiration handling
- Improved JWT token service with last password change validation
- Updated user position repository with batch update capabilities

### Fixed
- Password reset token management to prevent multiple active tokens per user
- JWT token invalidation when password is changed
- User position creation logic to prevent duplicates

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