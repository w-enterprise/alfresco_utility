package it.welf.alfresco.export.model;

public class NodeInfo {
    private String id;
    private String name;
    private String type;
    private String path;
    private boolean isFolder;
    private boolean selected = false;

    public NodeInfo(String id, String name, String type, String path, boolean isFolder) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.path = path;
        this.isFolder = isFolder;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getPath() {
        return path;
    }

    public boolean isFolder() {
        return isFolder;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public String toString() {
        return name + " [" + id + "]";
    }
}
