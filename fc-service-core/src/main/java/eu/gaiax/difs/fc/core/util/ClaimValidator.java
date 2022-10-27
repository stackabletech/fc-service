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
import org.apache.jena.util.iterator.ExtendedIterator;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

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
     * @param sdClaim the claim to be validated
     * @return the claim as a formatted triple string
     */
    public Model validateClaim(SdClaim sdClaim) {
        Model model = validateRDFTripleSyntax(sdClaim);
        return model;
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
    private Model validateRDFTripleSyntax(SdClaim sdClaim) {
        Model model = ModelFactory.createDefaultModel();
        try (InputStream in = IOUtils.toInputStream(sdClaim.asTriple(), "UTF-8")) {
            switchOnJenaLiteralValidation();
            RDFDataMgr.read(model, in, Lang.TTL);

        } catch (IOException e) {
            // TODO: How to consistently log syntax errors in input data? DEBUG, WARN, ERR?
            log.debug(e.getMessage());
            throw new QueryException("Syntax error in triple " + sdClaim.asTriple());

        } catch (DatatypeFormatException e) {
            // Only occurs if the value of a literal does not comply with the
            // literals datatype
            throw new QueryException(
                    "Object in triple " +
                            sdClaim.asTriple() +
                            " has an invalid value given its datatype"
            );

        } catch (RiotException e) {
            throw new QueryException(
                    "Object in triple " + sdClaim.asTriple() +
                            " has a syntax error: " + e.getMessage()
            );

        } finally {
            resetJenaLiteralValidation();
        }

        for (ExtendedIterator<Triple> it = model.getGraph().find(); it.hasNext(); ) {
            Triple triple = it.next();
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
        return model;
    }
}
