package eu.xfsc.fc.core.service.catalogue.participant;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.xfsc.fc.core.dao.catalogue.node.NParticipant;
import eu.xfsc.fc.core.dao.catalogue.node.repository.ParticipantRepository;
import eu.xfsc.fc.core.service.catalogue.pojo.ParticipantDTO;
import eu.xfsc.fc.core.service.catalogue.pojo.RegistrationNumberDTO;
import eu.xfsc.fc.core.service.catalogue.utils.InvokeService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;

import static eu.xfsc.fc.core.service.catalogue.utils.constant.CESConstant.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParticipantService {
    private final ObjectMapper mapper;
    private final ParticipantRepository participantRepository;
    private final RegistrationNumberService registrationNumberService;
    private final AddressService addressService;
    private final TermsAndConditionService termsAndConditionService;
    @Value("${gaiax.tnc:123}")
    private String tnc;

    private static List<RegistrationNumberDTO> processOnRegistrationNumber(JSONObject registrationNumberVc, String url) {
        Map<String, String> registrationNumberFields = Map.of("gx:leiCode", LEI_CODE, "gx:vatID", VAT_ID,
                "gx:taxID", TAX_ID, "gx:EORI", EORI, "gx:EUID", EU_ID);
        List<RegistrationNumberDTO> registrationNumberDtos = new ArrayList<>();
        for (Map.Entry<String, String> c : registrationNumberFields.entrySet()) {
            if (Objects.nonNull(registrationNumberVc.opt(c.getKey()))) {
                RegistrationNumberDTO registrationNumberDto = new RegistrationNumberDTO(c.getKey(), (String) registrationNumberVc.opt(c.getKey()), url);
                registrationNumberDtos.add(registrationNumberDto);
            }
        }
        return registrationNumberDtos;
    }

    @SneakyThrows
    public List<ParticipantDTO> processParticipantCredential(String url) {
        String participantSd = InvokeService.executeRequest(url, HttpMethod.GET);
        JSONObject sdJson = parseJson(participantSd, "Not able to parse participant selfDescription {}", "invalid.participant.details");
        JSONArray verifiableCredentials = sdJson.getJSONObject(SELF_DESCRIPTION_CREDENTIAL).getJSONArray(VERIFIABLE_CREDENTIAL);
        Map<String, JSONObject> vcMap = new HashMap<>();
        for (Object vcs : verifiableCredentials) {
            JSONObject vc = (JSONObject) vcs;
            JSONObject credentialSubject = vc.getJSONObject(CREDENTIAL_SUBJECT);
            vcMap.put(credentialSubject.getString(ID), vc);
        }
        List<ParticipantDTO> dtos = new ArrayList<>();
        for (Map.Entry<String, JSONObject> map : vcMap.entrySet()) {
            ParticipantDTO dto = createParticipantDto(map.getValue(), vcMap);
            //TODO need to remove the url.split
            if (Objects.nonNull(dto) && Objects.equals(url.split("/")[3], dto.getCredentialSubjectId().split("/")[3])) {
                dtos.add(dto);
            }
        }
        return dtos;
    }

    public JSONObject parseJson(String json, String format, String message) {
        try {
            return mapper.convertValue(json, JSONObject.class);
        } catch (Exception ex) {
            log.error(format, json);
            throw ex;
        }
    }

    public ParticipantDTO createParticipantDto(JSONObject vc, Map<String, JSONObject> vcMap) {
        if (Objects.isNull(vc)) {
            return null;
        }
        JSONObject credentialSubject = vc.getJSONObject(CREDENTIAL_SUBJECT);
        String type = credentialSubject.getString(TYPE);
        ParticipantDTO participantDto = new ParticipantDTO();
        if (!Objects.equals(type, GX_LEGAL_PARTICIPANT)) {
            return null;
        }
        participantDto.setCredentialSubjectId(credentialSubject.getString(ID));
        participantDto.setIssuer(vc.optString("issuer"));
        participantDto.setLegalName(credentialSubject.getString(GX_LEGAL_NAME));
        participantDto.setHeadQuarterAddress(credentialSubject.optJSONObject(GX_HEADQUARTER_ADDRESS).optString(GX_COUNTRY_SUBDIVISION_CODE));
        participantDto.setLegalAddress(credentialSubject.getJSONObject(GX_LEGAL_ADDRESS).optString(GX_COUNTRY_SUBDIVISION_CODE));
        processParticipantOrganizations(credentialSubject.optJSONArray(GX_PARENT_ORGANIZATION), vcMap, participantDto.getParentOrganizations());
        processParticipantOrganizations(credentialSubject.optJSONArray(GX_SUB_ORGANIZATION), vcMap, participantDto.getSubOrganizations());
        JSONObject registrationNumberVc = vcMap.get(credentialSubject.getJSONObject(GX_REGISTRATION_NUMBER).getString(ID)).getJSONObject(CREDENTIAL_SUBJECT);
        participantDto.setTncContent(tnc);
        participantDto.setGaiaxTnc(true);
        participantDto.setRegistrationNumbers(processOnRegistrationNumber(registrationNumberVc, credentialSubject.getString(ID)));
        create(participantDto);
        return participantDto;
    }

    private void processParticipantOrganizations(JSONArray parentOrganizations, Map<String, JSONObject> vcMap, Set<ParticipantDTO> participantDto) {
        if (Objects.isNull(parentOrganizations)) {
            return;
        }
        parentOrganizations.forEach(org -> {
            JSONObject organization = (JSONObject) org;
            JSONObject participantVc = vcMap.get(organization.getString(ID));
            if (Objects.nonNull(participantVc)) {
                ParticipantDTO dto = createParticipantDto(participantVc, vcMap);
                if (Objects.nonNull(dto)) {
                    participantDto.add(dto);
                }
            }
        });
    }

    public NParticipant create(ParticipantDTO dto) {
        NParticipant participant = create(dto.getCredentialSubjectId(), dto.getIssuer(),
                dto.getLegalName(), dto.getRegistrationNumbers(),
                dto.getHeadQuarterAddress(), dto.getLegalAddress(),
                null, null, dto.getTncContent(), dto.isGaiaxTnc());
        for (ParticipantDTO organization : dto.getParentOrganizations()) {
            if (CollectionUtils.isEmpty(participant.getParentOrganization())) {
                participant.setParentOrganization(new HashSet<>());
            }
            participant.getParentOrganization().add(create(organization.getCredentialSubjectId(), organization.getIssuer(),
                    organization.getLegalName(), organization.getRegistrationNumbers(),
                    organization.getHeadQuarterAddress(), organization.getLegalAddress(),
                    null, null, organization.getTncContent(), organization.isGaiaxTnc()));
        }
        for (ParticipantDTO organization : dto.getSubOrganizations()) {
            if (CollectionUtils.isEmpty(participant.getSubOrganization())) {
                participant.setSubOrganization(new HashSet<>());
            }
            participant.getSubOrganization().add(create(organization.getCredentialSubjectId(), organization.getIssuer(),
                    organization.getLegalName(), organization.getRegistrationNumbers(),
                    organization.getHeadQuarterAddress(), organization.getLegalAddress(),
                    null, null, organization.getTncContent(), organization.isGaiaxTnc()));
        }
        return participantRepository.save(participant);
    }

    private NParticipant create(String credentialSubjectId, String did, String legalName, List<RegistrationNumberDTO> registrationNumbers,
                                String headQuarterCountry, String legalAddressCountry,
                                String tncHash, String tncUrl, String tncContent, boolean gaiaxTnc) {
        NParticipant participant = participantRepository.getByCredentialSubjectId(credentialSubjectId);
        if (Objects.nonNull(participant)) {
            return participant;
        }
        participant = NParticipant.builder()
                .credentialSubjectId(credentialSubjectId)
                .did(did)
                .registrationNumber(registrationNumberService.create(registrationNumbers))
                .headQuarterAddress(addressService.create(headQuarterCountry))
                .legalAddress(addressService.create(legalAddressCountry))
                .termsAndCondition(!gaiaxTnc ? termsAndConditionService.create(tncHash, tncUrl, tncContent, gaiaxTnc) : termsAndConditionService.create(tncHash, tncUrl, tnc, gaiaxTnc))
                .vc(InvokeService.executeRequest(credentialSubjectId, HttpMethod.GET))
                .build();
        participant.setName(legalName);
        participant.setCreatedAt(new Date());
        return participantRepository.save(participant);
    }

}
