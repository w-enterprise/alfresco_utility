package com.welf.estraigruppiutenti.service;

import org.apache.chemistry.opencmis.client.api.*;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * Servizio per operazioni CRUD su documenti tramite CMIS.
 */
public class CmisDocumentService {
    private static final Logger logger = LoggerFactory.getLogger(CmisDocumentService.class);
    private final Session session;

    public CmisDocumentService(Session session) {
        this.session = session;
    }

    /**
     * Crea un documento in una cartella specifica.
     */
    public Document createDocument(String parentPath, String name, String content, String mimeType) {
        Folder parent = (Folder) session.getObjectByPath(parentPath);
        
        Map<String, Object> properties = new HashMap<>();
        properties.put(PropertyIds.NAME, name);
        properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document");

        byte[] contentBytes = content.getBytes();
        ContentStream contentStream = new ContentStreamImpl(name, BigInteger.valueOf(contentBytes.length), mimeType, new ByteArrayInputStream(contentBytes));

        Document doc = parent.createDocument(properties, contentStream, VersioningState.MAJOR);
        logger.info("Documento creato: {} (ID: {})", name, doc.getId());
        return doc;
    }

    /**
     * Legge un documento dato il path.
     */
    public Document getDocument(String path) {
        CmisObject obj = session.getObjectByPath(path);
        if (obj instanceof Document) {
            return (Document) obj;
        }
        throw new IllegalArgumentException("L'oggetto al path " + path + " non è un documento.");
    }

    /**
     * Aggiorna il contenuto di un documento.
     */
    public void updateDocumentContent(String docId, String newContent, String mimeType) {
        Document doc = (Document) session.getObject(docId);
        byte[] contentBytes = newContent.getBytes();
        ContentStream contentStream = new ContentStreamImpl(doc.getName(), BigInteger.valueOf(contentBytes.length), mimeType, new ByteArrayInputStream(contentBytes));
        
        doc.setContentStream(contentStream, true);
        logger.info("Contenuto documento aggiornato: {}", docId);
    }

    /**
     * Elimina un documento.
     */
    public void deleteDocument(String docId) {
        Document doc = (Document) session.getObject(docId);
        doc.delete(true);
        logger.info("Documento eliminato: {}", docId);
    }
}
