package it.welf.alfresco.deleter.model;

public class NodeInfo {
    private String id;
    private String name;
    private String status; // "FOUND", "NOT_FOUND", "DELETED", "ERROR"
    private String message;

    public NodeInfo(String id, String name, String status, String message) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.message = message;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getStatus() { return status; }
    public String getMessage() { return message; }
    
    public void setStatus(String status) { this.status = status; }
    public void setMessage(String message) { this.message = message; }
    
    @Override
    public String toString() {
        return String.format("%s (%s) - %s: %s", id, name, status, message);
    }
}
