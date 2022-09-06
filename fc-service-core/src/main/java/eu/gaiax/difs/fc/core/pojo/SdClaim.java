package eu.gaiax.difs.fc.core.pojo;

/**
 * POJO Class for holding a Claim. A Claim is a triple represented by a subject, predicate, and object.
 */
@lombok.AllArgsConstructor
@lombok.EqualsAndHashCode
@lombok.Getter
@lombok.ToString
public class SdClaim {

  private final String subject;
  private final String predicate;
  private final String object;

  public String stripSubject() {
    return subject.substring(1, subject.length() - 1);
  }
  
  public String asTriple() {
    return String.format("%s %s %s . \n", subject, predicate, object );
  }
  
}
