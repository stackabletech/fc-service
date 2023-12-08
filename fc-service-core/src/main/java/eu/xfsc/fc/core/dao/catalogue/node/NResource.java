package eu.xfsc.fc.core.dao.catalogue.node;

import lombok.*;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static eu.xfsc.fc.core.dao.catalogue.NodeRelationConstant.*;
import static org.springframework.data.neo4j.core.schema.Relationship.Direction.INCOMING;
import static org.springframework.data.neo4j.core.schema.Relationship.Direction.OUTGOING;

@Node(labels = N_RESOURCE)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NResource extends BaseEntity {
    private String credentialSubjectId;
    private String type;
    private String description;
    private Boolean containsPII;
    private Date obsoleteDateTime;
    private Date expirationDateTime;
    private String producedBy;
    private String vc;
    private Set<String> policy = new HashSet<>();
    private Set<String> license = new HashSet<>();
    private Set<String> exposedThroughResource = new HashSet<>();
    @Relationship(value = AGGREGATION_OF, direction = OUTGOING)
    private Set<NResource> aggregationResource = new HashSet<>();
    @Relationship(value = COPYRIGHT_OWNED_BY, direction = OUTGOING)
    private Set<NParticipant> copyRightOwnedBy = new HashSet<>();
    @Relationship(value = MAINTAINED_BY, direction = OUTGOING)
    private Set<NParticipant> maintainedBy = new HashSet<>();
    @Relationship(value = MANUFACTURED_BY, direction = OUTGOING)
    private Set<NParticipant> manufacturedBy = new HashSet<>();
    @Relationship(value = OWNED_BY, direction = OUTGOING)
    private Set<NParticipant> ownedBy = new HashSet<>();
    @Relationship(value = LOCATION, direction = OUTGOING)
    private Set<NAddress> location = new HashSet<>();
    @Relationship(value = LOCATION_ADDRESS, direction = OUTGOING)
    private Set<NAddress> locationAddress = new HashSet<>();
    @Relationship(value = HOSTED_ON, direction = INCOMING)
    private NServiceInstance serviceInstance;
}
