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

  @Value("${federated-catalogue.file-store.sd.location}")
  private String sdFilesLocation;

  @Value("${federated-catalogue.file-store.schema.location}")
  private String schemaFilesLocation;

  @Value("${federated-catalogue.file-store.context-cache.location}")
  private String contextCacheFilesLocation;

  private final File TEMPORARY_FOLDER_FILE = Files.createTempDir(); // Files.newTemporaryFolder();
  private final String TEMPORARY_FOLDER_PATH_SD = TEMPORARY_FOLDER_FILE.getAbsolutePath() + File.separator + "testSdFiles";
  private final String TEMPORARY_FOLDER_PATH_SCHEMA = TEMPORARY_FOLDER_FILE.getAbsolutePath() + File.separator + "testSchemaFiles"; 
  private final String TEMPORARY_FOLDER_PATH_CC = TEMPORARY_FOLDER_FILE.getAbsolutePath() + File.separator + "testContextCache";

  @Bean
  public FileStore sdFileStore() {
    if (scope.equals("test")) {
      return new FileStoreImpl(TEMPORARY_FOLDER_PATH_SD);
    }
    return new FileStoreImpl(sdFilesLocation);
  }

  @Bean
  public FileStore schemaFileStore() {
    if (scope.equals("test")) {
      return new FileStoreImpl(TEMPORARY_FOLDER_PATH_SCHEMA);
    }
    return new FileStoreImpl(schemaFilesLocation);
  }

  @Bean
  public FileStore contextCacheFileStore() {
    if (scope.equals("test")) {
      return new FileStoreImpl(TEMPORARY_FOLDER_PATH_CC);
    }
    return new FileStoreImpl(contextCacheFilesLocation);
  }
}
