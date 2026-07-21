package it.welf.alfresco.folderprops.model;

public class ReportItem {
    private final String category;
    private final String nodeId;
    private final String name;
    private final String path;
    private final String mimeType;
    private final String documentType;
    private final String lastModified;
    private final long totalSizeBytes;

    public ReportItem(String category, String nodeId, String name, String path, String mimeType, String documentType, String lastModified) {
        this(category, nodeId, name, path, mimeType, documentType, lastModified, -1L);
    }

    public ReportItem(String category, String nodeId, String name, String path, String mimeType, String documentType, String lastModified, long totalSizeBytes) {
        this.category = category;
        this.nodeId = nodeId;
        this.name = name;
        this.path = path;
        this.mimeType = mimeType;
        this.documentType = documentType;
        this.lastModified = lastModified;
        this.totalSizeBytes = totalSizeBytes;
    }

    public String getCategory() {
        return category;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getDocumentType() {
        return documentType;
    }

    public String getLastModified() {
        return lastModified;
    }

    public long getTotalSizeBytes() {
        return totalSizeBytes;
    }
}
