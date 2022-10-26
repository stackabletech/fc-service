package eu.gaiax.difs.fc.core.service.verification.impl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.document.JsonDocument;
import com.apicatalog.rdf.RdfDataset;
import com.apicatalog.rdf.RdfGraph;
import com.apicatalog.rdf.RdfTriple;
import com.apicatalog.rdf.RdfValue;

import eu.gaiax.difs.fc.core.exception.ParserException;
import eu.gaiax.difs.fc.core.exception.VerificationException;
import eu.gaiax.difs.fc.core.pojo.ContentAccessor;
import eu.gaiax.difs.fc.core.pojo.SdClaim;
import eu.gaiax.difs.fc.core.service.verification.ClaimExtractor;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TitaniumClaimExtractor implements ClaimExtractor {

    @Override
    public List<SdClaim> extractClaims(ContentAccessor content) throws Exception {
        log.debug("extractClaims.enter; got content: {}", content);
        List<SdClaim> claims = new ArrayList<>();
        Document document = JsonDocument.of(content.getContentAsStream());
        JsonArray arr = JsonLd.expand(document).get();
        log.debug("extractClaims; expanded: {}", arr);
        JsonObject vp = arr.get(0).asJsonObject();
        JsonArray vcs = vp.get("https://www.w3.org/2018/credentials#verifiableCredential").asJsonArray();
        JsonObject vc = vcs.get(0).asJsonObject();
        JsonArray graph = vc.get("@graph").asJsonArray();
        for (JsonValue val: graph) {
            JsonObject obj = val.asJsonObject();
            JsonArray css = obj.getJsonArray("https://www.w3.org/2018/credentials#credentialSubject");
            for (JsonValue cs: css) {
                Document csDoc = JsonDocument.of(cs.asJsonObject());
                RdfDataset rdf = JsonLd.toRdf(csDoc).produceGeneralizedRdf(true).get();
                RdfGraph rdfGraph = rdf.getDefaultGraph();
                List<RdfTriple> triples = rdfGraph.toList();
                for (RdfTriple triple: triples) {
                    log.debug("extractClaims; got triple: {}", triple);
                    SdClaim claim = new SdClaim(rdf2String(triple.getSubject()), rdf2String(triple.getPredicate()), rdf2String(triple.getObject()));
                    claims.add(claim);
                }
            }
        }
        log.debug("extractClaims.exit; returning claims: {}", claims);
        return claims;
    }
    
}
