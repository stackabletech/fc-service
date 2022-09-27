package eu.gaiax.difs.fc.core.util;

import eu.gaiax.difs.fc.core.exception.QueryException;
import eu.gaiax.difs.fc.core.pojo.SdClaim;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.validator.routines.UrlValidator;
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
    public String validateClaim(SdClaim sdClaim) {
        validateRDFTripleSyntax(sdClaim);
        return sdClaim.asTriple();
    }

    /**
     * Method to validate that a claim follow the RDF triple syntax, i.e.,
     * < URI , URI, (URI, Literal) >
     *
     * @param sdClaim the claim to be validated
     */
    private void validateRDFTripleSyntax(SdClaim sdClaim) {
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

        UrlValidator urlValidator = UrlValidator.getInstance();
        for (ExtendedIterator<Triple> it = model.getGraph().find(); it.hasNext(); ) {
            Triple triple = it.next();

            Node s = triple.getSubject();
            if (!s.isURI() || !urlValidator.isValid(s.getURI())) {
                throw new QueryException(
                        "Subject in triple " +
                                sdClaim.asTriple() +
                                " is not a valid URI");
            }

            Node p = triple.getPredicate();
            if (!p.isURI() || !urlValidator.isValid(p.getURI())) {
                throw new QueryException(
                        "Predicate in triple " +
                                sdClaim.asTriple() +
                                " is not a valid URI");
            }

            Node o = triple.getObject();
            if (o.isURI()) {
                if (!urlValidator.isValid(o.getURI())) {
                    throw new QueryException(
                            "Object in triple " +
                                    sdClaim.asTriple() +
                                    " is not a valid URI"
                    );
                }

            } else if (o.isLiteral()) {
                // Nothing needs to be done here as literal syntax errors and
                // datatype errors are already handled by the parser directly.
                // See the catch blocks after the RDFDataMgr.read( ) call above.

            } else {
                // assuming that blank nodes are not allowed
                throw new QueryException(
                        "Object in triple " +
                                sdClaim.asTriple() +
                                " is neither a valid literal nor a valid URI"
                );
            }
        }
    }
}
