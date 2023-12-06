package eu.xfsc.fc.core.dao.catalogue.node;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Node;

import static eu.xfsc.fc.core.dao.catalogue.NodeRelationConstant.N_SERVICE_ACCESS_POINT;

@Node(labels = N_SERVICE_ACCESS_POINT)
@Getter
@Setter
public class NServiceAccessPoint extends BaseEntity {
    private String version;
    private String port;
    private String protocol;
    private String openAPI;
    private String host;
}
