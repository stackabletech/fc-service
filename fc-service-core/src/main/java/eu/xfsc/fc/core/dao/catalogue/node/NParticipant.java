package eu.xfsc.fc.core.dao.catalogue.node;

import lombok.*;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.HashSet;
import java.util.Set;

import static eu.xfsc.fc.core.dao.catalogue.NodeRelationConstant.*;
import static org.springframework.data.neo4j.core.schema.Relationship.Direction.INCOMING;
import static org.springframework.data.neo4j.core.schema.Relationship.Direction.OUTGOING;

@Node(labels = N_PARTICIPANT)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NParticipant extends BaseEntity {
    private String type;
    private String did;
    private String credentialSubjectId;
    private String vc;
    @Relationship(value = REGISTRATION_NUMBER)
    private Set<NRegistrationNumber> registrationNumber = new HashSet<>();
    @Relationship(value = MAINTAINED_BY, direction = INCOMING)
    private NServiceInstance maintainedBy;
    @Relationship(value = TENANT_OWNED_BY, direction = INCOMING)
    private NServiceInstance tenantOwnedBy;
    @Relationship(value = TERMS_AND_CONDITIONS, direction = OUTGOING)
    private NTermsAndCondition termsAndCondition;
    @Relationship(value = LEGAL_ADDRESS, direction = OUTGOING)
    private NAddress legalAddress;
    @Relationship(value = HEADQUARTER_ADDRESS, direction = OUTGOING)
    private NAddress headQuarterAddress;
    @Relationship(value = PARENT_ORGANIZATION, direction = OUTGOING)
    private Set<NParticipant> parentOrganization = new HashSet<>();
    @Relationship(value = SUB_ORGANIZATION, direction = OUTGOING)
    private Set<NParticipant> subOrganization = new HashSet<>();
}
