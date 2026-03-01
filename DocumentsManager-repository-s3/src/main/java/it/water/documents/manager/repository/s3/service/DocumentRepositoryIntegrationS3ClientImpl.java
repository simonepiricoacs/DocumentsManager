package it.water.documents.manager.repository.s3.service;

import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.core.interceptors.annotations.Inject;
import it.water.documents.manager.api.integration.DocumentRepositoryIntegrationClient;
import it.water.documents.manager.repository.s3.api.DocumentRepositoryS3Client;
import it.water.documents.manager.repository.s3.api.DocumentRepositoryS3Option;

import it.water.core.model.exceptions.WaterRuntimeException;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@FrameworkComponent(services =  {DocumentRepositoryIntegrationClient.class})
public class DocumentRepositoryIntegrationS3ClientImpl implements DocumentRepositoryIntegrationClient {

    @Setter
    @Inject
    private DocumentRepositoryS3Client documentRepositoryS3Client;

    @Setter
    @Inject
    private DocumentRepositoryS3Option documentRepositoryS3Option;

    private String getBucket() {
        return documentRepositoryS3Option.getBucket();
    }

    @Override
    public void addNewFile(String path, InputStream sourceFile) {
        try {

            byte[] content = sourceFile.readAllBytes();
            documentRepositoryS3Client.upload(getBucket(), path, content);
        } catch (IOException e) {
            throw new WaterRuntimeException("Failed to upload file: " + path, e);
        }

    }

    @Override
    public void updateFile(String path, InputStream sourceFile) {
        try {
            byte[] content = sourceFile.readAllBytes();
            documentRepositoryS3Client.upload(getBucket(), path, content);
        } catch (IOException e) {
            throw new WaterRuntimeException("Failed to update file: " + path, e);
        }
    }

    @Override
    public void moveFile(String oldPath, String newPath, String fileName) {
        String sourceKey = oldPath + "/" + fileName;
        String destKey = newPath + "/" + fileName;
        documentRepositoryS3Client.copy(getBucket(), sourceKey, getBucket(), destKey);
        documentRepositoryS3Client.delete(getBucket(), sourceKey);
    }

    @Override
    public void renameFile(String path, String oldFileName, String newFileName) {
        String sourceKey = path + "/" + oldFileName;
        String destKey = path + "/" + newFileName;
        documentRepositoryS3Client.copy(getBucket(), sourceKey, getBucket(), destKey);
        documentRepositoryS3Client.delete(getBucket(), sourceKey);
    }

    @Override
    public void deleteFile(String path, String fileName) {
        String key = path + "/" + fileName;
        documentRepositoryS3Client.delete(getBucket(), key);
    }


    @Override
    public InputStream fetchDocumentContent(String path) {
        return documentRepositoryS3Client.downloadAsStream(getBucket(), path);
    }


    // FOLDER OPERATIONS

    @Override
    public void addFolder(String path, String folderName) {
        log.debug("just for test Adding folder {}", folderName);
    }

    @Override
    public void emptyFolder(String path) {
        log.debug("just for test Emptying folder {}", path);
    }

    @Override
    public void removeFolder(String path) {
        log.debug("just for test Removing folder {}", path);
    }

    @Override
    public void renameFolder(String path, String oldName, String newName) {
        log.debug("just for test Renaming folder from {} to {}", oldName, newName);
    }

    @Override
    public void moveFolder(String oldPath, String newPath) {
        log.debug("just for test Moving folder from {} to {}", oldPath, newPath);
    }




}
