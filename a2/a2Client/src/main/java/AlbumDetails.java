public class AlbumDetails {
    private String AlbumID;
    private String ImageSize;

    public AlbumDetails(String albumID, String imageSize) {
        AlbumID = albumID;
        ImageSize = imageSize;
    }

    public AlbumDetails() {
    }

    public String getAlbumID() {
        return AlbumID;
    }

    public String getImageSize() {
        return ImageSize;
    }

    public void setAlbumID(String albumID) {
        AlbumID = albumID;
    }

    public void setImageSize(String imageSize) {
        ImageSize = imageSize;
    }
}
