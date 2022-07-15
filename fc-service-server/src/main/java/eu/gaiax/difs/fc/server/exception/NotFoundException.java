package eu.gaiax.difs.fc.server.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotFoundException extends ServiceException {
    private final String message;

    public NotFoundException(String msg) {
        super(msg);
        this.message = msg;
    }
}