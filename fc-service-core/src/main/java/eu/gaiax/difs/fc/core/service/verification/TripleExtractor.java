package eu.gaiax.difs.fc.core.service.verification;

import com.apicatalog.rdf.RdfTriple;
import eu.gaiax.difs.fc.core.pojo.ContentAccessor;

import java.util.List;

public interface TripleExtractor {
    List<RdfTriple> extractClaimsRDF(ContentAccessor content) throws Exception;
}
