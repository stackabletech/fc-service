package eu.xfsc.fc.core.dao.catalogue.node;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static eu.xfsc.fc.core.dao.catalogue.NodeRelationConstant.*;
import static org.springframework.data.neo4j.core.schema.Relationship.Direction.OUTGOING;

@Node(labels = N_SERVICE_OFFERING)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NServiceOffer extends BaseEntity {
    private String credentialSubjectId;
    private String vc;
    private String description;
    private boolean isCatalogue;
    private Set<String> policy = new HashSet<>();
    private String labelLevel;
    @JsonIgnore
    @Relationship(value = HAS_CATALOGUE, direction = OUTGOING)
    private NServiceOffer catalogue;
    @Relationship(value = DATA_ACCOUNT_EXPORT, direction = OUTGOING)
    private NDataAccountExport dataAccountExport;
    @Relationship(value = TERMS_AND_CONDITIONS, direction = OUTGOING)
    private NTermsAndCondition termsAndCondition;
    @Relationship(value = PROVIDED_BY, direction = OUTGOING)
    private NParticipant participant;
    @Relationship(value = DATA_PROTECTION_REGIME, direction = OUTGOING)
    private Set<NDataProtectionRegime> protectionRegime = new HashSet<>();
    @Relationship(value = AGGREGATION_OF, direction = OUTGOING)
    private Set<NResource> resources = new HashSet<>();
    @Relationship(value = DEPENDS_ON, direction = OUTGOING)
    private Set<NServiceOffer> dependedServices = new HashSet<>();
    @Relationship(value = LOCATED_IN, direction = OUTGOING)
    private Set<NAddress> locations = new HashSet<>();
    private Date createdDate;
    private Double veracity;
    private Double transparency;
    private Double trustIndex;
}
