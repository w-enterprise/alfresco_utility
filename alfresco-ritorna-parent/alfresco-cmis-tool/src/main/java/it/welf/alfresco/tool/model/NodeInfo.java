package it.welf.alfresco.tool.model;

public class NodeInfo {
    private String id;
    private String name;
    private String type;

    public NodeInfo(String id, String name, String type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }

    @Override
    public String toString() {
        return String.format("ID: %s\nName: %s\nType: %s", id, name, type);
    }
}
