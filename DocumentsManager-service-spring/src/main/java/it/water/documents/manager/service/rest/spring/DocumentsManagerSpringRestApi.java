/*
 * Copyright 2024 Aristide Cittadino
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.water.documents.manager.service.rest.spring;

import com.fasterxml.jackson.annotation.JsonView;
import it.water.core.api.model.PaginableResult;
import it.water.core.api.service.rest.FrameworkRestApi;
import it.water.core.api.service.rest.WaterJsonView;
import it.water.documents.manager.api.rest.DocumentsManagerRestApi;
import it.water.documents.manager.model.Document;
import it.water.documents.manager.model.Folder;
import it.water.service.rest.api.security.LoggedIn;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * @Author Aristide Cittadino
 * Interface exposing same methods of its parent DocumentsManagerRestApi but adding Spring annotations.
 * Swagger annotation should be found because they have been defined in the parent DocumentsManagerRestApi.
 */
@RequestMapping("/documents")
@FrameworkRestApi
public interface DocumentsManagerSpringRestApi extends DocumentsManagerRestApi {
    @LoggedIn
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @JsonView(WaterJsonView.Public.class)
    Document save(
            @RequestPart(Document.DOCUMENT_ENTITY_HTTP_PART_NAME)
            Document document, @RequestPart(value = Document.DOCUMENT_CONTENT_HTTP_PART_NAME) MultipartFile file);

    @LoggedIn
    @PutMapping(consumes = "multipart/form-data")
    @JsonView(WaterJsonView.Public.class)
    Document update(@RequestPart(Document.DOCUMENT_ENTITY_HTTP_PART_NAME) Document document, @RequestPart(value = Document.DOCUMENT_CONTENT_HTTP_PART_NAME, required = false) MultipartFile file);

    @LoggedIn
    @GetMapping("/{id}")
    @JsonView(WaterJsonView.Public.class)
    Document find(@PathVariable("id") long id);

    @LoggedIn
    @GetMapping
    @JsonView(WaterJsonView.Public.class)
    PaginableResult<Document> findAll();

    @LoggedIn
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @JsonView(WaterJsonView.Public.class)
    void remove(@PathVariable("id") long id);

    @LoggedIn
    @PostMapping("/folders")
    @JsonView(WaterJsonView.Public.class)
    Folder saveFolder(@RequestBody Folder folder);


    @LoggedIn
    @PutMapping("/folders")
    @JsonView(WaterJsonView.Public.class)
    Folder updateFolder(@RequestBody Folder folder);


    @LoggedIn
    @GetMapping("/folders/{id}")
    @JsonView(WaterJsonView.Public.class)
    Folder findFolder(@PathVariable("id") long id);

    @LoggedIn
    @GetMapping(value = "/content", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @JsonView(WaterJsonView.Public.class)
    void fetchContentStream(@RequestParam("path") String path, @RequestParam("fileName") String fileName);

    @LoggedIn
    @GetMapping(value = "/content/id/{documentId}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @JsonView(WaterJsonView.Public.class)
    void fetchContentStream(@PathVariable("documentId") long documentId);

    @LoggedIn
    @GetMapping(value = "/content/uid/{documentUID}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @JsonView(WaterJsonView.Public.class)
    void fetchContentStream(@PathVariable("documentUID") String documentUID);

    @LoggedIn
    @GetMapping("/folders")
    @JsonView(WaterJsonView.Public.class)
    PaginableResult<Folder> findAllFolders();


    @LoggedIn
    @DeleteMapping("/folders/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @JsonView(WaterJsonView.Public.class)
    void removeFolder(@PathVariable("id") long id);
}
