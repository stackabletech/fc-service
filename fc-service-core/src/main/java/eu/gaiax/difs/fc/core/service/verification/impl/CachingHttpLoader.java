package eu.gaiax.difs.fc.core.service.verification.impl;

import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.JsonLdErrorCode;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.document.JsonDocument;
import com.apicatalog.jsonld.document.RdfDocument;
import com.apicatalog.jsonld.http.DefaultHttpClient;
import com.apicatalog.jsonld.http.HttpClient;
import com.apicatalog.jsonld.http.HttpResponse;
import com.apicatalog.jsonld.http.ProfileConstants;
import com.apicatalog.jsonld.http.link.Link;
import com.apicatalog.jsonld.http.media.MediaType;
import com.apicatalog.jsonld.loader.DocumentLoader;
import com.apicatalog.jsonld.loader.DocumentLoaderOptions;
import com.apicatalog.jsonld.loader.HttpLoader;
import com.apicatalog.jsonld.uri.UriResolver;
import eu.gaiax.difs.fc.core.service.filestore.FileStore;
import eu.gaiax.difs.fc.core.util.HashUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

/**
 * A com.​apicatalog.​jsonld DocumentLoader that caches fetched documents.
 */
@Slf4j
class CachingHttpLoader implements DocumentLoader {

  public static final int MAX_REDIRECTIONS = 10;

  private static final String PLUS_JSON = "+json";

  private final HttpClient httpClient;
  private final HttpDocumentCache httpCache;

  public CachingHttpLoader(final FileStore fileStore) {
    this(fileStore, DefaultHttpClient.defaultInstance());
  }

  public CachingHttpLoader(final FileStore fileStore, HttpClient httpClient) {
    this.httpCache = new HttpDocumentCache(fileStore);
    this.httpClient = httpClient;
  }

  private HttpDocument fetchRemoteDocument(final URI uri, final DocumentLoaderOptions options) throws JsonLdError {
    try {
      URI targetUri = uri;
      MediaType contentType = null;
      URI contextUri = null;
      for (int redirection = 0; redirection < MAX_REDIRECTIONS; redirection++) {
        try ( HttpResponse response = httpClient.send(targetUri, HttpLoader.getAcceptHeader(options.getRequestProfile()))) {
          if (response.statusCode() == 301
              || response.statusCode() == 302
              || response.statusCode() == 303
              || response.statusCode() == 307) {
            final Optional<String> location = response.location();
            if (location.isPresent()) {
              targetUri = UriResolver.resolveAsUri(targetUri, location.get());
              continue;
            }
            throw new JsonLdError(JsonLdErrorCode.LOADING_DOCUMENT_FAILED, "Header location is required for code [" + response.statusCode() + "].");
          }

          if (response.statusCode() != 200) {
            throw new JsonLdError(JsonLdErrorCode.LOADING_DOCUMENT_FAILED, "Unexpected response code [" + response.statusCode() + "]");
          }

          final Optional<String> contentTypeValue = response.contentType();

          if (contentTypeValue.isPresent()) {
            contentType = MediaType.of(contentTypeValue.get());
          }

          final Collection<String> linkValues = response.links();
          if (linkValues != null && !linkValues.isEmpty()) {
            if (contentType == null
                || (!MediaType.JSON.match(contentType)
                && !contentType.subtype().toLowerCase().endsWith(PLUS_JSON))) {
              final URI baseUri = targetUri;

              Optional<Link> alternate = linkValues.stream()
                  .flatMap(l -> Link.of(l, baseUri).stream())
                  .filter(
                      l -> l.relations().contains("alternate")
                      && l.type().isPresent()
                      && MediaType.JSON_LD.match(l.type().get())
                  )
                  .findFirst();

              if (alternate.isPresent()) {
                targetUri = alternate.get().target();
                continue;
              }
            }

            if (contentType != null
                && !MediaType.JSON_LD.match(contentType)
                && (MediaType.JSON.match(contentType)
                || contentType.subtype().toLowerCase().endsWith(PLUS_JSON))) {

              final URI baseUri = targetUri;
              final List<Link> contextUris = linkValues.stream()
                  .flatMap(l -> Link.of(l, baseUri).stream())
                  .filter(l -> l.relations().contains(ProfileConstants.CONTEXT))
                  .collect(Collectors.toList());

              if (contextUris.size() > 1) {
                throw new JsonLdError(JsonLdErrorCode.MULTIPLE_CONTEXT_LINK_HEADERS);
              } else if (contextUris.size() == 1) {
                contextUri = contextUris.get(0).target();
              }
            }
          }

          if (contentType == null) {
            log.warn("GET on URL [{}] does not return content-type header. Trying application/json.", uri);
            contentType = MediaType.JSON;
          }
          String body;
          try ( InputStream responseBody = response.body()) {
            body = IOUtils.toString(responseBody, StandardCharsets.UTF_8);
          }
          return new HttpDocument(contentType.toString(), targetUri.toString(), Objects.toString(contextUri, null), body);
        }
      }

      throw new JsonLdError(JsonLdErrorCode.LOADING_DOCUMENT_FAILED, "Too many redirections");

    } catch (IOException e) {
      throw new JsonLdError(JsonLdErrorCode.LOADING_DOCUMENT_FAILED, e);
    }
  }

  private HttpDocument getAndCache(URI uri, DocumentLoaderOptions options) throws JsonLdError {
    final String hash = HashUtils.calculateSha256AsHex(uri.toString());
    HttpDocument httpDocument = httpCache.fetchFromCache(uri.toString(), hash);
    if (httpDocument != null) {
      return httpDocument;
    }
    log.debug("Downloading {}", uri);
    httpDocument = fetchRemoteDocument(uri, options);
    httpCache.storeInCache(uri.toString(), hash, httpDocument);
    return httpDocument;
  }

  private Document createDocument(final HttpDocument httpDocument) throws JsonLdError {
    final MediaType type = MediaType.of(httpDocument.type);
    final URI targetUri = httpDocument.targetUri == null ? null : URI.create(httpDocument.targetUri);
    final URI contextUrl = httpDocument.contextUri == null ? null : URI.create(httpDocument.contextUri);

    final Document remoteDocument;
    if (JsonDocument.accepts(type)) {
      remoteDocument = JsonDocument.of(type, new StringReader(httpDocument.body));
    } else {
      remoteDocument = RdfDocument.of(type, new StringReader(httpDocument.body));
    }
    remoteDocument.setDocumentUrl(targetUri);
    remoteDocument.setContextUrl(contextUrl);
    return remoteDocument;
  }

  @Override
  public Document loadDocument(URI uri, DocumentLoaderOptions options) throws JsonLdError {
    HttpDocument httpDocument = getAndCache(uri, options);
    return createDocument(httpDocument);
  }

  @AllArgsConstructor
  @Getter
  @Setter
  public static final class HttpDocument {

    private String type;
    private String targetUri;
    private String contextUri;
    private String body;
  }
}
