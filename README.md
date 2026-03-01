# DocumentsManager Module

## Module goal

The DocumentsManager module provides a robust, secure, and extensible document and folder management system for the Water Framework. It supports CRUD operations, content storage and retrieval, hierarchical folder structures, and integrates with external document repositories. The module exposes a REST API for file upload/download and folder management, with fine-grained permission control and multi-tenant support.

## Module technical characteristics

- **Java, JPA, and OSGi/Spring**: Core implementation in Java, using JPA for persistence, and supporting both OSGi and Spring environments.
- **REST API**: JAX-RS endpoints for document and folder operations, including multipart file upload and binary download.
- **Entity Model**: `Document` and `Folder` entities with unique constraints, ownership, and hierarchical relationships.
- **Repository Abstraction**: Integration with external document repositories via the `DocumentRepositoryIntegrationClient` interface.
- **Service Layer**: Separation of API, system, and repository layers for extensibility and testability.
- **Validation**: Extensive use of validation annotations to prevent malicious input and ensure data integrity.
- **Role-based Access Control**: Default roles for managers, editors, and viewers, with customizable permissions.
- **Test Coverage**: Comprehensive JUnit and Karate tests for all major features and edge cases.
- **Gradle Build**: Multi-project build with publishing, code coverage, and SonarQube integration.

## Permission and security

- **Role-based Permissions**: Three default roles for both documents and folders:
  - `documentsManager` / `foldersManager`: Full CRUD access
  - `documentsEditor` / `foldersEditor`: Create, read, update (no delete)
  - `documentsViewer` / `foldersViewer`: Read-only
- **Annotations**: Uses `@AccessControl`, `@DefaultRoleAccess`, and `@AllowGenericPermissions` for fine-grained method and entity security.
- **Ownership and Sharing**: Implements `SharedEntity` and `OwnedChildResource` for multi-tenant and owner-based access control.
- **Validation**: Prevents code injection and enforces required fields with `@NoMalitiusCode`, `@NotNullOnPersist`, and standard JSR-303 annotations.
- **REST Security**: All endpoints require authentication (`@LoggedIn`). JWT-based security is supported and configurable.

## How to use it

### Import in your project

#### OSGi:
```gradle
implementation group: 'it.water.documents.manager', name: 'DocumentsManager-api', version: project.waterVersion
implementation group: 'it.water.documents.manager', name: 'DocumentsManager-model', version: project.waterVersion
implementation group: 'it.water.documents.manager', name: 'DocumentsManager-service', version: project.waterVersion
```

#### Spring:
```gradle
implementation group: 'it.water.documents.manager', name: 'DocumentsManager-service-spring', version: project.waterVersion
```

### Setup and usage

1. **Configure properties** (see below for details)
2. **Implement or configure a `DocumentRepositoryIntegrationClient`** to connect to your storage backend (filesystem, S3, etc.)
3. **Use the REST API** for file and folder operations:
   - Upload: `POST /documents` (multipart)
   - Download: `GET /documents/content?id=...` or by path/UID
   - Folder CRUD: `/documents/folders` endpoints
4. **Programmatic usage**:
```java
@Inject
private DocumentApi documentApi;

// Save a document
Document doc = new Document("/my/path", "file.txt", "uid-123", "text/plain", ownerId);
doc.setDocumentContentInputStream(new FileInputStream("/tmp/file.txt"));
documentApi.save(doc);

// Fetch content
Document withContent = documentApi.fetchDocumentContent(doc.getId());
InputStream content = withContent.getDocumentContentInputStream();
```

## Properties and configurations

| Property                                 | Description                                 | Default                | Required |
|------------------------------------------|---------------------------------------------|------------------------|----------|
| `water.keystore.password`                | Keystore password for JWT signing           | -                      | Yes      |
| `water.keystore.alias`                   | Certificate alias in keystore               | `server-cert`          | Yes      |
| `water.keystore.file`                    | Path to keystore file                       | -                      | Yes      |
| `water.private.key.password`             | Private key password                        | -                      | Yes      |
| `water.rest.security.jwt.duration.millis`| JWT token expiration (ms)                   | `3600000`              | No       |
| `water.rest.security.jwt.validate`       | Enable/disable JWT validation for REST      | `true`                 | No       |
| `water.testMode`                         | Enable test mode                            | `false`                | No       |

Example (from test resources):
```properties
water.keystore.password=water.
water.keystore.alias=server-cert
water.keystore.file=src/test/resources/certs/server.keystore
water.private.key.password=water.
water.rest.security.jwt.duration.millis=3600000
water.rest.security.jwt.validate=false
water.testMode=true
```

## How to customize behaviours for this module

- **Custom Document Storage**: Implement `DocumentRepositoryIntegrationClient` to connect to your own storage backend (e.g., S3, filesystem, cloud storage).
- **Custom Permissions**: Use `@AccessControl` and `@DefaultRoleAccess` on your own entities or extend the default ones to change role mappings.
- **Custom REST Endpoints**: Extend or override the REST controller (`DocumentControllerImpl`) to add new endpoints or change download logic (override `prepareDownload`).
- **Custom Validation**: Add or override validation annotations on the `Document` or `Folder` entities.
- **Custom Business Logic**: Extend the service or repository classes to add hooks for virus scanning, metadata extraction, etc.
- **Integration with External Systems**: Use the integration client pattern to connect to DMS, cloud storage, or other document management systems.
- **Testing**: Use the provided test utilities and patterns to write your own tests for custom logic.

## Notes
- The module is designed for extensibility and can be adapted to a wide range of document management scenarios.
- All file operations are abstracted, so you can swap out the storage backend without changing business logic.
- The REST API is cross-framework and can be used from any HTTP client.

