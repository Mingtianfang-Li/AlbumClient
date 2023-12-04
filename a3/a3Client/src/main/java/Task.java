public class Task {
    String AlbumID;
    AlbumDetails albumDetails;

    public Task(AlbumDetails albumDetails) {
        this.albumDetails = albumDetails;
        this.AlbumID = albumDetails.getAlbumID();
    }

    public String getAlbumID() {
        return AlbumID;
    }

    public AlbumDetails getAlbumDetails() {
        return albumDetails;
    }
}
