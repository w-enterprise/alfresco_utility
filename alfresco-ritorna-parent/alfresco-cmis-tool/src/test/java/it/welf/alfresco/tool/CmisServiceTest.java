package it.welf.alfresco.tool;

import it.welf.alfresco.tool.model.NodeInfo;
import it.welf.alfresco.tool.service.CmisService;
import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.FileableCmisObject;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.Session;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CmisServiceTest {

    @Test
    void testGetParentNode() {
        // Mock dependencies
        Session session = mock(Session.class);
        FileableCmisObject childNode = mock(FileableCmisObject.class);
        Folder parentNode = mock(Folder.class);
        ObjectType type = mock(ObjectType.class);

        String childId = "123";
        String parentId = "456";
        String parentName = "ParentFolder";

        // Define behavior
        when(session.getObject(childId)).thenReturn(childNode);
        when(childNode.getParents()).thenReturn(Collections.singletonList(parentNode));
        when(parentNode.getId()).thenReturn(parentId);
        when(parentNode.getName()).thenReturn(parentName);
        when(parentNode.getType()).thenReturn(type);
        when(type.getId()).thenReturn("cmis:folder");

        // Execute
        CmisService service = new CmisService();
        NodeInfo result = service.getParentNode(session, childId);

        // Verify
        assertNotNull(result);
        assertEquals(parentId, result.getId());
        assertEquals(parentName, result.getName());
        assertEquals("cmis:folder", result.getType());
    }
}
