package com.welf.estraigruppiutenti.service;

import com.welf.estraigruppiutenti.model.CmisConnectionException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisUnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CmisConnectionServiceTest {

    private CmisConnectionService service;

    @BeforeEach
    void setUp() {
        service = new CmisConnectionService();
    }

    @Test
    void testCreateSessionWithEmptyUrl() {
        CmisConnectionException ex = assertThrows(CmisConnectionException.class, () -> {
            service.createSession("", "admin", "admin");
        });
        assertTrue(ex.getMessage().contains("URL CMIS mancante"));
    }

    @Test
    void testCreateSessionWithEmptyUsername() {
        CmisConnectionException ex = assertThrows(CmisConnectionException.class, () -> {
            service.createSession("http://localhost:8080/alfresco/api/-default-/public/cmis/versions/1.1/browser", "", "admin");
        });
        assertTrue(ex.getMessage().contains("Username mancante"));
    }
}
