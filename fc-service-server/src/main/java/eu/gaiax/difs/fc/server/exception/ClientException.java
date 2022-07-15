package eu.gaiax.difs.fc.server.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClientException extends ServiceException {
    private final String message;

    public ClientException(String msg) {
        super(msg);
        this.message = msg;
    }
}