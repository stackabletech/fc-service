package eu.gaiax.difs.fc.server.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParserException extends ServiceException {
  public ParserException(String msg) {
    super(msg);
  }

  public ParserException(String msg, Throwable throwable) {
    super(msg, throwable);
  }
}