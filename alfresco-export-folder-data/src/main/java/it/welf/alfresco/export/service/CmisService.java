package it.welf.alfresco.export.service;

import it.welf.alfresco.export.model.NodeInfo;
import org.apache.chemistry.opencmis.client.api.*;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.data.Ace;
import org.apache.chemistry.opencmis.commons.data.Acl;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class CmisService {
    private Session session;

    public void connect(String url, String username, String password) throws Exception {
        Map<String, String> parameter = new HashMap<>();
        parameter.put(SessionParameter.USER, username);
        parameter.put(SessionParameter.PASSWORD, password);
        parameter.put(SessionParameter.ATOMPUB_URL, url);
        parameter.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());
        parameter.put(SessionParameter.LOCALE_ISO3166_COUNTRY, "");
        parameter.put(SessionParameter.LOCALE_ISO639_LANGUAGE, "en");

        SessionFactory factory = SessionFactoryImpl.newInstance();
        List<Repository> repositories = factory.getRepositories(parameter);
        if (repositories.isEmpty()) {
            throw new Exception("No repositories found at the specified URL.");
        }
        session = repositories.get(0).createSession();
    }

    public List<NodeInfo> getChildren(String folderId) {
        CmisObject obj = session.getObject(folderId);
        if (obj instanceof Folder) {
            return getChildren((Folder) obj);
        }
        return new ArrayList<>();
    }

    public List<NodeInfo> getChildren(Folder folder) {
        List<NodeInfo> children = new ArrayList<>();
        ItemIterable<CmisObject> objects = folder.getChildren();
        for (CmisObject o : objects) {
            String path = "";
            if (o instanceof FileableCmisObject) {
                List<String> paths = ((FileableCmisObject) o).getPaths();
                if (paths != null && !paths.isEmpty()) {
                    path = paths.get(0);
                }
            }
            children.add(new NodeInfo(o.getId(), o.getName(), o.getType().getId(), path, o instanceof Folder));
        }
        return children;
    }

    public void exportPermissionsRecursive(String rootId, int maxDepth, String csvFile, Consumer<String> logger) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(csvFile))) {
            writer.println("nodeId;path;principalId;permission");
            CmisObject rootObj = session.getObject(rootId);
            processNode(rootObj, 1, maxDepth, writer, logger);
        }
    }

    private void processNode(CmisObject obj, int currentDepth, int maxDepth, PrintWriter writer, Consumer<String> logger) {
        String path = "";
        if (obj instanceof FileableCmisObject) {
            List<String> paths = ((FileableCmisObject) obj).getPaths();
            path = (paths != null && !paths.isEmpty()) ? paths.get(0) : "";
        }

        logger.accept("Processando: " + obj.getName() + " (Profondità: " + currentDepth + ")");

        // Estrai permessi SOLO se è una cartella (cmis:folder)
        if (obj instanceof Folder) {
            try {
                Acl acl = session.getAcl(obj, true);
                if (acl != null && acl.getAces() != null) {
                    for (Ace ace : acl.getAces()) {
                        String principalId = ace.getPrincipalId();
                        for (String perm : ace.getPermissions()) {
                            writer.println(obj.getId() + ";" + path + ";" + principalId + ";" + perm);
                        }
                    }
                    writer.flush(); // Salvataggio progressivo
                }
            } catch (Exception e) {
                logger.accept("Errore permessi per " + obj.getName() + ": " + e.getMessage());
            }
        }

        // Ricorsione se cartella e profondità lo permette
        if (obj instanceof Folder && (maxDepth == 0 || currentDepth < maxDepth)) {
            ItemIterable<CmisObject> children = ((Folder) obj).getChildren();
            for (CmisObject child : children) {
                processNode(child, currentDepth + 1, maxDepth, writer, logger);
            }
        }
    }

    public Session getSession() {
        return session;
    }
}
