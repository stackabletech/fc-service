package eu.xfsc.fc.core.dao.catalogue.node;

import lombok.*;
import org.springframework.data.neo4j.core.schema.Node;

import static eu.xfsc.fc.core.dao.catalogue.NodeRelationConstant.N_ADDRESS;

@Node(labels = N_ADDRESS)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NAddress extends BaseEntity {
    private String gps;
}
