package model;

public class Folder {
    public String folderId;
    public String name;
    public String ownerId;
    public boolean isPublic;
    public long createdAt;
    public boolean hasAccess = false;
    public String accessType = "";
    public long expiresAt = 0;
    public boolean isPending = false;
}