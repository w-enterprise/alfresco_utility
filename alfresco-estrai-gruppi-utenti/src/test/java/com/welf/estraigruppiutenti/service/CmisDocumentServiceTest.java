package com.welf.estraigruppiutenti.service;

import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CmisDocumentServiceTest {

    @Mock
    private Session session;

    @Mock
    private Folder folder;

    @Mock
    private Document document;

    private CmisDocumentService service;

    @BeforeEach
    void setUp() {
        service = new CmisDocumentService(session);
    }

    @Test
    void testCreateDocument() {
        String parentPath = "/Company Home";
        String name = "test.txt";
        String content = "Hello CMIS";
        String mimeType = "text/plain";

        when(session.getObjectByPath(parentPath)).thenReturn(folder);
        when(folder.createDocument(any(Map.class), any(ContentStream.class), eq(VersioningState.MAJOR))).thenReturn(document);
        when(document.getId()).thenReturn("doc-id-123");

        Document result = service.createDocument(parentPath, name, content, mimeType);

        assertNotNull(result);
        assertEquals("doc-id-123", result.getId());
        verify(folder).createDocument(any(Map.class), any(ContentStream.class), eq(VersioningState.MAJOR));
    }

    @Test
    void testGetDocument() {
        String path = "/Company Home/test.txt";
        when(session.getObjectByPath(path)).thenReturn(document);

        Document result = service.getDocument(path);

        assertNotNull(result);
        verify(session).getObjectByPath(path);
    }

    @Test
    void testUpdateDocumentContent() {
        String docId = "doc-id-123";
        String newContent = "Updated content";
        String mimeType = "text/plain";

        when(session.getObject(docId)).thenReturn(document);
        when(document.getName()).thenReturn("test.txt");

        service.updateDocumentContent(docId, newContent, mimeType);

        verify(document).setContentStream(any(ContentStream.class), eq(true));
    }

    @Test
    void testDeleteDocument() {
        String docId = "doc-id-123";
        when(session.getObject(docId)).thenReturn(document);

        service.deleteDocument(docId);

        verify(document).delete(true);
    }
}
