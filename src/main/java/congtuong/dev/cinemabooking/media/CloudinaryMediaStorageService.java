package congtuong.dev.cinemabooking.media;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import congtuong.dev.cinemabooking.config.CloudinaryProperties;
import congtuong.dev.cinemabooking.exception.MediaException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryMediaStorageService implements MediaStorageService {

    private final Cloudinary cloudinary;
    private final CloudinaryProperties properties;

    @Override
    public StoredMedia uploadImage(byte[] content, String publicId) {
        validateConfiguration();
        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    content,
                    ObjectUtils.asMap(
                            "public_id", publicId,
                            "resource_type", "image",
                            "overwrite", true,
                            "invalidate", true
                    )
            );
            return new StoredMedia(
                    requiredResult(result, "public_id"),
                    requiredResult(result, "secure_url")
            );
        } catch (IOException exception) {
            throw providerFailure("Unable to upload poster", exception);
        }
    }

    @Override
    public void deleteImage(String publicId) {
        validateConfiguration();
        try {
            cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.asMap(
                            "resource_type", "image",
                            "invalidate", true
                    )
            );
        } catch (IOException exception) {
            throw providerFailure("Unable to delete poster", exception);
        }
    }

    private void validateConfiguration() {
        if (isBlank(properties.cloudName())
                || isBlank(properties.apiKey())
                || isBlank(properties.apiSecret())) {
            throw new MediaException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Cloudinary is not configured"
            );
        }
    }

    private String requiredResult(Map<?, ?> result, String key) {
        Object value = result.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new MediaException(
                    HttpStatus.BAD_GATEWAY,
                    "Cloudinary returned an invalid response"
            );
        }
        return value.toString();
    }

    private MediaException providerFailure(
            String message,
            IOException exception
    ) {
        return new MediaException(
                HttpStatus.BAD_GATEWAY,
                message + ": " + exception.getMessage()
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
