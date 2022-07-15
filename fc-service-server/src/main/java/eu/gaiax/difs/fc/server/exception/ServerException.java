package eu.gaiax.difs.fc.server.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServerException extends ServiceException {
    private final String message;

    public ServerException(String msg) {
        super(msg);
        this.message = msg;
    }
}