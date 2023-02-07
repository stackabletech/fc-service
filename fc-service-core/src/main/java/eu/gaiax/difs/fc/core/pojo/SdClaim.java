package eu.gaiax.difs.fc.core.pojo;

import java.util.Objects;

import com.apicatalog.rdf.RdfTriple;
import com.apicatalog.rdf.RdfValue;

/**
 * POJO Class for holding a Claim. A Claim is a triple represented by a subject, predicate, and object.
 */
public class SdClaim {

  private RdfTriple triple;
  private String subject;
  private String predicate;
  private String object;
  
  public SdClaim(RdfTriple triple) {
	this.triple = triple;
	this.subject = rdf2String(triple.getSubject());
	this.predicate = rdf2String(triple.getPredicate());
	this.object = rdf2String(triple.getObject());
  }
  
  public SdClaim(String subject, String predicate, String object) {
	this.subject = subject;
	this.predicate = predicate;
	this.object = object;	  
  }
  
  public RdfTriple getTriple() {
	return triple;
  }
  
  public RdfValue getSubject() {
	return triple == null ? null : triple.getSubject();
  }

  public String getSubjectString() {
	return subject;
  }

  public String getSubjectValue() {
    return triple == null ? subject.substring(1, subject.length() - 1) : triple.getSubject().getValue(); 
  }
    
  public RdfValue getPredicate() {
	return triple == null ? null : triple.getPredicate();
  }
  
  public String getPredicateString() {
	return predicate;
  }
  
  public String getPredicateValue() {
	return triple == null ? predicate.substring(1, predicate.length() - 1) : triple.getPredicate().getValue(); 
  }
	    
  public RdfValue getObject() {
	return triple == null ? null : triple.getObject();
  }
  
  public String getObjectString() {
	return object;
  }

  public String getObjectValue() {
	return triple == null ? object.substring(1, object.length() - 1) : triple.getObject().getValue(); 
  }
	    
  public String asTriple() {
    return String.format("%s %s %s . \n", getSubjectString(), getPredicateString(), getObjectString());
  }
  
  @Override
  public int hashCode() {
	return Objects.hash(object, predicate, subject);
  }

  @Override
  public boolean equals(Object obj) {
	if (this == obj)
		return true;
	if (obj == null)
		return false;
	if (getClass() != obj.getClass())
		return false;
	SdClaim other = (SdClaim) obj;
	return Objects.equals(object, other.object) && Objects.equals(predicate, other.predicate)
			&& Objects.equals(subject, other.subject);
  }

  @Override
  public String toString() {
	return "SdClaim[" + subject + " " + predicate + " " + object + "]";  
  }
  
  private String rdf2String(RdfValue rdf) {
    if (rdf.isBlankNode()) return rdf.getValue();
    if (rdf.isLiteral()) return "\"" + rdf.getValue() + "\"";
    // rdf is IRI. here we could try to make it absolute..
    return "<" + rdf.getValue() + ">";
  }
  
}
