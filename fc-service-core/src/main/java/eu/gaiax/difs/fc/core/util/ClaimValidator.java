package eu.gaiax.difs.fc.core.util;

import eu.gaiax.difs.fc.core.exception.QueryException;
import eu.gaiax.difs.fc.core.pojo.SdClaim;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.jena.datatypes.DatatypeFormatException;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RiotException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@Slf4j
public class ClaimValidator {
    // Stored to temporarily deviate from the standard Jena behavior of parsing
    // literals
    private boolean eagerJenaLiteralValidation;
    private boolean jenaAcceptanceOfUnknownLiteralDatatypes;

    private void switchOnJenaLiteralValidation() {
        // save the actual settings to not interfere with other modules which
        // rely on other settings
        eagerJenaLiteralValidation =
                org.apache.jena.shared.impl.JenaParameters.enableEagerLiteralValidation;
        jenaAcceptanceOfUnknownLiteralDatatypes =
                org.apache.jena.shared.impl.JenaParameters.enableSilentAcceptanceOfUnknownDatatypes;

        // Now switch to picky mode
        org.apache.jena.shared.impl.JenaParameters.enableEagerLiteralValidation = true;
        org.apache.jena.shared.impl.JenaParameters.enableSilentAcceptanceOfUnknownDatatypes = false;
    }

    private void resetJenaLiteralValidation() {
        org.apache.jena.shared.impl.JenaParameters.enableEagerLiteralValidation =
                eagerJenaLiteralValidation;
        org.apache.jena.shared.impl.JenaParameters.enableSilentAcceptanceOfUnknownDatatypes =
                jenaAcceptanceOfUnknownLiteralDatatypes;
    }

    /**
     * Validates if a claim elements are following the required syntax and
     * conditions before sending them to Neo4J
     *
     * @param sdClaimList the set of claims to be validated
     * @return the claim as a formatted triple string
     * @throws IOException 
     */
    public Model validateClaims(List<SdClaim> sdClaimList) throws IOException {
        Model listClaims = ModelFactory.createDefaultModel();
        StringBuilder payload = new StringBuilder();
        for (SdClaim sdClaim : sdClaimList) {
            validateRDFTripleSyntax(sdClaim);
            payload.append(sdClaim.asTriple());

        }
        InputStream in = IOUtils.toInputStream(payload, "UTF-8");
        RDFDataMgr.read(listClaims, in, Lang.TTL);
        return listClaims;
    }

    private String removeEnclosingAngleBrackets(String uriStr) {
        int strLen = uriStr.length();

        return uriStr.substring(1, strLen - 1);
    }

    /**
     * Method to validate that a claim follow the RDF triple syntax, i.e., <
     * (URI, blank node) , URI, (URI, blank node, literal) >
     *
     * @param sdClaim the claim to be validated
     */
    private void validateRDFTripleSyntax(SdClaim sdClaim) {
        Model model = ModelFactory.createDefaultModel();
        try (InputStream in = IOUtils.toInputStream(sdClaim.asTriple(), "UTF-8")) {
            switchOnJenaLiteralValidation();
            RDFDataMgr.read(model, in, Lang.TTL);

        } catch (IOException | DatatypeFormatException | RiotException e) {
            log.debug("Error in Validating validateRDFTripleSyntax {}", e.getMessage());
            throw new QueryException(
                    "Triple " + sdClaim.asTriple() +
                            " has a syntax error: " + e.getMessage()
            );

        } finally {
            resetJenaLiteralValidation();
        }

        Triple triple = model.getGraph().find().next();
        // --- subject ----------------------------------------------------
        Node s = triple.getSubject();
        if (s.isURI()) {
            // Caution! Jena will automatically apply modifications
            // to generate valid URIs. E.g. the broken URI
            // htw3id.org/gaia-x/indiv#serviceElasticSearch.json
            // will be converted to the URL
            // file:///home/user/some/path/fc-service/htw3id.org/gaia-x/indiv#serviceElasticSearch.json
            // Hence, we have to use the original URI string here
            // otherwise calling URI.create( ) will not fail in case
            // of a broken URI.
            // AND: We will have to strip off the angle brackets!
            // Any abbreviated URIS (i.e. something like ex:Foo without
            // angle brackets) will already be rejected above as Jena
            // should complain about the not defined prefix (e.g. ex in
            // the ex:Foo example above).
            try {
                String subjectStr =
                        removeEnclosingAngleBrackets(sdClaim.getSubject());
                URI uri = new URI(subjectStr);

            } catch (URISyntaxException e) {
                throw new QueryException(
                        "Subject in triple " +
                                sdClaim.asTriple() +
                                " is not a valid URI ");
            } // else it should be a blank node
        }
        // --- predicate --------------------------------------------------
        Node p = triple.getPredicate();
        if (p.isURI()) {
            try {
                // c.f. the comment for handling subject nodes above
                String predicateStr =
                        removeEnclosingAngleBrackets(sdClaim.getPredicate());
                URI uri = new URI(predicateStr);
            } catch (URISyntaxException e) {
                throw new QueryException(
                        "Predicate in triple " +
                                sdClaim.asTriple() +
                                " is not a valid URI ");

            }
        }
        // --- object -----------------------------------------------------
        Node o = triple.getObject();
        if (o.isURI()) {
            // c.f. the comment for handling subject nodes above
            try {
                String objectStr =
                        removeEnclosingAngleBrackets(sdClaim.getObject());
                URI uri = new URI(objectStr);
            } catch (URISyntaxException e) {
                throw new QueryException(
                        "Object in triple " +
                                sdClaim.asTriple() +
                                " is not a valid URI ");
            }
        } else if (o.isLiteral()) {
            // Nothing needs to be done here as literal syntax errors and
            // datatype errors are already handled by the parser directly.
            // See the catch blocks after the RDFDataMgr.read( ) call above.

        } // else it's a blank node, which is OK
    }
}
