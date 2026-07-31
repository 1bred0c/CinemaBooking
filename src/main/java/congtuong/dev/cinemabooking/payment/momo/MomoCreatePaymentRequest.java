package congtuong.dev.cinemabooking.payment.momo;

public record MomoCreatePaymentRequest(
        String partnerCode,
        String partnerName,
        String storeId,
        String requestId,
        long amount,
        String orderId,
        String orderInfo,
        String redirectUrl,
        String ipnUrl,
        String lang,
        String requestType,
        boolean autoCapture,
        String extraData,
        String signature
) {
}
