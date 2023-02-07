package eu.gaiax.difs.fc.core.service.verification;

import java.util.List;

import eu.gaiax.difs.fc.core.pojo.ContentAccessor;
import eu.gaiax.difs.fc.core.pojo.SdClaim;

public interface ClaimExtractor {
    
    List<SdClaim> extractClaims(ContentAccessor content) throws Exception;
    
}
