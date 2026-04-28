package it.water.documents.manager.repository.s3;

import it.water.documents.manager.repository.s3.api.DocumentStorageClient;
import it.water.documents.manager.repository.s3.api.DocumentRepositoryS3Option;
import it.water.documents.manager.repository.s3.service.DocumentRepositoryIntegrationS3ClientImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.mockito.Mockito.*;

/**
 * Unit test for DocumentRepositoryIntegrationS3ClientImpl.
 *
 * @author Christian Claudio Rosati
 */
@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DocumentRepositoryIntegrationS3ClientTest {

    private static final String BUCKET_NAME = "test-bucket";
    private static final byte[] TEST_CONTENT = "test content".getBytes();

    @Mock
    private DocumentStorageClient documentStorageClient;

    @Mock
    private DocumentRepositoryS3Option documentRepositoryS3Option;

    @InjectMocks
    private DocumentRepositoryIntegrationS3ClientImpl integrationClient;

    @BeforeEach
    void setUp() {
        lenient().when(documentRepositoryS3Option.getBucket()).thenReturn(BUCKET_NAME);
    }


    @Test
    @Order(1)
    void addNewFileShouldUploadContent() {
        InputStream inputStream = new ByteArrayInputStream(TEST_CONTENT);
        String path = "documents/test";

        integrationClient.addNewFile(path, inputStream);

        verify(documentStorageClient, times(1))
                .upload(eq(BUCKET_NAME), eq(path), eq(TEST_CONTENT));
    }

    @Test
    @Order(2)
    void addNewFileShouldThrowRuntimeExceptionOnIOException() throws IOException {
        InputStream mockInputStream = mock(InputStream.class);
        when(mockInputStream.readAllBytes()).thenThrow(new IOException("Read error"));

        Assertions.assertThrows(RuntimeException.class, () ->
                integrationClient.addNewFile("path", mockInputStream));
    }

    @Test
    @Order(3)
    void updateFileShouldUploadContent() {
        InputStream inputStream = new ByteArrayInputStream(TEST_CONTENT);
        String path = "documents/test";

        integrationClient.updateFile(path, inputStream);

        // verify that the upload method was called exactly once with the correct parameters
        verify(documentStorageClient, times(1))
                .upload(eq(BUCKET_NAME), eq(path), eq(TEST_CONTENT));
    }

    @Test
    @Order(4)
    void updateFileShouldThrowRuntimeExceptionOnIOException() throws IOException {
        InputStream mockInputStream = mock(InputStream.class);
        when(mockInputStream.readAllBytes()).thenThrow(new IOException("Read error"));

        Assertions.assertThrows(RuntimeException.class, () ->
                integrationClient.updateFile("path", mockInputStream));
    }

    @Test
    @Order(5)
    void deleteFileShouldDeleteFromS3() {
        String path = "documents/folder";
        String fileName = "file.pdf";

        integrationClient.deleteFile(path, fileName);

        verify(documentStorageClient, times(1))
                .delete(eq(BUCKET_NAME), eq(path + "/" + fileName));
    }

    @Test
    @Order(6)
    void fetchDocumentContentShouldReturnInputStream() {
        String path = "documents/test";
        InputStream expectedStream = new ByteArrayInputStream(TEST_CONTENT);
        when(documentStorageClient.downloadAsStream(BUCKET_NAME, path))
                .thenReturn(expectedStream);

        InputStream result = integrationClient.fetchDocumentContent(path);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(expectedStream, result);
        verify(documentStorageClient, times(1))
                .downloadAsStream(eq(BUCKET_NAME), eq(path));
    }

    @Test
    @Order(7)
    void moveFileShouldCopyAndDeleteSource() {
        String oldPath = "documents/old";
        String newPath = "documents/new";
        String fileName = "file.pdf";
        String sourceKey = oldPath + "/" + fileName;
        String destKey = newPath + "/" + fileName;

        integrationClient.moveFile(oldPath, newPath, fileName);

        verify(documentStorageClient, times(1))
                .copy(eq(BUCKET_NAME), eq(sourceKey), eq(BUCKET_NAME), eq(destKey));
        verify(documentStorageClient, times(1))
                .delete(eq(BUCKET_NAME), eq(sourceKey));
    }

    @Test
    @Order(8)
    void moveFileShouldPreserveFileName() {
        String oldPath = "path/a/b";
        String newPath = "path/x/y";
        String fileName = "document.pdf";

        integrationClient.moveFile(oldPath, newPath, fileName);

        verify(documentStorageClient).copy(
                eq(BUCKET_NAME),
                eq("path/a/b/document.pdf"),
                eq(BUCKET_NAME),
                eq("path/x/y/document.pdf")
        );
    }

    @Test
    @Order(9)
    void renameFileShouldCopyAndDeleteSource() {
        String path = "documents/folder";
        String oldFileName = "old-name.pdf";
        String newFileName = "new-name.pdf";
        String sourceKey = path + "/" + oldFileName;
        String destKey = path + "/" + newFileName;

        integrationClient.renameFile(path, oldFileName, newFileName);

        verify(documentStorageClient, times(1))
                .copy(eq(BUCKET_NAME), eq(sourceKey), eq(BUCKET_NAME), eq(destKey));
        verify(documentStorageClient, times(1))
                .delete(eq(BUCKET_NAME), eq(sourceKey));
    }

    @Test
    @Order(10)
    void renameFileShouldKeepSamePath() {
        String path = "documents/invoices/2024";
        String oldName = "invoice_001.pdf";
        String newName = "invoice_001_final.pdf";

        integrationClient.renameFile(path, oldName, newName);

        verify(documentStorageClient).copy(
                eq(BUCKET_NAME),
                eq("documents/invoices/2024/invoice_001.pdf"),
                eq(BUCKET_NAME),
                eq("documents/invoices/2024/invoice_001_final.pdf")
        );
        verify(documentStorageClient).delete(
                eq(BUCKET_NAME),
                eq("documents/invoices/2024/invoice_001.pdf")
        );
    }

    // FOLDER OPERATIONS TESTS

    @Test
    @Order(11)
    void addFolderShouldWork() {
        Assertions.assertDoesNotThrow(() ->
                integrationClient.addFolder("documents", "newFolder"));
    }

    @Test
    @Order(12)
    void emptyFolderShouldWork() {
        Assertions.assertDoesNotThrow(() ->
                integrationClient.emptyFolder("documents/folder"));
    }

    @Test
    @Order(13)
    void removeFolderShouldWork() {
        Assertions.assertDoesNotThrow(() ->
                integrationClient.removeFolder("documents/folder"));
    }

    @Test
    @Order(14)
    void renameFolderShouldWork() {
        Assertions.assertDoesNotThrow(() ->
                integrationClient.renameFolder("documents", "oldName", "newName"));
    }

    @Test
    @Order(15)
    void moveFolderShouldWork() {
        Assertions.assertDoesNotThrow(() ->
                integrationClient.moveFolder("documents/old", "documents/new"));
    }
}
