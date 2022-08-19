package eu.gaiax.difs.fc.core.config;

import java.util.Properties;
import javax.sql.DataSource;
import org.apache.tomcat.dbcp.dbcp2.BasicDataSource;
import org.springframework.beans.factory.annotation.Value;
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

  @Value("${datastore.database-driver}")
  private String databaseDriver;
  @Value("${datastore.database-url}")
  private String databaseUrl;
  @Value("${datastore.database-username}")
  private String databaseUser;
  @Value("${datastore.database-password}")
  private String databasePass;

  /**
   * Create the session Factory bean.
   *
   * @return The sessionFactory bean
   */
  @Bean
  public LocalSessionFactoryBean sessionFactory() {
    LocalSessionFactoryBean sessionFactory = new LocalSessionFactoryBean();
    sessionFactory.setDataSource(dataSource());
    sessionFactory.setPackagesToScan(
            "eu.gaiax.difs.fc.core.service.sdstore.impl"
    );
    sessionFactory.setHibernateProperties(hibernateProperties());
    return sessionFactory;
  }

  /**
   * Create the Data Source to the database.
   *
   * @return the Data Source to the database.
   */
  @Bean
  public DataSource dataSource() {
    BasicDataSource dataSource = new BasicDataSource();
    dataSource.setDriverClassName(databaseDriver);
    dataSource.setUrl(databaseUrl);
    dataSource.setUsername(databaseUser);
    dataSource.setPassword(databasePass);
    return dataSource;
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
