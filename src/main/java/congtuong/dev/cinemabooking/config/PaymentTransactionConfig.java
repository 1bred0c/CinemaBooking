package congtuong.dev.cinemabooking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class PaymentTransactionConfig {

    @Bean
    public TransactionTemplate paymentTransactionTemplate(
            PlatformTransactionManager transactionManager
    ) {
        return new TransactionTemplate(transactionManager);
    }
}
