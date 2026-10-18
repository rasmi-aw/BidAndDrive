package com.auction.app.config;

import com.auction.application.command.CloseAuctionUseCase;
import com.auction.application.command.CreateAuctionUseCase;
import com.auction.application.command.PlaceBidUseCase;
import com.auction.application.query.GetAuctionUseCase;
import com.auction.application.query.ListAuctionBidsUseCase;
import com.auction.application.query.ListRunningAuctionsUseCase;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class UseCaseConfigTest {

    @Autowired
    private ApplicationContext context;

    static Stream<Arguments> useCases() {
        return Stream.of(
                Arguments.of(CreateAuctionUseCase.class, false),
                Arguments.of(PlaceBidUseCase.class, false),
                Arguments.of(CloseAuctionUseCase.class, false),
                Arguments.of(ListRunningAuctionsUseCase.class, true),
                Arguments.of(GetAuctionUseCase.class, true),
                Arguments.of(ListAuctionBidsUseCase.class, true));
    }

    @ParameterizedTest
    @MethodSource("useCases")
    void everyUseCaseRunsInATransaction(Class<?> useCaseType, boolean readOnly) {
        Object bean = context.getBean(useCaseType);

        assertThat(bean).isInstanceOf(Advised.class);
        TransactionInterceptor interceptor = Arrays.stream(((Advised) bean).getAdvisors())
                .map(advisor -> advisor.getAdvice())
                .filter(TransactionInterceptor.class::isInstance)
                .map(TransactionInterceptor.class::cast)
                .findFirst().orElseThrow();
        Method handle = Arrays.stream(useCaseType.getMethods())
                .filter(m -> m.getName().equals("handle")).findFirst().orElseThrow();

        TransactionAttribute attribute = interceptor.getTransactionAttributeSource()
                .getTransactionAttribute(handle, useCaseType);

        assertThat(attribute).isNotNull();
        assertThat(attribute.isReadOnly()).isEqualTo(readOnly);
        assertThat(attribute.rollbackOn(new RuntimeException())).isTrue();
    }
}
