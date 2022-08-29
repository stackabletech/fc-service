package eu.gaiax.difs.fc.core.service.graphdb;

import eu.gaiax.difs.fc.core.pojo.SdClaim;

import java.util.List;

public interface GraphStore {


    /**
     * Pushes set of claims to the Graph db. The set of claims are list of claim
     * objects containing subject, predicate and object in the form of ntriples
     * format stored in individual strings.
     *
     * @param sdClaimList       List of claims to add to the Graph DB.
     * @param credentialSubject
     * @return String SUCCESS or FAIL
     */
    public void addClaims(List<SdClaim> sdClaimList, String credentialSubject);


}

