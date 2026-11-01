package fr.uge.project.ruge.config;

import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.Arrays;

@Configuration
public class FlywayConfig {

  @Bean(initMethod = "migrate")
  public Flyway flyway(DataSource dataSource) {
    return Flyway.configure()
      .dataSource(dataSource)
      .locations("classpath:db/migration")
      .load();
  }

  @Bean
  public static BeanFactoryPostProcessor flywayDependsOnPostProcessor() {
    return beanFactory -> {
      var names = beanFactory.getBeanNamesForType(EntityManagerFactory.class, true, false);
      for (var name : names) {
        var beanDefinition = beanFactory.getBeanDefinition(name);
        var dependsOn = beanDefinition.getDependsOn();
        if (dependsOn == null) {
          beanDefinition.setDependsOn("flyway");
        } else if (Arrays.stream(dependsOn).noneMatch("flyway"::equals)) {
          var updated = Arrays.copyOf(dependsOn, dependsOn.length + 1);
          updated[dependsOn.length] = "flyway";
          beanDefinition.setDependsOn(updated);
        }
      }
    };
  }
}
