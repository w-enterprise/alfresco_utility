package com.welf.estraigruppiutenti.model;

/**
 * Informazioni essenziali su un gruppo Alfresco.
 */
public class GroupInfo {
    private final String id;
    private final String displayName;
    private final String description;
    private final int directUsersCount;

    /**
     * Crea un nuovo gruppo.
     *
     * @param id               id del gruppo (es. GROUP_xxx).
     * @param displayName      nome visualizzato.
     * @param description      descrizione.
     * @param directUsersCount numero utenti diretti.
     */
    public GroupInfo(String id, String displayName, String description, int directUsersCount) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.directUsersCount = directUsersCount;
    }

    /**
     * @return id del gruppo.
     */
    public String getId() {
        return id;
    }

    /**
     * @return nome visualizzato.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * @return descrizione.
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return numero di utenti diretti.
     */
    public int getDirectUsersCount() {
        return directUsersCount;
    }
}
