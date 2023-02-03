package eu.gaiax.difs.fc.core.service.verification.impl;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.document.JsonDocument;
import com.apicatalog.rdf.RdfDataset;
import com.apicatalog.rdf.RdfGraph;
import com.apicatalog.rdf.RdfTriple;
import eu.gaiax.difs.fc.core.pojo.ContentAccessor;
import eu.gaiax.difs.fc.core.service.verification.TripleExtractor;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class TitaniumClaimExtractorNQuad implements TripleExtractor {
@Override
    public List<RdfTriple> extractClaimsRDF(ContentAccessor content) throws Exception {
        log.debug("extractClaims.enter; got content: {}", content);
        Document document = JsonDocument.of(content.getContentAsStream());
        JsonArray arr = JsonLd.expand(document).get();
        log.debug("extractClaims; expanded: {}", arr);
        JsonObject vp = arr.get(0).asJsonObject();
        JsonArray vcs = vp.get("https://www.w3.org/2018/credentials#verifiableCredential").asJsonArray();
        List <RdfTriple> returnedRDFList = new ArrayList<>();
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
                        returnedRDFList.add(triple);
                    }
                }
            }
        }
        log.debug("extractClaims.exit; returning RDFClaims: {}", returnedRDFList);
        return returnedRDFList;
    }
    
    
}

