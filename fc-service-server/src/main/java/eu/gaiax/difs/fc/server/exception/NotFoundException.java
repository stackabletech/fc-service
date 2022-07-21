package eu.gaiax.difs.fc.server.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotFoundException extends ServiceException {
    public NotFoundException(String msg) {
        super(msg);
    }
}