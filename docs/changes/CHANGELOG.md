# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Contract-first API scaffolding generated via OpenAPI. See ADR: [ADR0001](../adr/ADR0001-contract-first-api.md).
- Customer endpoints expose OpenAPI annotations.
- New api for /orders/{id}. Added to the OpenAPI.yaml and the controllers to implement the function. 
- New API for /customer/{id}. Added to the OpenAPI.yaml and the controllers to implement the function. 

### Changed
- DTOs now generated with `DTO` suffix via codegen. Rationale: ensure clear separation from domain entities.
- Changed the OpenAPI.yaml so that the post creates return the application as per current controller application. Note that for customer I changed this to return the customer object as per implementation.
- Changed the post methods to use an explicit DTO to separate the API from the entity model. The API is auto generated.
- OpenAPI.yaml has the Order.Customer.description but the entity has Order.customer.name - changed the OpenAPI.yaml to use name instead of description.
- OpenAPI.yaml has the order create not taking the customer components of the order object. So changed the OpenAPI.yaml to take an order object, this is consistent with the test case.

### Deprecated
- (none)

### Removed
- Hand coded DTO objects in com.example.store.dto and replaced with the core dto 

### Fixed
- Corrected `description` field typo in OpenAPI spec for `/order` POST.

### Security
- (none)

## [1.0.0] - 2025-09-05
### Added
- Initial release.
