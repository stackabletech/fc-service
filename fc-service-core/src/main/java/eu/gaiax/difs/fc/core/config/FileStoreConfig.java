package eu.gaiax.difs.fc.core.config;

import eu.gaiax.difs.fc.core.service.filestore.FileStore;
import eu.gaiax.difs.fc.core.service.filestore.impl.FileStoreImpl;
import java.io.File;
//import org.assertj.core.util.Files;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.io.Files;

@Configuration
public class FileStoreConfig {

  @Value("${federated-catalogue.scope}")
  private String scope;

  @Value("${federated-catalogue.file-store.schema.location}")
  private String schemaFilesLocation;

  @Value("${federated-catalogue.file-store.context-cache.location}")
  private String contextCacheFilesLocation;

  private final File TEMPORARY_FOLDER_FILE = Files.createTempDir();  

  @Bean
  public FileStore schemaFileStore() {
    if (scope.equals("runtime")) {
      return new FileStoreImpl(schemaFilesLocation);
    }
    String TEMPORARY_FOLDER_PATH_SCHEMA = TEMPORARY_FOLDER_FILE.getAbsolutePath() + File.separator + "testSchemaFiles";	
    return new FileStoreImpl(TEMPORARY_FOLDER_PATH_SCHEMA);
  }

  @Bean
  public FileStore contextCacheFileStore() {
    if (scope.equals("runtime")) {
      return new FileStoreImpl(contextCacheFilesLocation);
    }
    String TEMPORARY_FOLDER_PATH_CC = TEMPORARY_FOLDER_FILE.getAbsolutePath() + File.separator + "testContextCache";
    return new FileStoreImpl(TEMPORARY_FOLDER_PATH_CC);
  }
}
