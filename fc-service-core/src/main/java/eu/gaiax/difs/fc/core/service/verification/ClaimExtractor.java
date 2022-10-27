package eu.gaiax.difs.fc.core.service.verification;

import java.util.List;

import com.apicatalog.rdf.RdfValue;

import eu.gaiax.difs.fc.core.pojo.ContentAccessor;
import eu.gaiax.difs.fc.core.pojo.SdClaim;

public interface ClaimExtractor {
    
    List<SdClaim> extractClaims(ContentAccessor content) throws Exception;
    
    default String rdf2String(RdfValue rdf) {
        if (rdf.isBlankNode()) return rdf.getValue();
        if (rdf.isLiteral()) return "\"" + rdf.getValue() + "\"";
        // rdf is IRI. here we could try to make it absolute..
        return "<" + rdf.getValue() + ">";
     }
    
}
