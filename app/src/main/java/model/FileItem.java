package model;

public class FileItem {
    public String folderid;
    public String name;
    public String url;
    public String size;
    public long uploadedAt;

    public FileItem() {
    }

    public FileItem(String folderid,String name,String url, String size, long uploadedAt) {

        this.url = url;
        this.folderid = folderid;
        this.name = name;
        this.size = size;
        this.uploadedAt = uploadedAt;
    }
}
