package eu.xfsc.fc.core.dao.catalogue;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ECesStatus {
    IN_PROGRESS(1L),
    DONE(2L),
    FAILED(3L);

    private final Long id;
}
