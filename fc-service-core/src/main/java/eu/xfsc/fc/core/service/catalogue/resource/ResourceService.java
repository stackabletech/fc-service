package eu.xfsc.fc.core.service.catalogue.resource;

import eu.xfsc.fc.core.dao.catalogue.node.NAddress;
import eu.xfsc.fc.core.dao.catalogue.node.NParticipant;
import eu.xfsc.fc.core.dao.catalogue.node.NResource;
import eu.xfsc.fc.core.dao.catalogue.node.repository.ResourceRepository;
import eu.xfsc.fc.core.service.catalogue.participant.AddressService;
import eu.xfsc.fc.core.service.catalogue.participant.ParticipantService;
import eu.xfsc.fc.core.service.catalogue.pojo.LocationDTO;
import eu.xfsc.fc.core.service.catalogue.pojo.ParticipantDTO;
import eu.xfsc.fc.core.service.catalogue.pojo.ResourceDTO;
import eu.xfsc.fc.core.service.catalogue.utils.InvokeService;
import eu.xfsc.fc.core.service.catalogue.utils.JsonProcessorService;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

import static eu.xfsc.fc.core.service.catalogue.utils.constant.CESConstant.*;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final ParticipantService participantService;
    private final AddressService addressService;
    private final JsonProcessorService processorService;

    public List<ResourceDTO> ingestResource(String url) {
        String resourceVc = InvokeService.executeRequest(url, HttpMethod.GET);
        JSONObject sdJson = new JSONObject(resourceVc);
        if (Objects.isNull(sdJson)) {
            return null;
        }
        JSONArray vcs = sdJson.getJSONObject(SELF_DESCRIPTION_CREDENTIAL).getJSONArray(VERIFIABLE_CREDENTIAL);
        Map<String, JSONObject> vcMap = new HashMap<>();
        for (Object vc : vcs) {
            JSONObject v = (JSONObject) vc;
            JSONObject credentialSubject = v.getJSONObject(CREDENTIAL_SUBJECT);
            vcMap.put(credentialSubject.getString(ID), v);
        }
        List<ResourceDTO> dtos = new ArrayList<>();
        for (Map.Entry<String, JSONObject> map : vcMap.entrySet()) {
            ResourceDTO dto = ingestResource(map.getValue(), vcMap);
            if (Objects.nonNull(dto) && Objects.equals(url, dto.getCredentialSubjectId())) {
                dtos.add(dto);
            }
        }
        return dtos;
    }

    public ResourceDTO ingestResource(JSONObject vc, Map<String, JSONObject> vcMap) {
        if (Objects.isNull(vc)) {
            return null;
        }
        JSONObject credentialSubject = vc.getJSONObject(CREDENTIAL_SUBJECT);
        String type = credentialSubject.getString(TYPE);
        if (!Objects.equals(type, GX_PHYSICAL_RESOURCE) && !Objects.equals(type, GX_VIRTUAL_SOFTWARE_RESOURCE) && !Objects.equals(type, GX_VIRTUAL_DATA_RESOURCE)) {
            return null;
        }
        ResourceDTO resourceDTO = new ResourceDTO();
        resourceDTO.setType(type);
        resourceDTO.setCredentialSubjectId(credentialSubject.getString(ID));
        resourceDTO.setName(credentialSubject.getString(GX_NAME));
        resourceDTO.setDescription(credentialSubject.optString(GX_DESCRIPTION));
        resourceDTO.setContainsPII(credentialSubject.optBoolean(GX_CONTAINS_PII));

        processorService.processParticipant(credentialSubject.optJSONArray(GX_MAINTAINED_BY), resourceDTO.getMaintainedBy(), vcMap);
        processorService.processParticipant(credentialSubject.optJSONArray(GX_OWNED_BY), resourceDTO.getOwnedBy(), vcMap);
        processorService.processParticipant(credentialSubject.optJSONArray(GX_MANUFACTURED_BY), resourceDTO.getManufacturedBy(), vcMap);
        processorService.processParticipant(credentialSubject.optJSONArray(GX_COPYRIGHT_OWNED_BY), resourceDTO.getCopyrightOwnedBy(), vcMap);

        processorService.processLocation(credentialSubject.optJSONArray(GX_LOCATION_ADDRESS), resourceDTO.getLocationAddress());
        processorService.processLocation(credentialSubject.optJSONArray(GX_LOCATION), resourceDTO.getLocation());

        processorService.processString(credentialSubject.optJSONArray(GX_LICENSE), resourceDTO.getLicense());
        processorService.processString(credentialSubject.optJSONArray(GX_POLICY), resourceDTO.getPolicies());
        processorService.processString(credentialSubject.optJSONArray(GX_EXPOSED_THROUGH), resourceDTO.getExposedThrough());

        JSONObject producedBy = credentialSubject.optJSONObject(GX_PRODUCED_BY);
        if (Objects.nonNull(producedBy)) {
            resourceDTO.setProducedBy(producedBy.getString(ID));
        }
        JSONArray aggregationResource = credentialSubject.optJSONArray(GX_AGGREGATION_OF);
        if (Objects.nonNull(aggregationResource)) {
            aggregationResource.forEach(ag -> {
                JSONObject aggregateResource = (JSONObject) ag;
                ResourceDTO resource = ingestResource(vcMap.get(aggregateResource.get(ID)), vcMap);
                if (Objects.nonNull(resource)) {
                    resourceDTO.getAggregationOf().add(resource);
                }
            });
        }
        create(resourceDTO);
        return resourceDTO;
    }

    public NResource create(ResourceDTO dto) {
        return create(dto.getCredentialSubjectId(), dto.getType(), dto.getName(), dto.getDescription(), dto.getPolicies(),
                dto.getLicense(), processOnAggregateResource(dto.getAggregationOf()),
                dto.getExposedThrough(), dto.getProducedBy(),
                processOnParticipant(dto.getMaintainedBy()), processOnParticipant(dto.getOwnedBy()), processOnParticipant(dto.getManufacturedBy()), processOnParticipant(dto.getCopyrightOwnedBy()),
                processOnLocation(dto.getLocationAddress()), processOnLocation(dto.getLocation()),
                dto.getContainsPII());
    }

    private NResource create(String credentialSubjectId, String type, String name, String description, Set<String> policy,
                             Set<String> licenses, Set<NResource> aggregateResources,
                             Set<String> exposedDataResource, String producedBy,
                             Set<NParticipant> maintainer, Set<NParticipant> owner, Set<NParticipant> manufacturer, Set<NParticipant> copyrightOwnedBy,
                             Set<NAddress> address, Set<NAddress> location,
                             boolean containsPII) {
        NResource resource = resourceRepository.getByCredentialSubjectId(credentialSubjectId);
        if (Objects.nonNull(resource)) {
            return resource;
        }
        resource = NResource.builder()
                .credentialSubjectId(credentialSubjectId)
                .type(type)
                .description(description)
                .policy(policy)
                .containsPII(containsPII)
                .producedBy(producedBy)
                .license(licenses)
                .exposedThroughResource(exposedDataResource)
                .aggregationResource(aggregateResources)
                .copyRightOwnedBy(copyrightOwnedBy)
                .maintainedBy(maintainer)
                .manufacturedBy(manufacturer)
                .ownedBy(owner)
                .location(location)
                .locationAddress(address)
                .vc(InvokeService.executeRequest(credentialSubjectId, HttpMethod.GET))
                .build();
        resource.setName(name);
        resource.setCreatedAt(new Date());
        return resourceRepository.save(resource);
    }

    private Set<NParticipant> processOnParticipant(Set<ParticipantDTO> participants) {
        if (CollectionUtils.isEmpty(participants)) {
            return Collections.emptySet();
        }
        return participants.stream().map(participantService::create).collect(Collectors.toSet());
    }

    private Set<NAddress> processOnLocation(Set<LocationDTO> location) {
        if (CollectionUtils.isEmpty(location)) {
            return Collections.emptySet();
        }
        return location.stream().map(addressService::create).collect(Collectors.toSet());
    }

    private Set<NResource> processOnAggregateResource(Set<ResourceDTO> resources) {
        if (CollectionUtils.isEmpty(resources)) {
            return Collections.emptySet();
        }
        return resources.stream().map(this::create).collect(Collectors.toSet());
    }
}
