package eu.gaiax.difs.fc.core.config;

import java.util.Properties;
import javax.sql.DataSource;

import org.hibernate.SessionFactory;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;

/**
 * Class organizing the database connection. TODO: move to utils
 */
@Configuration //(proxyBeanMethods=false)
@EnableTransactionManagement
public class DatabaseConfig {

  @Autowired
  DataSource dataSource;
  
  //@PersistenceUnit
  //private EntityManagerFactory entityManagerFactory;

  /**
   * Create the session Factory bean.
   *
   * @return The sessionFactory bean
   */
  //@Bean
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
  
  @Bean
  public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
      LocalContainerEntityManagerFactoryBean entityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
      entityManagerFactoryBean.setPersistenceUnitName(this.getClass().getSimpleName());
      entityManagerFactoryBean.setPersistenceProvider(new HibernatePersistenceProvider());
      entityManagerFactoryBean.setDataSource(dataSource);
      entityManagerFactoryBean.setPackagesToScan(
       "eu.gaiax.difs.fc.core.service.sdstore.impl",
        "eu.gaiax.difs.fc.core.service.schemastore.impl"
      );

      JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
      entityManagerFactoryBean.setJpaVendorAdapter(vendorAdapter);
      entityManagerFactoryBean.setJpaProperties(hibernateProperties());
      return entityManagerFactoryBean;
  }
/*  
  @Bean
  public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
      //JpaVendorAdapteradapter can be autowired as well if it's configured in application properties.
      HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
      vendorAdapter.setGenerateDdl(false);

      LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
      factory.setJpaVendorAdapter(vendorAdapter);
      //Add package to scan for entities.
      factory.setPackagesToScan(
          "eu.gaiax.difs.fc.core.service.sdstore.impl",
          "eu.gaiax.difs.fc.core.service.schemastore.impl"
      );
      factory.setDataSource(dataSource);
      return factory;
  }  
*/
  /**
   * Create the transaction manager.
   *
   * @return the transaction manager.
   */
  //@Bean
  public PlatformTransactionManager hibernateTransactionManager() {
    HibernateTransactionManager transactionManager = new HibernateTransactionManager();
    //SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
    EntityManagerFactory entityManagerFactory = entityManagerFactory().getObject();
    SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
    transactionManager.setSessionFactory(sessionFactory); //.getObject());
    //SpringSessionContext ctx;
    return transactionManager;
  }
  
  
  @Bean
  public JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
      JpaTransactionManager transactionManager = new JpaTransactionManager();
      transactionManager.setEntityManagerFactory(entityManagerFactory);
      return transactionManager;
  }

  @Bean
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
