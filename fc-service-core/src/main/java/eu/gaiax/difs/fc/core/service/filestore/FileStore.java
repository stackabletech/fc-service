package eu.gaiax.difs.fc.core.service.filestore;

import eu.gaiax.difs.fc.core.pojo.ContentAccessor;
import java.io.File;
import java.io.IOException;

public interface FileStore {
  void storeFile(String hash, ContentAccessor content) throws IOException;

  void replaceFile(String hash, ContentAccessor content) throws IOException;

  ContentAccessor readFile(String hash) throws IOException;

  void deleteFile(String hash) throws IOException;

  Iterable<File> getFileIterable();

  void clearStorage() throws IOException;
}