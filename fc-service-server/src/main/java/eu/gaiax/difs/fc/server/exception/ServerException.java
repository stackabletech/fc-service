package eu.gaiax.difs.fc.server.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServerException extends ServiceException {
    public ServerException(String msg) {
        super(msg);
    }
}