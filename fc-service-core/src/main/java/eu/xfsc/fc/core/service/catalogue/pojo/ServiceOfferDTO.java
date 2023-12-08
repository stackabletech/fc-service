package eu.xfsc.fc.core.service.catalogue.pojo;

import eu.xfsc.fc.core.dao.catalogue.node.NAddress;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
public class ServiceOfferDTO {

    private String credentialSubjectId;
    private String name;
    private String description;
    private TermsAndConditionDTO tnc;
    private DataAccountExportDTO dataAccountExport;
    private Set<String> dataProtectionRegime = new HashSet<>();
    private ParticipantDTO providedBy;
    private Set<String> policy = new HashSet<>();
    private Set<ServiceOfferDTO> dependsOn = new HashSet<>();
    private Set<ResourceDTO> aggregationOf = new HashSet<>();
    private Set<NAddress> locations = new HashSet<>();

    private Double veracity;
    private Double transparency;
    private Double trustIndex;

    private String labelLevel;
}
