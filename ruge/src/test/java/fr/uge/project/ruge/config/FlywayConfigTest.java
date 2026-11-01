package fr.uge.project.ruge.config;

import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;

import javax.sql.DataSource;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class FlywayConfigTest {
  static {
    System.setProperty("net.bytebuddy.experimental", "true");
  }

  @Test
  void flywayBeanUsesProvidedDataSourceAndMigrationLocation() {
    DataSource dataSource = mock(DataSource.class);

    Flyway flyway = new FlywayConfig().flyway(dataSource);

    assertThat(flyway).isNotNull();
    assertThat(flyway.getConfiguration().getDataSource()).isSameAs(dataSource);
    assertThat(Arrays.stream(flyway.getConfiguration().getLocations()).map(Object::toString))
        .contains("classpath:db/migration");
  }

  @Test
  void postProcessorAddsFlywayDependencyToEntityManagerFactoryBeans() {
    var beanFactory = new DefaultListableBeanFactory();
    var noDepends = new RootBeanDefinition(EntityManagerFactory.class);
    var hasOtherDepends = new RootBeanDefinition(EntityManagerFactory.class);
    var alreadyDependsOnFlyway = new RootBeanDefinition(EntityManagerFactory.class);

    hasOtherDepends.setDependsOn("anotherBean");
    alreadyDependsOnFlyway.setDependsOn("anotherBean", "flyway");

    beanFactory.registerBeanDefinition("entityManagerFactoryA", noDepends);
    beanFactory.registerBeanDefinition("entityManagerFactoryB", hasOtherDepends);
    beanFactory.registerBeanDefinition("entityManagerFactoryC", alreadyDependsOnFlyway);

    FlywayConfig.flywayDependsOnPostProcessor().postProcessBeanFactory(beanFactory);

    assertThat(beanFactory.getBeanDefinition("entityManagerFactoryA").getDependsOn())
        .containsExactly("flyway");
    assertThat(beanFactory.getBeanDefinition("entityManagerFactoryB").getDependsOn())
        .containsExactly("anotherBean", "flyway");
    assertThat(beanFactory.getBeanDefinition("entityManagerFactoryC").getDependsOn())
        .containsExactly("anotherBean", "flyway");
  }
}
