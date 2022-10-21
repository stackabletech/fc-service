package eu.gaiax.difs.fc.core.exception;

/**
 * This exception is thrown whenever a claim is passed to the
 * graph store that has syntax errors w.r.t. its RDF
 * serialisation, e.g. broken URIs, invalid literals etc.
 */
public class QueryException extends ServerException {
    
  public QueryException(String msg) {
    super(msg);
  }

  public QueryException(String message, Throwable cause) {
    super(message, cause);
  }

}
