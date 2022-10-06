package eu.gaiax.difs.fc.core.service.verification.impl;

import eu.gaiax.difs.fc.core.service.validatorcache.impl.ValidatorCacheImpl;
import eu.gaiax.difs.fc.core.config.DatabaseConfig;
import eu.gaiax.difs.fc.core.pojo.Validator;
import eu.gaiax.difs.fc.core.service.validatorcache.ValidatorCache;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Assertions;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.MethodName.class)
@SpringBootTest
@ActiveProfiles("tests-sdstore")
@ContextConfiguration(classes = {ValidatorCacheTest.TestApplication.class, ValidatorCacheImpl.class, DatabaseConfig.class})
@DirtiesContext
@Transactional
@Slf4j
@AutoConfigureEmbeddedDatabase(provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY)
public class ValidatorCacheTest {

  @SpringBootApplication
  public static class TestApplication {

    public static void main(final String[] args) {
      SpringApplication.run(TestApplication.class, args);
    }
  }

  @Autowired
  private ValidatorCache validatorCache;

  @Autowired
  private SessionFactory sessionFactory;

  @Test
  void test01AddingAndRemoving() throws IOException {
    log.info("test01AddingAndRemoving");
    Validator validator = new Validator("SomeUrl", "Some Text Content", Instant.now());
    validatorCache.addToCache(validator);

    Validator fromCache = validatorCache.getFromCache(validator.getDidURI());
    Assertions.assertEquals(validator, fromCache, "Returned Validator is not the same as the stored Validator");

    validatorCache.removeFromCache(validator.getDidURI());
    fromCache = validatorCache.getFromCache(validator.getDidURI());
    Assertions.assertNull(fromCache, "Validator should have been removed from cache");
  }

  @Test
  void test02Expiration() throws IOException {
    log.info("test02Expiration");
    Validator v1 = new Validator("SomeUrl1", "Some Text Content", Instant.now().minus(1, ChronoUnit.MINUTES));
    validatorCache.addToCache(v1);
    Validator v2 = new Validator("SomeUrl2", "Some Text Content", Instant.now().plus(1, ChronoUnit.MINUTES));
    validatorCache.addToCache(v2);

    Validator fromCache1 = validatorCache.getFromCache(v1.getDidURI());
    Assertions.assertEquals(v1, fromCache1, "Returned Validator is not the same as the stored Validator");
    Validator fromCache2 = validatorCache.getFromCache(v2.getDidURI());
    Assertions.assertEquals(v2, fromCache2, "Returned Validator is not the same as the stored Validator");

    int expired = validatorCache.expireValidators();
    log.info("Expired {} keys", expired);
    Assertions.assertEquals(1, expired, "Incorrect number of keys expired.");

    fromCache1 = validatorCache.getFromCache(v1.getDidURI());
    log.info("Found {}", fromCache1);
    Assertions.assertNull(fromCache1, "Validator should have been removed from cache");
    fromCache2 = validatorCache.getFromCache(v2.getDidURI());
    Assertions.assertEquals(v2, fromCache2, "Validator should not have been removed from the cache");

    validatorCache.removeFromCache(v2.getDidURI());
  }

}
