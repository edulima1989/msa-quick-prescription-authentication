package com.quickprescription.authentication.config;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "app.flyway", name = "force-on-startup", havingValue = "true", matchIfMissing = true)
public class FlywayStartupConfig {

    @Bean(name = "flyway", initMethod = "migrate")
    @ConditionalOnMissingBean(Flyway.class)
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .defaultSchema("public")
                .schemas("public")
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .validateOnMigrate(true)
                .load();
    }

    @Bean
    public static BeanFactoryPostProcessor entityManagerDependsOnFlywayPostProcessor() {
        return (ConfigurableListableBeanFactory beanFactory) -> {
            if (!beanFactory.containsBeanDefinition("entityManagerFactory")
                    || !beanFactory.containsBeanDefinition("flyway")) {
                return;
            }

            String[] existingDependencies =
                    beanFactory.getBeanDefinition("entityManagerFactory").getDependsOn();

            if (existingDependencies == null || existingDependencies.length == 0) {
                beanFactory.getBeanDefinition("entityManagerFactory").setDependsOn("flyway");
                return;
            }

            for (String dependency : existingDependencies) {
                if ("flyway".equals(dependency)) {
                    return;
                }
            }

            String[] updatedDependencies = new String[existingDependencies.length + 1];
            System.arraycopy(existingDependencies, 0, updatedDependencies, 0, existingDependencies.length);
            updatedDependencies[existingDependencies.length] = "flyway";
            beanFactory.getBeanDefinition("entityManagerFactory").setDependsOn(updatedDependencies);
        };
    }
}
