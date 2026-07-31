package congtuong.dev.cinemabooking.media;

public interface MediaStorageService {

    StoredMedia uploadImage(byte[] content, String publicId);

    void deleteImage(String publicId);
}
