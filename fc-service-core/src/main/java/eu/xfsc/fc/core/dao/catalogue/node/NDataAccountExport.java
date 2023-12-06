package eu.xfsc.fc.core.dao.catalogue.node;

import lombok.*;
import org.springframework.data.neo4j.core.schema.Node;

import java.util.HashSet;
import java.util.Set;

import static eu.xfsc.fc.core.dao.catalogue.NodeRelationConstant.N_DATA_ACCOUNT_EXPORT;

@Node(labels = N_DATA_ACCOUNT_EXPORT)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NDataAccountExport extends BaseEntity {
    private String accessType;
    private String requestType;
    private Set<String> formatType = new HashSet<>();
}
