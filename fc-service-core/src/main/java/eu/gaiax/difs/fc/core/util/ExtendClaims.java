package eu.gaiax.difs.fc.core.util;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.AnonId;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

public class ExtendClaims {

    /**
     * Adds annotation property with value credential subject for claims Uses
     * model which previously validated containing claims
     *
     * @param model
     * @param credentialSubject
     * @return Triples as string
     */
    public static String addPropertyGraphUri(Model model, String credentialSubject) {
        Property claimsGraphUri = model.createProperty("http://w3id.org/gaia-x/service#claimsGraphUri");
        Triple triple = model.getGraph().find().next();
        Node s = triple.getSubject();
        Node p = triple.getPredicate();
        Node o = triple.getObject();

        Resource subject;
        if (s.isURI())
            subject = model.createResource(s.getURI());
        else // assuming it is a blank node
            subject = model.createResource(new AnonId(s.getBlankNodeLabel()));

        model.add(subject, claimsGraphUri, credentialSubject);
        if (o.isURI() && !p.equals(RDF.type.asNode())) {
            Resource object = model.createResource(o.getURI());
            model.add(object, claimsGraphUri, credentialSubject);
        } else if (o.isBlank() && !p.equals(RDF.type.asNode())) {
            Resource blankNode = model.createResource(new AnonId(o.getBlankNodeId()));
            model.add(blankNode, claimsGraphUri, credentialSubject);
        }
        OutputStream outputstream = new ByteArrayOutputStream();
        model.write(outputstream, "N-TRIPLES");

        return outputstream.toString();
    }


}
