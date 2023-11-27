package eu.xfsc.fc.core.service.catalogue.serviceoffer;

import eu.xfsc.fc.core.dao.catalogue.node.*;
import eu.xfsc.fc.core.dao.catalogue.node.repository.ServiceOfferRepository;
import eu.xfsc.fc.core.service.catalogue.labellevel.LabelLevelService;
import eu.xfsc.fc.core.service.catalogue.participant.ParticipantService;
import eu.xfsc.fc.core.service.catalogue.participant.TermsAndConditionService;
import eu.xfsc.fc.core.service.catalogue.pojo.DataAccountExportDTO;
import eu.xfsc.fc.core.service.catalogue.pojo.ResourceDTO;
import eu.xfsc.fc.core.service.catalogue.pojo.ServiceOfferDTO;
import eu.xfsc.fc.core.service.catalogue.pojo.TermsAndConditionDTO;
import eu.xfsc.fc.core.service.catalogue.resource.ResourceService;
import eu.xfsc.fc.core.service.catalogue.utils.InvokeService;
import eu.xfsc.fc.core.service.catalogue.utils.JsonProcessorService;
import eu.xfsc.fc.core.service.catalogue.utils.constant.CESConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

import static eu.xfsc.fc.core.service.catalogue.utils.constant.CESConstant.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceOfferService {

    private final ServiceOfferRepository serviceOfferRepository;
    private final JsonProcessorService processorService;
    private final ParticipantService participantService;
    private final TermsAndConditionService termsAndConditionService;
    private final ResourceService resourceService;
    private final LabelLevelService labelLevelService;

    public void processServiceOfferCredential(String serviceOfferUrl) {
        String serviceOfferSd = InvokeService.executeRequest(serviceOfferUrl, HttpMethod.GET);
        JSONObject sdJson = processorService.parseJson(serviceOfferSd, "Not able to parse service offer selfDescription {}");
        JSONArray verifiableCredentials = sdJson.getJSONObject(SELF_DESCRIPTION_CREDENTIAL).getJSONArray(VERIFIABLE_CREDENTIAL);
        Map<String, JSONObject> vcMap = new HashMap<>();
        for (Object vcs : verifiableCredentials) {
            JSONObject vc = (JSONObject) vcs;
            JSONObject credentialSubject = vc.getJSONObject(CREDENTIAL_SUBJECT);
            vcMap.put(credentialSubject.getString(CESConstant.ID), vc);
        }
        for (Map.Entry<String, JSONObject> map : vcMap.entrySet()) {
            ingestServiceOffer(map.getValue(), vcMap);
        }
    }

    private ServiceOfferDTO ingestServiceOffer(JSONObject vc, Map<String, JSONObject> vcMap) {
        if (Objects.isNull(vc)) {
            return null;
        }
        JSONObject credentialSubject = vc.getJSONObject(CREDENTIAL_SUBJECT);
        String type = credentialSubject.getString(TYPE);
        if (!Objects.equals(type, GX_SERVICE_OFFERING)) {
            return null;
        }
        String participantUrl = credentialSubject.getJSONObject(GX_PROVIDED_BY).getString(ID);
        String serviceOfferUrl = credentialSubject.getString(ID);
        ServiceOfferDTO offer = new ServiceOfferDTO();
        offer.setCredentialSubjectId(serviceOfferUrl);
        offer.setName(credentialSubject.optString(GX_NAME));
        offer.setDescription(credentialSubject.optString(GX_DESCRIPTION));
        offer.setTnc(processorService.processForTnc(credentialSubject.getJSONObject(GX_TERMS_AND_CONDITIONS)));
        JSONObject dataAccountExport = credentialSubject.getJSONObject(GX_DATA_ACCOUNT_EXPORT);
        JSONArray formatTypes = dataAccountExport.getJSONArray(GX_FORMAT_TYPE);
        HashSet<String> formats = new HashSet<>();
        processorService.processString(formatTypes, formats);
        offer.setDataAccountExport(new DataAccountExportDTO(dataAccountExport.getString(GX_REQUEST_TYPE), dataAccountExport.getString(GX_ACCESS_TYPE), formats));
        processorService.processString(credentialSubject.optJSONArray(GX_DATA_PROTECTION_REGIME), offer.getDataProtectionRegime());
        processorService.processString(credentialSubject.optJSONArray(GX_POLICY), offer.getPolicy());
        JSONObject participantVc = vcMap.get(participantUrl);
        offer.setProvidedBy(participantService.createParticipantDto(participantVc, vcMap));
        processOnDependsService(vcMap, offer, credentialSubject.optJSONArray(GX_DEPENDS_ON));
        processOnResource(vcMap, offer, credentialSubject.optJSONArray(GX_AGGREGATION_OF));
        offer.setLabelLevel(labelLevelService.getLabelLevel(credentialSubject, vcMap));
        create(offer);
        return offer;
    }

    private void processOnDependsService(Map<String, JSONObject> vcMap, ServiceOfferDTO offer, JSONArray dependsOnServices) {
        if (Objects.isNull(dependsOnServices)) {
            return;
        }
        dependsOnServices.forEach(s -> {
            JSONObject service = (JSONObject) s;
            JSONObject dependedService = vcMap.get(service.getString(ID));
            ServiceOfferDTO dto = ingestServiceOffer(dependedService, vcMap);
            if (Objects.nonNull(dto)) {
                offer.getDependsOn().add(dto);
            }
        });
    }

    private void processOnResource(Map<String, JSONObject> vcMap, ServiceOfferDTO offer, JSONArray aggregationResource) {
        if (Objects.isNull(aggregationResource)) {
            return;
        }
        aggregationResource.forEach(r -> {
            JSONObject aggs = (JSONObject) r;
            String id = aggs.getString(ID);
            JSONObject resourceVc = vcMap.get(id);
            if (Objects.isNull(resourceVc)) {
                List<ResourceDTO> resources = resourceService.ingestResource(id);
                if (!CollectionUtils.isEmpty(resources)) {
                    offer.getAggregationOf().addAll(resources);
                }
            } else {
                ResourceDTO resource = resourceService.ingestResource(resourceVc, vcMap);
                if (Objects.nonNull(resource)) {
                    offer.getAggregationOf().add(resource);
                }
            }
        });
    }

    private void create(ServiceOfferDTO dto) {
        NServiceOffer offer = create(dto.getCredentialSubjectId(), dto.getDescription(), dto.getName(), dto.getPolicy(), dto.getTnc(), dto.getLabelLevel(),
                dto.getDataAccountExport(), dto.getDataProtectionRegime(), participantService.create(dto.getProvidedBy()),
                dto.getVeracity(), dto.getTrustIndex(), dto.getTransparency(), dto.getLocations());
        for (ServiceOfferDTO dependsOn : dto.getDependsOn()) {
            if (CollectionUtils.isEmpty(offer.getDependedServices())) {
                offer.setDependedServices(new HashSet<>());
            }
            offer.getDependedServices().add(create(dependsOn.getCredentialSubjectId(), dependsOn.getDescription(), dependsOn.getName(), dependsOn.getPolicy(), dependsOn.getTnc(), dependsOn.getLabelLevel(),
                    dependsOn.getDataAccountExport(), dependsOn.getDataProtectionRegime(), participantService.create(dependsOn.getProvidedBy()),
                    dependsOn.getVeracity(), dependsOn.getTrustIndex(), dependsOn.getTransparency(), dependsOn.getLocations()));
        }

        for (ResourceDTO resource : dto.getAggregationOf()) {
            if (CollectionUtils.isEmpty(offer.getResources())) {
                offer.setResources(new HashSet<>());
            }
            offer.getResources().add(resourceService.create(resource));
        }
        serviceOfferRepository.save(offer);
    }

    private NServiceOffer create(String credentialSubjectId, String description, String name, Set<String> policyUrl, TermsAndConditionDTO tnc, String labelLevel,
                                 DataAccountExportDTO dataAccountExport, Set<String> dataProtectionRegimes, NParticipant providedBy,
                                 Double veracity, Double trustIndex, Double transparency, Set<NAddress> locations) {
        NServiceOffer serviceOffer = serviceOfferRepository.getByCredentialSubjectId(credentialSubjectId);
        if (Objects.nonNull(serviceOffer)) {
            return serviceOffer;
        }
        Set<NDataProtectionRegime> regimes = dataProtectionRegimes.stream().map(regime -> {
            NDataProtectionRegime protectionRegime = new NDataProtectionRegime();
            protectionRegime.setName(regime);
            return protectionRegime;
        }).collect(Collectors.toSet());
        serviceOffer = NServiceOffer.builder()
                .credentialSubjectId(credentialSubjectId)
                .policy(policyUrl)
                .description(description)
                .labelLevel(labelLevel)
                .protectionRegime(CollectionUtils.isEmpty(regimes) ? null : regimes)
                .dataAccountExport(NDataAccountExport.builder()
                        .accessType(dataAccountExport.accessType())
                        .formatType(dataAccountExport.formatType())
                        .requestType(dataAccountExport.requestType())
                        .build())
                .termsAndCondition(termsAndConditionService.create(tnc.hash(), tnc.url(), null, false))
                .participant(providedBy)
                .veracity(veracity)
                .trustIndex(trustIndex)
                .transparency(transparency)
                .locations(locations)
                .vc(InvokeService.executeRequest(credentialSubjectId, HttpMethod.GET))
                .build();
        serviceOffer.setName(name);
        serviceOffer.setCreatedAt(new Date());
        return serviceOfferRepository.save(serviceOffer);
    }

}
