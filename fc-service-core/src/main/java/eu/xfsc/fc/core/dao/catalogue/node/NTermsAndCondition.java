package eu.xfsc.fc.core.dao.catalogue.node;

import lombok.*;
import org.springframework.data.neo4j.core.schema.Node;

import static eu.xfsc.fc.core.dao.catalogue.NodeRelationConstant.N_TERMS_AND_CONDITIONS;

@Node(labels = N_TERMS_AND_CONDITIONS)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NTermsAndCondition extends BaseEntity {
    private String hash;
    private String url;
    private String content;
    private boolean gaiaxTnc;
}
