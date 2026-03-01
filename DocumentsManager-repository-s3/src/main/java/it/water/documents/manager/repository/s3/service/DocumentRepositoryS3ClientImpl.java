package it.water.documents.manager.repository.s3.service;

import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.core.interceptors.annotations.Inject;
import it.water.core.model.exceptions.WaterRuntimeException;
import it.water.documents.manager.repository.s3.api.DocumentRepositoryS3Option;
import it.water.documents.manager.repository.s3.api.DocumentRepositoryS3Client;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;
import java.net.URI;
import java.util.function.Supplier;

import static it.water.documents.manager.repository.s3.config.DocumentRepositoryS3Constant.*;

@FrameworkComponent(services =  {DocumentRepositoryS3Client.class})
@Slf4j
public class DocumentRepositoryS3ClientImpl implements DocumentRepositoryS3Client {


    private S3Client s3Client;

    @Inject
    @Setter
    private DocumentRepositoryS3Option documentRepositoryOption;


    private S3Client getS3Client() {
        if (s3Client == null) {
            log.debug("Initializing S3 Client with endpoint: {}", documentRepositoryOption.getEndpoint());
            s3Client = S3Client.builder()
                    .endpointOverride(URI.create(documentRepositoryOption.getEndpoint()))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(
                                    documentRepositoryOption.getAccessKey(),
                                    documentRepositoryOption.getSecretKey())))
                    .serviceConfiguration(S3Configuration.builder()
                            .pathStyleAccessEnabled(documentRepositoryOption.isPathStyleEnabled())
                            .build())
                    .region(Region.of(documentRepositoryOption.getRegion()))
                    .build();
            log.debug("S3 Client initialized successfully");
        }

        return s3Client;
    }

    @Override
    public void upload(String bucket, String key, byte[] content) {
        executeS3Operation(OP_UPLOAD, bucket, key, () -> {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            getS3Client().putObject(request, RequestBody.fromBytes(content));
            return null;
        });
    }

    @Override
    public void upload(String bucket, String key, InputStream content, long contentLength) {
        executeS3Operation(OP_STREAM_UPLOAD, bucket, key, () -> {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentLength(contentLength)
                    .build();

            getS3Client().putObject(
                    request,
                    RequestBody.fromInputStream(content, contentLength)
            );
            return null;
        });
    }

    @Override
    public byte[] download(String bucket, String key) {
        return executeS3Operation(OP_DOWNLOAD, bucket, key, () -> {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            return getS3Client()
                    .getObjectAsBytes(request)
                    .asByteArray();
        });
    }

    @Override
    public InputStream downloadAsStream(String bucket, String key) {
        return executeS3Operation(OP_STREAM_DOWNLOAD, bucket, key, () -> {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            return getS3Client().getObject(request);
        });
    }


    @Override
    public void delete(String bucket, String key) {
        executeS3Operation(OP_DELETE, bucket, key, () -> {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            getS3Client().deleteObject(request);
            return null;
        });
    }


    @Override
    public boolean exists(String bucket, String key) {
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            getS3Client().headObject(request);
            return true;

        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    @Override
    public void copy(String sourceBucket, String sourceKey,
                     String destinationBucket, String destinationKey) {

        executeS3Operation(OP_COPY, sourceBucket, sourceKey, () -> {

            CopyObjectRequest request = CopyObjectRequest.builder()
                    .sourceBucket(sourceBucket)
                    .sourceKey(sourceKey)
                    .destinationBucket(destinationBucket)
                    .destinationKey(destinationKey)
                    .build();

            getS3Client().copyObject(request);
            return null;
        });
    }



    private <T> T executeS3Operation(String operation, String bucket, String key, Supplier<T> action
    ) {
        String endpoint = documentRepositoryOption.getEndpoint();

        try {
            log.debug("S3 {} started - endpoint={}, bucket={}, key={}",
                    operation, endpoint, bucket, key);

            T result = action.get();

            log.info("S3 {} successful - bucket={}, key={}", operation, bucket, key);
            return result;

        } catch (NoSuchKeyException e) {
            throw new WaterRuntimeException(
                    String.format(ERR_FILE_NOT_FOUND, operation, bucket, key), e);

        } catch (NoSuchBucketException e) {
            throw new WaterRuntimeException(
                    String.format(ERR_BUCKET_NOT_FOUND, bucket, endpoint), e);

        } catch (SdkClientException e) {
            throw new WaterRuntimeException(
                    String.format(ERR_CONNECTION, endpoint, operation, bucket, key), e);

        } catch (S3Exception e) {
            throw new WaterRuntimeException(
                    String.format(ERR_S3, operation, bucket, key, e.statusCode()), e);

        } catch (Exception e) {
            throw new WaterRuntimeException(
                    String.format(ERR_UNEXPECTED, operation, bucket, key), e);
        }
    }


}


