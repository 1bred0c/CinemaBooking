package congtuong.dev.cinemabooking.payment.vnpay;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payment.vnpay")
public record VnPayProperties(
        String tmnCode,
        String hashSecret,
        String url,
        String returnUrl,
        String ipnUrl,
        String version,
        String command,
        String orderType,
        String locale
) {
}
