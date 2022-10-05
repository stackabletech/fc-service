package eu.gaiax.difs.fc.core.config;

import eu.gaiax.difs.fc.core.service.filestore.FileStore;
import eu.gaiax.difs.fc.core.service.filestore.impl.FileStoreImpl;
import java.io.File;
import org.assertj.core.util.Files;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FileStoreConfig {

  @Value("${federated-catalogue.scope}")
  private String scope;

  @Value("${federated-catalogue.file-store.sd.location}")
  private String sdFilesLocation;

  @Value("${federated-catalogue.file-store.schema.location}")
  private String schemaFilesLocation;

  private final String TEMPORARY_FOLDER_PATH = Files.temporaryFolderPath() + "federated-catalogue" + File.separator + "test";

  @Bean
  public FileStore sdFileStore() {
    if (scope.equals("test")) {
      return new FileStoreImpl(TEMPORARY_FOLDER_PATH);
    }
    return new FileStoreImpl(sdFilesLocation);
  }

  @Bean
  public FileStore schemaFileStore() {
    if (scope.equals("test")) {
      return new FileStoreImpl(TEMPORARY_FOLDER_PATH);
    }
    return new FileStoreImpl(schemaFilesLocation);
  }
}
