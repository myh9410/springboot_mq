package com.springboot.mq.common.config.persistence.customerdb;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jta.atomikos.AtomikosDataSourceBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import javax.sql.DataSource;

@EnableJpaRepositories(
        basePackages = "com.springboot.mq.domains.repository.customer.user",
        entityManagerFactoryRef = "customerEntityManager",
        transactionManagerRef = "jtaTransactionManager"
)
@Configuration
public class CustomerDbConfig {

    @Primary
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.customer")
    public DataSource customerDataSource() {
        return new AtomikosDataSourceBean();
    }

    @Bean("customerEntityManager")
    @Primary
    public LocalContainerEntityManagerFactoryBean customerEntityManager(
            JpaVendorAdapter jpaVendorAdapter
    ) {
        LocalContainerEntityManagerFactoryBean entityManager = new LocalContainerEntityManagerFactoryBean();

        entityManager.setJtaDataSource(customerDataSource());
        entityManager.setJpaVendorAdapter(jpaVendorAdapter);
        entityManager.setPackagesToScan("com.springboot.mq.domains.domain");
        entityManager.setPersistenceUnitName("customerEntityManager");

        return entityManager;
    }

}
