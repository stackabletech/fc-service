package eu.xfsc.fc.core.config;

import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.document.JsonDocument;
import com.apicatalog.jsonld.http.ProfileConstants;
import com.apicatalog.jsonld.loader.DocumentLoader;
import com.apicatalog.jsonld.loader.DocumentLoaderOptions;
import com.apicatalog.jsonld.loader.SchemeRouter;
import com.danubetech.verifiablecredentials.jsonld.VerifiableCredentialContexts;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import eu.xfsc.fc.core.exception.VerificationException;
import foundation.identity.jsonld.ConfigurableDocumentLoader;
import lombok.extern.slf4j.Slf4j;

/**
 * Spring configuration class to create and inject a {@link DocumentLoader} into
 * {@link Autowired} fields.
 * 
 * <p>
 * Note: Please use <code> @Qualifier("additionalContextDocumentLoader")</code> to 
 * inject a document loader with additional context(s) by the application config item
 * <code>federated-catalogue.verification.additional-contexts</code>.
 * <p>
 * 
 */
@Slf4j
@Configuration
public class DocumentLoaderConfig {

	@Value("${federated-catalogue.verification.doc-loader.enable-file:true}")
	private boolean enableFile;
	@Value("${federated-catalogue.verification.doc-loader.enable-http:true}")
	private boolean enableHttp;
	@Value("${federated-catalogue.verification.doc-loader.enable-local-cache:false}")
	private boolean enableLocalCache;
	@Value("${federated-catalogue.verification.doc-loader.additional-contexts}")
	private List<String> trustAnchorAdditionalContexts;
	
	/**
	 * Returns a default document loader with additional context and settings
	 * 
	 * @see VerifiableCredentialContexts#DOCUMENT_LOADER
	 * <p>
	 * <b>TODO:</b> all w3c context files are part of the libraries in use, may be we should add the 
	 *      contexts we configure in the application into the catalog and load them by a classloader
	 *      instead of streaming them all the time. Right now we should stay with this solution, since
	 *      GAIA-X is enforcing fast changes during the last months...
	 * </p>     
	 * @return a document loader instance
	 */
	@Bean
	public DocumentLoader documentLoader(Cache<URI, Document> docLoaderCache) {
		log.info("documentLoader.enter; cache: {}", docLoaderCache);
		ConfigurableDocumentLoader loader = (ConfigurableDocumentLoader) VerifiableCredentialContexts.DOCUMENT_LOADER;
		loader.setEnableFile(enableFile);
		loader.setEnableHttp(enableHttp);
		loader.setEnableHttps(enableHttp);
		loader.setEnableLocalCache(enableLocalCache);
		if (!enableLocalCache) {
		    loader.setRemoteCache(docLoaderCache);
		}

		if (this.trustAnchorAdditionalContexts != null && !this.trustAnchorAdditionalContexts.isEmpty()) {
			final DocumentLoaderOptions loaderOptions = new DocumentLoaderOptions();
			loaderOptions.setProfile(ProfileConstants.CONTEXT);
			loaderOptions.setRequestProfile(Arrays.asList(loaderOptions.getProfile()));
			for (final String trustAnchorContextUrl : this.trustAnchorAdditionalContexts) {
				loadContext(loader, loaderOptions, trustAnchorContextUrl);
			}
		}
		//try {
		//	docLoaderCache.put(new URI("https://schema.org"), JsonDocument.of(new StringReader("{\"@context\": {}}")));
		//} catch (URISyntaxException | JsonLdError ex) {
		//	log.info("documentLoader.error", ex);
		//}
		log.info("documentLoader.exit; returning: {}", loader);
		return loader;
	}
	
	private void loadContext(ConfigurableDocumentLoader loader, DocumentLoaderOptions options, String url) {
		try {
			final URI uri = URI.create(url);
			// use cached document loader
			final Document document = SchemeRouter.defaultInstance().loadDocument(uri, options);
			if (document instanceof JsonDocument) {
				final JsonDocument jsonDocument = JsonDocument.class.cast(document);
				if (enableLocalCache) {
				    loader.getLocalCache().put(uri, jsonDocument);
				} else {
				    loader.getRemoteCache().put(uri, jsonDocument);
				}
				log.debug("Add additional context {} to the VerifiablePresentation", url);
			} else {
				log.error("Load an unknown document type '{}' for context '{}', expected was '{}'",
						document.getClass(), url, JsonDocument.class.getSimpleName());
				throw new VerificationException(
						"Unknown document format while load additional (GAIA-X) context '"	+ url + "'");
			}
		} catch (Exception ex) {
			log.error("Unexpected error while try to load additional context {} to the VerifiablePresentation", url);
			throw new ExceptionInInitializerError("Unexpected error while load additional context '" + url + "': " + ex.getMessage());
		}
	}
	
	@Bean
	public Cache<URI, Document> docLoaderCache(@Value("${federated-catalogue.verification.doc-loader.cache.size}") int cacheSize,
			@Value("${federated-catalogue.verification.doc-loader.cache.timeout}") Duration timeout) {
		log.info("docLoaderCache.enter; cache size: {}, ttl: {}", cacheSize, timeout);
        Caffeine<?, ?> cache = Caffeine.newBuilder().expireAfterAccess(timeout); 
        if (cacheSize > 0) {
            cache = cache.maximumSize(cacheSize);
        } 
        //if (synchronizer != null) {
        //    cache = cache.removalListener(new DataListener<>(synchronizer));
        //}
		log.info("docLoaderCache.exit; returning: {}", cache);
        return (Cache<URI, Document>) cache.build();
	}
	
	
}
