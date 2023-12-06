package eu.xfsc.fc.core.service.catalogue.pojo;

import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
public class ResourceDTO {
    private String credentialSubjectId;
    private String type;
    private String name;
    private String description;
    private Boolean containsPII;
    private String producedBy;
    private Set<String> exposedThrough = new HashSet<>();
    private Set<String> license = new HashSet<>();
    private Set<String> policies = new HashSet<>();
    private Set<ResourceDTO> aggregationOf = new HashSet<>();
    private Set<ParticipantDTO> maintainedBy = new HashSet<>();
    private Set<ParticipantDTO> ownedBy = new HashSet<>();
    private Set<ParticipantDTO> manufacturedBy = new HashSet<>();
    private Set<ParticipantDTO> copyrightOwnedBy = new HashSet<>();
    private Set<LocationDTO> locationAddress = new HashSet<>();
    private Set<LocationDTO> location = new HashSet<>();
}
