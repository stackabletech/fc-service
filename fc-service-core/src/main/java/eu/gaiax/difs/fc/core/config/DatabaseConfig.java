package eu.gaiax.difs.fc.core.config;

import java.util.Properties;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Class organizing the database connection. TODO: move to utils
 */
@Configuration
@EnableTransactionManagement
public class DatabaseConfig {

  @Autowired
  DataSource dataSource;

  /**
   * Create the session Factory bean.
   *
   * @return The sessionFactory bean
   */
  @Bean
  public LocalSessionFactoryBean sessionFactory() {
    LocalSessionFactoryBean sessionFactory = new LocalSessionFactoryBean();
    sessionFactory.setDataSource(dataSource);
    sessionFactory.setPackagesToScan(
            "eu.gaiax.difs.fc.core.service.sdstore.impl",
            "eu.gaiax.difs.fc.core.service.schemastore.impl"
    );
    sessionFactory.setHibernateProperties(hibernateProperties());
    return sessionFactory;
  }

  /**
   * Create the transaction manager.
   *
   * @return the transaction manager.
   */
  @Bean
  public PlatformTransactionManager hibernateTransactionManager() {
    HibernateTransactionManager transactionManager = new HibernateTransactionManager();
    transactionManager.setSessionFactory(sessionFactory().getObject());
    return transactionManager;
  }

  private Properties hibernateProperties() {
    Properties hibernateProperties = new Properties();
    hibernateProperties.setProperty("hibernate.hbm2ddl.auto", "validate");
    return hibernateProperties;
  }
}
