package com.springboot.mq.common.config.persistence.boarddb;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jta.atomikos.AtomikosDataSourceBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import javax.sql.DataSource;

@EnableJpaRepositories(
        basePackages = "com.springboot.mq.domains.repository.board",
        entityManagerFactoryRef = "boardEntityManager",
        transactionManagerRef = "jtaTransactionManager"
)
@Configuration
public class BoardDbConfig {

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.boards")
    public DataSource boardDataSource() {
        return new AtomikosDataSourceBean();
    }

    @Bean(name = "boardEntityManager")
    public LocalContainerEntityManagerFactoryBean boardEntityManager(
            JpaVendorAdapter jpaVendorAdapter
    ) {
        LocalContainerEntityManagerFactoryBean entityManager = new LocalContainerEntityManagerFactoryBean();

        entityManager.setJtaDataSource(boardDataSource());
        entityManager.setJpaVendorAdapter(jpaVendorAdapter);
        entityManager.setPackagesToScan("com.springboot.mq.domains.domain");
        entityManager.setPersistenceUnitName("boardEntityManager");

        return entityManager;
    }

}
