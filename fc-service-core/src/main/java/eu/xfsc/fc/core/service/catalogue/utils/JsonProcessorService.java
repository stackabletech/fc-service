package eu.xfsc.fc.core.service.catalogue.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.xfsc.fc.core.service.catalogue.participant.ParticipantService;
import eu.xfsc.fc.core.service.catalogue.pojo.LocationDTO;
import eu.xfsc.fc.core.service.catalogue.pojo.ParticipantDTO;
import eu.xfsc.fc.core.service.catalogue.pojo.TermsAndConditionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static eu.xfsc.fc.core.service.catalogue.utils.constant.CESConstant.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class JsonProcessorService {

    private final ParticipantService participantService;
    private final ObjectMapper mapper;

    public void processParticipant(JSONArray participants, Set<ParticipantDTO> dto, Map<String, JSONObject> vcMap) {
        if (Objects.isNull(participants)) {
            return;
        }
        for (Object p : participants) {
            Object id = ((JSONObject) p).get(ID);
            JSONObject participantVc = vcMap.get(id);
            if (Objects.isNull(participantVc)) {
                List<ParticipantDTO> dtos = participantService.processParticipantCredential(id.toString());
                if (!CollectionUtils.isEmpty(dtos)) {
                    dto.addAll(dtos);
                }
            } else {
                ParticipantDTO participantDto = participantService.createParticipantDto(participantVc, vcMap);
                if (Objects.nonNull(participantDto)) {
                    dto.add(participantDto);
                }
            }
        }
    }

    public void processLocation(JSONArray locations, Set<LocationDTO> dto) {
        if (Objects.isNull(locations)) {
            return;
        }
        locations.forEach(l -> {
            JSONObject location = (JSONObject) l;
            dto.add(new LocationDTO(location.optString(GX_COUNTRY_CODE), location.optString(GX_GPS)));
        });
    }

    public void processStringJsonObject(JSONArray licenses, Set<String> dto) {
        if (Objects.isNull(licenses)) {
            return;
        }
        licenses.forEach(l -> {
            JSONObject license = (JSONObject) l;
            dto.add(license.getString(ID));
        });
    }

    public void processString(JSONArray licenses, Set<String> dto) {
        if (Objects.isNull(licenses)) {
            return;
        }
        licenses.forEach(l -> dto.add((String) l));
    }

    public TermsAndConditionDTO processForTnc(JSONObject tnc) {
        return new TermsAndConditionDTO(tnc.optString(GX_URL), tnc.optString(GX_HASH));
    }

    public JSONObject parseJson(String json, String format) {
        try {
            return mapper.convertValue(json, JSONObject.class);
        } catch (Exception ex) {
            log.error(format, json);
            throw ex;
        }
    }

}
