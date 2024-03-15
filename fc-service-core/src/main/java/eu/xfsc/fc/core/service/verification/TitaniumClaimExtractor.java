package eu.xfsc.fc.core.service.verification;

import java.util.ArrayList;
import java.util.List;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.document.JsonDocument;
import com.apicatalog.rdf.RdfDataset;
import com.apicatalog.rdf.RdfGraph;
import com.apicatalog.rdf.RdfTriple;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.xfsc.fc.core.pojo.ContentAccessor;
import eu.xfsc.fc.core.pojo.SdClaim;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TitaniumClaimExtractor implements ClaimExtractor {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<SdClaim> extractClaims(ContentAccessor content) throws Exception {
        log.debug("extractClaims.enter; got content: {}", content);
        List<SdClaim> claims = new ArrayList<>();
        Document document = JsonDocument.of(content.getContentAsStream());
        JsonArray arr = JsonLd.expand(document).get();
        log.debug("extractClaims; expanded: {}", arr);
        JsonObject vp = arr.get(0).asJsonObject();
        JsonArray vcs = vp.get("https://www.w3.org/2018/credentials#verifiableCredential").asJsonArray();
        for (JsonValue vcv: vcs) {
            JsonObject vc = vcv.asJsonObject();
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
                        SdClaim claim = new SdClaim(triple, objectMapper);
                        claims.add(claim);
                    }
                }
            }
        }
        log.debug("extractClaims.exit; returning claims: {}", claims);
        return claims;
    }
    
    
}

