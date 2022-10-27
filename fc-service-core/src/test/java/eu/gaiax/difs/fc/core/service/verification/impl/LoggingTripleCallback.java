package eu.gaiax.difs.fc.core.service.verification.impl;

import com.github.jsonldjava.core.JsonLdTripleCallback;
import com.github.jsonldjava.core.RDFDataset;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoggingTripleCallback implements JsonLdTripleCallback {

    @Override
    public Object call(RDFDataset rdf) {
        log.debug("got RDF: {}", rdf);
        return null;
    }

}
