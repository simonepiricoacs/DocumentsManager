# DocumentsManager Module — Document & Folder Management

## Purpose
Provides document storage and hierarchical folder management for Water Framework applications. Separates **metadata** (stored in the database via JPA) from **content** (stored via a pluggable `DocumentRepositoryIntegrationClient` — filesystem, S3, cloud DMS, etc.). Supports file upload/download, MIME type detection, and ownership-based access control.

## Sub-modules

| Sub-module | Runtime | Key Classes |
|---|---|---|
| `DocumentsManager-api` | All | `DocumentApi`, `DocumentSystemApi`, `DocumentRestApi`, `FolderApi`, `FolderSystemApi`, `FolderRestApi`, `DocumentRepository`, `FolderRepository`, `DocumentRepositoryIntegrationClient` |
| `DocumentsManager-model` | All | `Document`, `Folder` entities |
| `DocumentsManager-service` | Water/OSGi | Service impl, repositories, REST controllers |
| `DocumentsManager-service-spring` | Spring Boot | Spring MVC REST controllers, Spring Boot app config |

## Document Entity

```java
@Entity
@Table(name = "document")
@AccessControl(
    availableActions = {CrudActions.class},
    rolesPermissions = {
        @DefaultRoleAccess(roleName = "documentsManager", actions = {CrudActions.class}),
        @DefaultRoleAccess(roleName = "documentsEditor",  actions = {CrudActions.UPDATE, CrudActions.FIND, CrudActions.FIND_ALL}),
        @DefaultRoleAccess(roleName = "documentsViewer",  actions = {CrudActions.FIND, CrudActions.FIND_ALL})
    }
)
public class Document extends AbstractJpaEntity implements ProtectedEntity, OwnedResource {
    @NotNull @NoMalitiusCode
    private String path;             // storage path within the repository

    @NotNull @NoMalitiusCode
    private String filename;         // original filename

    private String uid;              // unique content identifier (UUID)
    private String mimeType;         // e.g., "application/pdf", "image/png"
    private long ownerUserId;

    // Transient — not persisted, used during upload/download
    @Transient private InputStream content;
    @Transient private OutputStream outputStream;
}
```

## Folder Entity

```java
@Entity
@Table(name = "document_folder")
@AccessControl(
    availableActions = {CrudActions.class},
    rolesPermissions = {
        @DefaultRoleAccess(roleName = "foldersManager", actions = {CrudActions.class}),
        @DefaultRoleAccess(roleName = "foldersEditor",  actions = {CrudActions.UPDATE, CrudActions.FIND, CrudActions.FIND_ALL}),
        @DefaultRoleAccess(roleName = "foldersViewer",  actions = {CrudActions.FIND, CrudActions.FIND_ALL})
    }
)
public class Folder extends AbstractJpaEntity implements ProtectedEntity, OwnedResource {
    @NotNull @NoMalitiusCode
    private String name;

    @NoMalitiusCode
    private String description;

    private Long parentFolderId;     // null = root folder
    private long ownerUserId;
}
```

## DocumentRepositoryIntegrationClient (Pluggable Storage Backend)

```java
public interface DocumentRepositoryIntegrationClient {
    // Store content and return the storage path/UID
    String store(String filename, InputStream content);

    // Retrieve content by UID
    InputStream retrieve(String uid);

    // Delete stored content
    void delete(String uid);

    // Check existence
    boolean exists(String uid);
}
```

**Default implementations available:**
- `FilesystemDocumentRepositoryClient` — stores to local filesystem path
- Custom implementations: S3, Azure Blob, Google Cloud Storage, Alfresco, SharePoint

**Registration:** Implement the interface, annotate with `@FrameworkComponent` — automatically discovered.

## Key Operations

### DocumentApi
```java
Document save(Document document);          // metadata + triggers storage
Document update(Document document);        // metadata update only
Document find(long id);                    // metadata only
InputStream download(long id);             // content retrieval
PaginatedResult<Document> findAll(int delta, int page, Query filter); // filtered by ownership
void remove(long id);                      // deletes metadata + content from storage
```

### FolderApi
```java
Folder save(Folder folder);
Folder update(Folder folder);
Folder find(long id);
List<Folder> findChildren(long parentId); // get sub-folders
List<Document> findDocuments(long folderId); // documents in folder
PaginatedResult<Folder> findAll(int delta, int page, Query filter);
void remove(long id);                      // removes folder and recursively deletes children
```

## REST Endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/water/documents` | Upload document (multipart/form-data) |
| `PUT` | `/water/documents` | Update document metadata |
| `GET` | `/water/documents/{id}` | Get document metadata |
| `GET` | `/water/documents/{id}/content` | Download document content |
| `GET` | `/water/documents` | List documents (filtered by ownership) |
| `DELETE` | `/water/documents/{id}` | Delete document + content |
| `POST` | `/water/documents/folders` | Create folder |
| `PUT` | `/water/documents/folders` | Update folder |
| `GET` | `/water/documents/folders/{id}` | Get folder |
| `GET` | `/water/documents/folders` | List folders |
| `DELETE` | `/water/documents/folders/{id}` | Delete folder (recursive) |

## Upload Pattern (Multipart)

```http
POST /water/documents
Content-Type: multipart/form-data

--boundary
Content-Disposition: form-data; name="file"; filename="report.pdf"
Content-Type: application/pdf

<binary content>
--boundary
Content-Disposition: form-data; name="path"
/reports/2024
--boundary--
```

## Default Roles

| Role | Allowed Actions |
|---|---|
| `documentsManager` | Full CRUD on documents |
| `documentsEditor` | UPDATE, FIND, FIND_ALL |
| `documentsViewer` | FIND, FIND_ALL |
| `foldersManager` | Full CRUD on folders |
| `foldersEditor` | UPDATE, FIND, FIND_ALL |
| `foldersViewer` | FIND, FIND_ALL |

## Dependencies
- `it.water.repository.jpa:JpaRepository-api` — `AbstractJpaEntity`
- `it.water.core:Core-permission` — `@AccessControl`, `CrudActions`
- `it.water.rest:Rest-persistence` — `BaseEntityRestApi`
- `jakarta.ws.rs:jakarta.ws.rs-api` — multipart upload support

## Testing
- Unit tests: `WaterTestExtension` — mock `DocumentRepositoryIntegrationClient` for content operations
- Test upload: provide `ByteArrayInputStream` as content
- Test download: verify `InputStream` is not null and readable
- REST tests: **Karate only** — use `multipart` keyword in feature files for upload testing
- Never call `DocumentRestController` or `FolderRestController` directly in JUnit

## Code Generation Rules
- Content storage is always delegated to `DocumentRepositoryIntegrationClient` — never store binary content in database columns
- `Document.content` is `@Transient` — it is populated during upload and returned during download, but never persisted in JPA
- `uid` is generated by the storage backend on `store()` — use it for all subsequent retrieve/delete operations
- `Folder.remove()` must cascade to child folders and documents — implement recursively or use `ON DELETE CASCADE` in schema
- REST controllers tested **exclusively via Karate**
