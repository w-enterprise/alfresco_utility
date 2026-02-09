package it.welf.alfresco.tool.service;

import it.welf.alfresco.tool.model.NodeInfo;
import org.apache.chemistry.opencmis.client.api.*;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.enums.BindingType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CmisService {

    public Session createSession(String url, String username, String password) {
        SessionFactory factory = SessionFactoryImpl.newInstance();
        Map<String, String> parameter = new HashMap<>();

        // User credentials
        parameter.put(SessionParameter.USER, username);
        parameter.put(SessionParameter.PASSWORD, password);

        // Connection settings
        parameter.put(SessionParameter.ATOMPUB_URL, url);
        parameter.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());
        
        // Alfresco specific settings usually not strictly required for basic connection but good practice
        // parameter.put(SessionParameter.OBJECT_FACTORY_CLASS, "org.alfresco.cmis.client.impl.AlfrescoObjectFactoryImpl");

        List<Repository> repositories = factory.getRepositories(parameter);
        if (repositories != null && !repositories.isEmpty()) {
            return repositories.get(0).createSession();
        } else {
            throw new RuntimeException("No repositories found at this URL");
        }
    }

    public NodeInfo getParentNode(Session session, String nodeId) {
        CmisObject object = session.getObject(nodeId);
        
        if (object instanceof FileableCmisObject) {
            FileableCmisObject fileable = (FileableCmisObject) object;
            List<Folder> parents = fileable.getParents();
            
            if (parents != null && !parents.isEmpty()) {
                Folder parent = parents.get(0); // Get the primary parent
                return new NodeInfo(parent.getId(), parent.getName(), parent.getType().getId());
            } else {
                throw new RuntimeException("Node has no parent (it might be the root folder or an unfiled object)");
            }
        } else {
             throw new RuntimeException("Object is not fileable (cannot have a parent folder)");
        }
    }
}
