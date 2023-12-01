package eu.xfsc.fc.core.config;

import java.net.URI;
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

	@Value("${federated-catalogue.verification.additional-contexts}")
	private List<String> trustAnchorAdditionalContexts;

	private static ConfigurableDocumentLoader ADDITIONAL_CONTEXT_DOCUMENT_LOADER;
	
	/**
	 * Returns a default document loader
	 * 
	 * @see VerifiableCredentialContexts#DOCUMENT_LOADER
	 * @return a default document loader instance
	 */
	@Bean
	public DocumentLoader documentLoader() {
		return VerifiableCredentialContexts.DOCUMENT_LOADER;
	}

	/**
	 * Returns a document loader with additional context(s) loaded
	 * 
	 * <p>
	 * <b>TODO:</b> all w3c context files are part of the libraries in use, may be we should add the 
	 *      contexts we configure in the application into the catalog and load them by a classloader
	 *      instead of streaming them all the time. Right now we should stay with this solution, since
	 *      GAIA-X is enforcing fast changes during the last months...
	 * </p>     
	 * @return a document loader instance
	 */
	@Bean
	public DocumentLoader additionalContextDocumentLoader() {
		if (ADDITIONAL_CONTEXT_DOCUMENT_LOADER == null) {
			ADDITIONAL_CONTEXT_DOCUMENT_LOADER = new ConfigurableDocumentLoader(
					VerifiableCredentialContexts.CONTEXTS);
			
			if (this.trustAnchorAdditionalContexts != null && !this.trustAnchorAdditionalContexts.isEmpty()) {

				final DocumentLoaderOptions loaderOptions = new DocumentLoaderOptions();
				loaderOptions.setProfile(ProfileConstants.CONTEXT);
				loaderOptions.setRequestProfile(Arrays.asList(loaderOptions.getProfile()));

				for (final String trustAnchroContextUrl : this.trustAnchorAdditionalContexts) {
					try {
						final URI uri = URI.create(trustAnchroContextUrl);
						// use cached document loader
						final Document document = SchemeRouter.defaultInstance().loadDocument(uri, loaderOptions);
						if (document instanceof JsonDocument) {
							final JsonDocument jsonDocument = JsonDocument.class.cast(document);
							ADDITIONAL_CONTEXT_DOCUMENT_LOADER.getLocalCache().put(uri, jsonDocument);
							log.debug("Add additional context {} to the VerifiablePresentation", trustAnchroContextUrl);
						} else {
							log.error("Load an unknown document type '{}' for context '{}', expected was '{}'",
									document.getClass(), trustAnchroContextUrl, JsonDocument.class.getSimpleName());
							throw new VerificationException(
									"Unknown document format while load additional (GAIA-X) context '"
											+ trustAnchroContextUrl + "'");
						}
					} catch (Exception ex) {
						log.error(
								"Unexpected error while try to load additional context {} to the VerifiablePresentation",
								trustAnchroContextUrl);
						throw new ExceptionInInitializerError("Unexpected error while load additional context '"
								+ trustAnchroContextUrl + "': " + ex.getMessage());
					}
				}
			} 
		}
		return ADDITIONAL_CONTEXT_DOCUMENT_LOADER;
	}
}
