package eu.gaiax.difs.fc.core.config;

import java.util.Properties;

import javax.sql.DataSource;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManagerFactory;

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
  @Bean(name="entityManagerFactory")
  public LocalSessionFactoryBean sessionFactory() {
    LocalSessionFactoryBean sessionFactory = new LocalSessionFactoryBean();
    sessionFactory.setDataSource(dataSource);
    sessionFactory.setMappingResources("mappings/validators.hbm.xml");
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
  public PlatformTransactionManager hibernateTransactionManager(SessionFactory sessionFactory) {
    HibernateTransactionManager transactionManager = new HibernateTransactionManager();
    //SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
    //EntityManagerFactory entityManagerFactory = entityManagerFactory().getObject();
    //SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
    transactionManager.setSessionFactory(sessionFactory); //.getObject());
    return transactionManager;
  }
  
  
  //@Bean
  public JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
      JpaTransactionManager transactionManager = new JpaTransactionManager();
      transactionManager.setEntityManagerFactory(entityManagerFactory);
      return transactionManager;
  }

  //@Bean
  public TransactionTemplate transactionTemplate(EntityManagerFactory entityManagerFactory) {
      return new TransactionTemplate(transactionManager(entityManagerFactory));
  }  

  private Properties hibernateProperties() {
    Properties hibernateProperties = new Properties();
    hibernateProperties.setProperty("hibernate.hbm2ddl.auto", "validate");
    //hibernateProperties.setProperty("hibernate.dialect", ArrayPostgresSQLDialect.class.getCanonicalName());
    return hibernateProperties;
  }

}
