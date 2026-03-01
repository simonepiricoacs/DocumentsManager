package it.water.documents.manager.repository.s3;

import it.water.core.model.exceptions.WaterRuntimeException;
import it.water.documents.manager.repository.s3.api.DocumentRepositoryS3Option;
import it.water.documents.manager.repository.s3.service.DocumentRepositoryS3ClientImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit test for DocumentRepositoryS3Client using Mockito.
 * Tests S3 operations by mocking the AWS S3Client.
 */
@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DocumentRepositoryS3ClientTest {

    private static final String TEST_BUCKET = "test-bucket";
    private static final String TEST_KEY = "test-key";
    private static final byte[] TEST_CONTENT = "test content".getBytes();

    @Mock
    private S3Client s3Client;

    @Mock
    private DocumentRepositoryS3Option documentRepositoryS3Option;

    private DocumentRepositoryS3ClientImpl documentRepositoryS3Client;

    @BeforeEach
    void setUp() throws Exception {
        documentRepositoryS3Client = new DocumentRepositoryS3ClientImpl();
        documentRepositoryS3Client.setDocumentRepositoryOption(documentRepositoryS3Option);
        injectS3Client(documentRepositoryS3Client, s3Client);
    }

    /**
     * Inject mocked S3Client using reflection.
     */
    private void injectS3Client(DocumentRepositoryS3ClientImpl client, S3Client mockS3Client) throws Exception {
        Field s3ClientField = DocumentRepositoryS3ClientImpl.class.getDeclaredField("s3Client");
        s3ClientField.setAccessible(true);
        s3ClientField.set(client, mockS3Client);
    }

    /**
     * Test upload with byte array content.
     */
    @Test
    @Order(1)
    void uploadWithByteArrayShouldWork() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        documentRepositoryS3Client.upload(TEST_BUCKET, TEST_KEY, TEST_CONTENT);

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client, times(1)).putObject(requestCaptor.capture(), any(RequestBody.class));

        PutObjectRequest capturedRequest = requestCaptor.getValue();
        Assertions.assertEquals(TEST_BUCKET, capturedRequest.bucket());
        Assertions.assertEquals(TEST_KEY, capturedRequest.key());
    }

    /**
     * Test upload with InputStream content.
     */
    @Test
    @Order(2)
    void uploadWithInputStreamShouldWork() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        InputStream inputStream = new ByteArrayInputStream(TEST_CONTENT);
        documentRepositoryS3Client.upload(TEST_BUCKET, TEST_KEY, inputStream, TEST_CONTENT.length);

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client, times(1)).putObject(requestCaptor.capture(), any(RequestBody.class));

        PutObjectRequest capturedRequest = requestCaptor.getValue();
        Assertions.assertEquals(TEST_BUCKET, capturedRequest.bucket());
        Assertions.assertEquals(TEST_KEY, capturedRequest.key());
        Assertions.assertEquals(TEST_CONTENT.length, capturedRequest.contentLength());
    }

    /**
     * Test download returns byte array.
     */
    @Test
    @Order(3)
    void downloadShouldReturnByteArray() {
        GetObjectResponse response = GetObjectResponse.builder().build();
        software.amazon.awssdk.core.ResponseBytes<GetObjectResponse> responseBytes =
                software.amazon.awssdk.core.ResponseBytes.fromByteArray(response, TEST_CONTENT);

        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);

        byte[] result = documentRepositoryS3Client.download(TEST_BUCKET, TEST_KEY);

        ArgumentCaptor<GetObjectRequest> requestCaptor = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3Client, times(1)).getObjectAsBytes(requestCaptor.capture());

        GetObjectRequest capturedRequest = requestCaptor.getValue();
        Assertions.assertEquals(TEST_BUCKET, capturedRequest.bucket());
        Assertions.assertEquals(TEST_KEY, capturedRequest.key());
        Assertions.assertArrayEquals(TEST_CONTENT, result);
    }

    /**
     * Test downloadAsStream returns InputStream.
     */
    @Test
    @Order(4)
    void downloadAsStreamShouldReturnInputStream() {
        GetObjectResponse response = GetObjectResponse.builder().build();
        ResponseInputStream<GetObjectResponse> responseInputStream =
                new ResponseInputStream<>(response, new ByteArrayInputStream(TEST_CONTENT));

        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseInputStream);

        InputStream result = documentRepositoryS3Client.downloadAsStream(TEST_BUCKET, TEST_KEY);

        ArgumentCaptor<GetObjectRequest> requestCaptor = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3Client, times(1)).getObject(requestCaptor.capture());

        GetObjectRequest capturedRequest = requestCaptor.getValue();
        Assertions.assertEquals(TEST_BUCKET, capturedRequest.bucket());
        Assertions.assertEquals(TEST_KEY, capturedRequest.key());
        Assertions.assertNotNull(result);
    }

    /**
     * Test delete operation.
     */
    @Test
    @Order(5)
    void deleteShouldWork() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(DeleteObjectResponse.builder().build());

        documentRepositoryS3Client.delete(TEST_BUCKET, TEST_KEY);

        ArgumentCaptor<DeleteObjectRequest> requestCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client, times(1)).deleteObject(requestCaptor.capture());

        DeleteObjectRequest capturedRequest = requestCaptor.getValue();
        Assertions.assertEquals(TEST_BUCKET, capturedRequest.bucket());
        Assertions.assertEquals(TEST_KEY, capturedRequest.key());
    }

    /**
     * Test exists returns true when object exists.
     */
    @Test
    @Order(6)
    void existsShouldReturnTrueWhenObjectExists() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().build());

        boolean result = documentRepositoryS3Client.exists(TEST_BUCKET, TEST_KEY);

        ArgumentCaptor<HeadObjectRequest> requestCaptor = ArgumentCaptor.forClass(HeadObjectRequest.class);
        verify(s3Client, times(1)).headObject(requestCaptor.capture());

        HeadObjectRequest capturedRequest = requestCaptor.getValue();
        Assertions.assertEquals(TEST_BUCKET, capturedRequest.bucket());
        Assertions.assertEquals(TEST_KEY, capturedRequest.key());
        Assertions.assertTrue(result);
    }

    /**
     * Test exists returns false when object does not exist.
     */
    @Test
    @Order(7)
    void existsShouldReturnFalseWhenObjectDoesNotExist() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("Key not found").build());

        boolean result = documentRepositoryS3Client.exists(TEST_BUCKET, TEST_KEY);

        verify(s3Client, times(1)).headObject(any(HeadObjectRequest.class));
        Assertions.assertFalse(result);
    }

    /**
     * Test upload throws exception on S3 error.
     * executeS3Operation wraps all S3 exceptions in WaterRuntimeException.
     */
    @Test
    @Order(8)
    void uploadShouldThrowExceptionOnS3Error() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().message("S3 error").build());

        WaterRuntimeException ex = Assertions.assertThrows(WaterRuntimeException.class, () ->
                documentRepositoryS3Client.upload(TEST_BUCKET, TEST_KEY, TEST_CONTENT));
        Assertions.assertInstanceOf(S3Exception.class, ex.getCause());
    }

    /**
     * Test download throws exception on S3 error.
     * executeS3Operation wraps NoSuchKeyException in WaterRuntimeException.
     */
    @Test
    @Order(9)
    void downloadShouldThrowExceptionOnS3Error() {
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("Key not found").build());

        WaterRuntimeException ex = Assertions.assertThrows(WaterRuntimeException.class, () ->
                documentRepositoryS3Client.download(TEST_BUCKET, TEST_KEY));
        Assertions.assertInstanceOf(NoSuchKeyException.class, ex.getCause());
    }

    /**
     * Test delete throws exception on S3 error.
     * executeS3Operation wraps all S3 exceptions in WaterRuntimeException.
     */
    @Test
    @Order(10)
    void deleteShouldThrowExceptionOnS3Error() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(S3Exception.builder().message("S3 error").build());

        WaterRuntimeException ex = Assertions.assertThrows(WaterRuntimeException.class, () ->
                documentRepositoryS3Client.delete(TEST_BUCKET, TEST_KEY));
        Assertions.assertInstanceOf(S3Exception.class, ex.getCause());
    }

    /**
     * Test copy operation.
     */
    @Test
    @Order(11)
    void copyShouldWork() {
        String destBucket = "dest-bucket";
        String destKey = "dest-key";

        when(s3Client.copyObject(any(CopyObjectRequest.class)))
                .thenReturn(CopyObjectResponse.builder().build());

        documentRepositoryS3Client.copy(TEST_BUCKET, TEST_KEY, destBucket, destKey);

        ArgumentCaptor<CopyObjectRequest> requestCaptor = ArgumentCaptor.forClass(CopyObjectRequest.class);
        verify(s3Client, times(1)).copyObject(requestCaptor.capture());

        CopyObjectRequest capturedRequest = requestCaptor.getValue();
        Assertions.assertEquals(TEST_BUCKET, capturedRequest.sourceBucket());
        Assertions.assertEquals(TEST_KEY, capturedRequest.sourceKey());
        Assertions.assertEquals(destBucket, capturedRequest.destinationBucket());
        Assertions.assertEquals(destKey, capturedRequest.destinationKey());
    }

    /**
     * Test copy within same bucket (for move/rename operations).
     */
    @Test
    @Order(12)
    void copyShouldWorkWithinSameBucket() {
        String destKey = "new-path/test-key";

        when(s3Client.copyObject(any(CopyObjectRequest.class)))
                .thenReturn(CopyObjectResponse.builder().build());

        documentRepositoryS3Client.copy(TEST_BUCKET, TEST_KEY, TEST_BUCKET, destKey);

        ArgumentCaptor<CopyObjectRequest> requestCaptor = ArgumentCaptor.forClass(CopyObjectRequest.class);
        verify(s3Client, times(1)).copyObject(requestCaptor.capture());

        CopyObjectRequest capturedRequest = requestCaptor.getValue();
        Assertions.assertEquals(TEST_BUCKET, capturedRequest.sourceBucket());
        Assertions.assertEquals(TEST_KEY, capturedRequest.sourceKey());
        Assertions.assertEquals(TEST_BUCKET, capturedRequest.destinationBucket());
        Assertions.assertEquals(destKey, capturedRequest.destinationKey());
    }

    /**
     * Test copy throws exception on S3 error.
     * executeS3Operation wraps all S3 exceptions in WaterRuntimeException.
     */
    @Test
    @Order(13)
    void copyShouldThrowExceptionOnS3Error() {
        when(s3Client.copyObject(any(CopyObjectRequest.class)))
                .thenThrow(S3Exception.builder().message("S3 error").build());

        WaterRuntimeException ex = Assertions.assertThrows(WaterRuntimeException.class, () ->
                documentRepositoryS3Client.copy(TEST_BUCKET, TEST_KEY, "dest-bucket", "dest-key"));
        Assertions.assertInstanceOf(S3Exception.class, ex.getCause());
    }

    /**
     * Test copy throws exception when source does not exist.
     * executeS3Operation wraps NoSuchKeyException in WaterRuntimeException.
     */
    @Test
    @Order(14)
    void copyShouldThrowExceptionWhenSourceNotFound() {
        when(s3Client.copyObject(any(CopyObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("Source key not found").build());

        WaterRuntimeException ex = Assertions.assertThrows(WaterRuntimeException.class, () ->
                documentRepositoryS3Client.copy(TEST_BUCKET, TEST_KEY, "dest-bucket", "dest-key"));
        Assertions.assertInstanceOf(NoSuchKeyException.class, ex.getCause());
    }

    /**
     * Test executeS3Operation catches NoSuchBucketException and wraps it in WaterRuntimeException.
     */
    @Test
    @Order(15)
    void uploadShouldWrapNoSuchBucketException() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(NoSuchBucketException.builder().message("Bucket not found").build());

        WaterRuntimeException ex = Assertions.assertThrows(WaterRuntimeException.class, () ->
                documentRepositoryS3Client.upload(TEST_BUCKET, TEST_KEY, TEST_CONTENT));
        Assertions.assertInstanceOf(NoSuchBucketException.class, ex.getCause());
    }

    /**
     * Test executeS3Operation catches SdkClientException (connection errors) and wraps it in WaterRuntimeException.
     */
    @Test
    @Order(16)
    void uploadShouldWrapSdkClientException() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(SdkClientException.create("Connection refused"));

        WaterRuntimeException ex = Assertions.assertThrows(WaterRuntimeException.class, () ->
                documentRepositoryS3Client.upload(TEST_BUCKET, TEST_KEY, TEST_CONTENT));
        Assertions.assertInstanceOf(SdkClientException.class, ex.getCause());
    }

    /**
     * Test executeS3Operation catches any unexpected Exception and wraps it in WaterRuntimeException.
     */
    @Test
    @Order(17)
    void uploadShouldWrapUnexpectedException() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(new RuntimeException("Unexpected internal error"));

        WaterRuntimeException ex = Assertions.assertThrows(WaterRuntimeException.class, () ->
                documentRepositoryS3Client.upload(TEST_BUCKET, TEST_KEY, TEST_CONTENT));
        Assertions.assertInstanceOf(RuntimeException.class, ex.getCause());
    }

    /**
     * Test exists() propagates S3Exception other than NoSuchKeyException unwrapped.
     * exists() only catches NoSuchKeyException - other S3 errors are not handled.
     */
    @Test
    @Order(18)
    void existsShouldPropagateUnhandledS3Exception() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder().message("Access denied").build());

        Assertions.assertThrows(S3Exception.class, () ->
                documentRepositoryS3Client.exists(TEST_BUCKET, TEST_KEY));
    }
}
