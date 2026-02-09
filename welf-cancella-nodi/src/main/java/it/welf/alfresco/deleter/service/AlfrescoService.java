package it.welf.alfresco.deleter.service;

import it.welf.alfresco.deleter.model.NodeInfo;
import org.apache.chemistry.opencmis.client.api.*;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisPermissionDeniedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AlfrescoService {
    private static final Logger logger = LoggerFactory.getLogger(AlfrescoService.class);
    private Session session;

    public void connect(String url, String username, String password) {
        Map<String, String> parameter = new HashMap<>();
        parameter.put(SessionParameter.USER, username);
        parameter.put(SessionParameter.PASSWORD, password);
        parameter.put(SessionParameter.ATOMPUB_URL, url); // Assuming AtomPub, can switch to Browser binding if URL suggests
        parameter.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());
        
        // Auto-detect binding based on URL
        if (url.contains("/browser")) {
             parameter.put(SessionParameter.BROWSER_URL, url);
             parameter.put(SessionParameter.BINDING_TYPE, BindingType.BROWSER.value());
             parameter.remove(SessionParameter.ATOMPUB_URL);
        }

        // Remove manual ObjectFactory specification to let OpenCMIS use the default one
        // parameter.put(SessionParameter.OBJECT_FACTORY_CLASS, "org.apache.chemistry.opencmis.client.impl.repository.ObjectFactoryImpl");

        SessionFactory factory = SessionFactoryImpl.newInstance();
        List<Repository> repositories = factory.getRepositories(parameter);
        
        if (repositories != null && !repositories.isEmpty()) {
            this.session = repositories.get(0).createSession();
            logger.info("Connected to repository: " + session.getRepositoryInfo().getName());
        } else {
            throw new RuntimeException("No repositories found at the given URL.");
        }
    }

    public boolean isConnected() {
        return session != null;
    }

    public NodeInfo checkNode(String nodeId) {
        String cleanId = normalizeId(nodeId);
        try {
            CmisObject obj = session.getObject(cleanId);
            return new NodeInfo(nodeId, obj.getName(), "FOUND", "Node exists");
        } catch (CmisObjectNotFoundException e) {
            return new NodeInfo(nodeId, "N/A", "NOT_FOUND", "Node not found");
        } catch (Exception e) {
            return new NodeInfo(nodeId, "N/A", "ERROR", e.getMessage());
        }
    }

    public NodeInfo deleteNode(String nodeId) {
        String cleanId = normalizeId(nodeId);
        try {
            CmisObject obj = session.getObject(cleanId);
            String name = obj.getName();
            obj.delete(true); // Delete all versions
            return new NodeInfo(nodeId, name, "DELETED", "Successfully deleted");
        } catch (CmisObjectNotFoundException e) {
            return new NodeInfo(nodeId, "N/A", "NOT_FOUND", "Node not found");
        } catch (CmisPermissionDeniedException e) {
            return new NodeInfo(nodeId, "N/A", "ERROR", "Permission denied");
        } catch (Exception e) {
            return new NodeInfo(nodeId, "N/A", "ERROR", e.getMessage());
        }
    }

    private String normalizeId(String nodeId) {
        if (nodeId == null) return "";
        if (nodeId.startsWith("workspace://SpacesStore/")) {
            return nodeId.replace("workspace://SpacesStore/", "");
        }
        return nodeId;
    }
}
