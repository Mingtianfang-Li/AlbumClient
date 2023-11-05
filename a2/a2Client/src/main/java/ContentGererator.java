import java.util.Random;

public class ContentGererator {
    final static private int maxAlbumNums = 10000;

    final static private int maxImageSize = 1000;
    final static private Random r = new Random();

    public static AlbumDetails createBody(){
        return getAlbumDetails();
    }
    private static AlbumDetails getAlbumDetails(){
        AlbumDetails body = new AlbumDetails();
        int albumID = 1 + r.nextInt(maxAlbumNums);
        body.setAlbumID(String.valueOf(albumID));
        int imageSize = 1 + r.nextInt(maxImageSize);
        body.setImageSize(String.valueOf(imageSize));
        return body;
    }
}
