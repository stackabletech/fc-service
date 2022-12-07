package eu.gaiax.difs.fc.core.util;

import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.StatementImpl;
import org.apache.jena.vocabulary.RDF;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ExtendClaims {

    /**
     * Adds annotation property with value credential subject for claims Uses
     * model which previously validated containing claims
     *
     * @param claims
     * @param credentialSubject
     * @return Triples as string
     */


    public static String addPropertyGraphUri(Model claims, String credentialSubject) {
        Literal credentialSubjectLiteral = ResourceFactory.createStringLiteral(credentialSubject);
        Property claimsGraphUri = ResourceFactory.createProperty("http://w3id.org/gaia-x/service#claimsGraphUri");

        List<Statement> additionalTriples = new ArrayList<>();

        StmtIterator triples = claims.listStatements();
        while (triples.hasNext()) {
            Statement triple = triples.next();
            Resource s = triple.getSubject();
            Property p = triple.getPredicate();
            RDFNode o = triple.getObject();
            additionalTriples.add(
                    new StatementImpl(
                            s,
                            claimsGraphUri,
                            credentialSubjectLiteral
                    )
            );

            if (o.isResource() && !p.equals(RDF.type)) {
                // URIs and blank nodes, but not literals
                additionalTriples.add(
                        new StatementImpl(
                                o.asResource(),
                                claimsGraphUri,
                                credentialSubjectLiteral
                        )
                );
            }
        }

        claims.add(additionalTriples);
        OutputStream outputstream = new ByteArrayOutputStream();
        claims.write(outputstream, "N-TRIPLES");
        return outputstream.toString();
    }


    public static HashSet<String> getMultivalProp(Model claims) {
        ExtendClaims extendClaims = new ExtendClaims();
        HashSet<String> multiprop = new HashSet<String>();
        StmtIterator triples = claims.listStatements();
        while (triples.hasNext()) {
            Statement triple = triples.next();
            Resource s = triple.getSubject();
            Property p = triple.getPredicate();
            if (extendClaims.checkMultivalueProp(s, p))
                multiprop.add(p.toString());
        }
        return multiprop;
    }


    private boolean checkMultivalueProp(Resource subject, Property predicate) {
        if (subject.listProperties(predicate).toList().size() > 1)
            return true;
        return false;
    }
}