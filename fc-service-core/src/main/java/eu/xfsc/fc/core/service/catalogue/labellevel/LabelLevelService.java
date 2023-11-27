package eu.xfsc.fc.core.service.catalogue.labellevel;

import eu.xfsc.fc.core.service.catalogue.utils.InvokeService;
import org.json.JSONObject;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Objects;

import static eu.xfsc.fc.core.service.catalogue.utils.constant.CESConstant.CREDENTIAL_SUBJECT;
import static eu.xfsc.fc.core.service.catalogue.utils.constant.CESConstant.GX_LABEL_LEVEL;

@Service
public class LabelLevelService {

    private String getLabelLevel(String url) {
        String labelLevel = InvokeService.executeRequest(url, HttpMethod.GET);
        JSONObject vc = new JSONObject(labelLevel);
        return vc.getJSONObject(CREDENTIAL_SUBJECT).getString(GX_LABEL_LEVEL);
    }

    public String getLabelLevel(JSONObject credentialSubject, Map<String, JSONObject> vcMap) {
        String labelLevel = credentialSubject.optString(GX_LABEL_LEVEL);
        if (!StringUtils.hasText(labelLevel)) {
            return null;
        }
        JSONObject vc = vcMap.get(labelLevel);
        if (Objects.isNull(vc)) {
            return getLabelLevel(labelLevel);
        }
        return vc.getJSONObject(CREDENTIAL_SUBJECT).optString(GX_LABEL_LEVEL);
    }
}
