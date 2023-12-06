package eu.xfsc.fc.core.dao.catalogue.node;

import lombok.*;
import org.springframework.data.neo4j.core.schema.Node;

import static eu.xfsc.fc.core.dao.catalogue.NodeRelationConstant.N_REGISTRATION_NUMBER;

@Node(labels = N_REGISTRATION_NUMBER)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NRegistrationNumber extends BaseEntity {
    private String type;
    private String number;
    private String countryCode;
    private String url;
}
