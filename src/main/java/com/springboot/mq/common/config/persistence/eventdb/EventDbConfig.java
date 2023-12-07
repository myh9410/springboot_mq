package com.springboot.mq.common.config.persistence.eventdb;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jta.atomikos.AtomikosDataSourceBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import javax.sql.DataSource;

@EnableJpaRepositories(
        basePackages = {"com.springboot.mq.domains.repository.test","com.springboot.mq.domains.repository.callback"},
        entityManagerFactoryRef = "testEntityManager",
        transactionManagerRef = "jtaTransactionManager"
)
@Configuration
public class EventDbConfig {

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.event")
    public DataSource testDataSource() {
        return new AtomikosDataSourceBean();
    }

    @Bean("testEntityManager")
    public LocalContainerEntityManagerFactoryBean testEntityManager(
            JpaVendorAdapter jpaVendorAdapter
    ) {
        LocalContainerEntityManagerFactoryBean entityManager = new LocalContainerEntityManagerFactoryBean();

        entityManager.setJtaDataSource(testDataSource());
        entityManager.setJpaVendorAdapter(jpaVendorAdapter);
        entityManager.setPackagesToScan("com.springboot.mq.domains.domain");
        entityManager.setPersistenceUnitName("testEntityManager");

        return entityManager;
    }

    @Bean("callbackEntityManager")
    public LocalContainerEntityManagerFactoryBean callbackEntityManager(
            JpaVendorAdapter jpaVendorAdapter
    ) {
        LocalContainerEntityManagerFactoryBean entityManager = new LocalContainerEntityManagerFactoryBean();

        entityManager.setJtaDataSource(testDataSource());
        entityManager.setJpaVendorAdapter(jpaVendorAdapter);
        entityManager.setPackagesToScan("com.springboot.mq.domains.domain");
        entityManager.setPersistenceUnitName("callbackEntityManager");

        return entityManager;
    }

}
