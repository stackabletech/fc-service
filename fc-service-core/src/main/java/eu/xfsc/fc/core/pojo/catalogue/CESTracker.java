package eu.xfsc.fc.core.pojo.catalogue;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CESTracker {

    private String cesId;
    private String reason;
    private String credential;
    private Long status;

}
