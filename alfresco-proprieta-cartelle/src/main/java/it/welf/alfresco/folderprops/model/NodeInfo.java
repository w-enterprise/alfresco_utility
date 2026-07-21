package it.welf.alfresco.folderprops.model;

public class NodeInfo {
    private final String id;
    private final String name;
    private final String type;
    private final String path;
    private final boolean folder;
    private boolean selected;

    public NodeInfo(String id, String name, String type, String path, boolean folder) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.path = path;
        this.folder = folder;
        this.selected = false;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getPath() { return path; }
    public boolean isFolder() { return folder; }
    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }
}
