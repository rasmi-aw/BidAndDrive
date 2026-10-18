package com.auction.app.config;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.MatchAlwaysTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

/**
 * Wraps framework-free use cases in a transactional proxy, so the domain module needs no {@code @Transactional}.
 */
final class TransactionalProxies {

    private TransactionalProxies() {
    }

    static <T> T readWrite(T useCase, PlatformTransactionManager transactionManager) {
        return proxy(useCase, transactionManager, false);
    }

    static <T> T readOnly(T useCase, PlatformTransactionManager transactionManager) {
        return proxy(useCase, transactionManager, true);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(T useCase, PlatformTransactionManager transactionManager, boolean readOnly) {
        DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();
        attribute.setReadOnly(readOnly);
        MatchAlwaysTransactionAttributeSource source = new MatchAlwaysTransactionAttributeSource();
        source.setTransactionAttribute(attribute);

        ProxyFactory factory = new ProxyFactory(useCase);
        factory.setProxyTargetClass(true);
        factory.addAdvice(new TransactionInterceptor(transactionManager, source));
        return (T) factory.getProxy();
    }
}
