package congtuong.dev.cinemabooking.payment.momo;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "payment.momo")
public class MomoProperties {
    private String endpoint;
    private String partnerCode;
    private String accessKey;
    private String secretKey;
    private String redirectUrl;
    private String ipnUrl;
    private String requestType = "captureWallet";
    private String lang = "vi";
    private String partnerName = "CinemaBooking";
    private String storeId = "CinemaBooking";
    private int timeoutSeconds = 30;
}
