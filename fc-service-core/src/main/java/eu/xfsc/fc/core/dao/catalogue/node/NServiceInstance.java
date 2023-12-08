package eu.xfsc.fc.core.dao.catalogue.node;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import static eu.xfsc.fc.core.dao.catalogue.NodeRelationConstant.N_SERVICE_INSTANCE;
import static eu.xfsc.fc.core.dao.catalogue.NodeRelationConstant.SERVICE_ACCESS_POINT;
import static org.springframework.data.neo4j.core.schema.Relationship.Direction.OUTGOING;

@Node(labels = N_SERVICE_INSTANCE)
@Getter
@Setter
public class NServiceInstance extends BaseEntity {
    @Relationship(value = SERVICE_ACCESS_POINT, direction = OUTGOING)
    private NServiceAccessPoint accessPoint;
}
