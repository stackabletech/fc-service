package eu.xfsc.fc.core.service.verification;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.xfsc.fc.core.pojo.ContentAccessor;
import eu.xfsc.fc.core.pojo.ContentAccessorDirect;
import eu.xfsc.fc.core.service.filestore.FileStore;

import java.io.FileNotFoundException;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

/**
 * A wrapper around a file store for storing http documents with some extra information.
 */
@Slf4j
public class HttpDocumentCache {

  private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private final FileStore fileStore;

  public HttpDocumentCache(FileStore fileStore) {
    this.fileStore = fileStore;
  }

  public CachingHttpLoader.HttpDocument fetchFromCache(final String uri, final String hash) {
    try {
      final ContentAccessor cachedContent = fileStore.readFile(hash);
      log.debug("fetchFromCache; Read cached version of {}", uri);
      return OBJECT_MAPPER.readValue(cachedContent.getContentAsStream(), CachingHttpLoader.HttpDocument.class);
    } catch (FileNotFoundException ex) {
      log.debug("fetchFromCache.error 1; No cached version found of {}", uri);
    } catch (IOException ex) {
      log.debug("fetchFromCache.error 2; Error reading cached version found of {}", uri, ex);
    }
    return null;
  }

  public void storeInCache(final String uri, final String hash, CachingHttpLoader.HttpDocument httpDocument) {
    try {
      log.debug("storeInCache; Storing content of {} as {}", uri, hash);
      ContentAccessorDirect content = new ContentAccessorDirect(OBJECT_MAPPER.writeValueAsString(httpDocument));
      fileStore.replaceFile(hash, content);
    } catch (IOException ex) {
      log.error("storeInCache.error; Failed to store downloaded content for {}", uri, ex);
    }
  }

}
