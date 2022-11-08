package eu.gaiax.difs.fc.core.service.verification.impl;

import eu.gaiax.difs.fc.core.service.filestore.FileStore;
import eu.gaiax.difs.fc.core.service.verification.impl.CachingHttpLoader.HttpDocument;
import eu.gaiax.difs.fc.core.util.HashUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.riot.system.stream.Locator;
import org.apache.jena.riot.system.stream.LocatorHTTP;

/**
 * A Jena Locator that caches http downloads.
 */
@Slf4j
class LocatorCaching implements Locator {

  private final HttpDocumentCache httpCache;
  private final LocatorHTTP locatorHttp = new LocatorHTTP();

  public LocatorCaching(FileStore fileStore) {
    this.httpCache = new HttpDocumentCache(fileStore);
  }

  private HttpDocument getAndCache(String uri) {
    String hash = HashUtils.calculateSha256AsHex(uri);

    HttpDocument cached = httpCache.fetchFromCache(uri, hash);
    if (cached != null) {
      return cached;
    }
    log.debug("Downloading {}", uri);
    TypedInputStream tis = locatorHttp.open(uri);
    if (tis == null) {
      return null;
    }
    try {
      log.debug("Storing content of {} as {}", uri, hash);
      final String remoteBody = IOUtils.toString(tis, StandardCharsets.UTF_8);
      HttpDocument fromRemote = new HttpDocument(tis.getContentType(), tis.getBaseURI(), null, remoteBody);
      httpCache.storeInCache(uri, hash, fromRemote);
      return fromRemote;
    } catch (IOException ex) {
      log.error("Failed to store downloaded content for {}", uri, ex);
      return null;
    }
  }

  @Override
  public TypedInputStream open(String uri) {
    HttpDocument accessor = getAndCache(uri);
    if (accessor == null) {
      return null;
    }
    return new TypedInputStream(
        IOUtils.toInputStream(accessor.getBody(), StandardCharsets.UTF_8),
        ContentType.create(accessor.getType()),
        accessor.getTargetUri());
  }

  @Override
  public String getName() {
    return "LocatorCaching";
  }

}
