package eu.xfsc.fc.core.service.verification;

import java.util.List;

import eu.xfsc.fc.core.pojo.ContentAccessor;
import eu.xfsc.fc.core.pojo.SdClaim;

public interface ClaimExtractor {
    
    List<SdClaim> extractClaims(ContentAccessor content) throws Exception;
    
}
