package eu.xfsc.fc.core.dao.catalogue.node;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Node;

import static eu.xfsc.fc.core.dao.catalogue.NodeRelationConstant.N_DATA_PROTECTION_REGIME;

@Node(labels = N_DATA_PROTECTION_REGIME)
@Getter
@Setter
@NoArgsConstructor
public class NDataProtectionRegime extends BaseEntity {
}
