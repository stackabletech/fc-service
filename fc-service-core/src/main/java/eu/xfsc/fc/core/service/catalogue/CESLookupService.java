package eu.xfsc.fc.core.service.catalogue;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.xfsc.fc.core.dao.catalogue.CESTrackerDao;
import eu.xfsc.fc.core.service.catalogue.serviceoffer.ServiceOfferService;
import eu.xfsc.fc.core.service.catalogue.utils.InvokeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static eu.xfsc.fc.core.dao.catalogue.ECesStatus.*;
import static eu.xfsc.fc.core.service.catalogue.utils.constant.CESConstant.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CESLookupService {

    private final ObjectMapper mapper;
    private final CESTrackerDao cesTracker;
    private final CESClient cesClient;
    private final ServiceOfferService serviceOfferService;

    @Scheduled(cron = "${scheduler.ces.lookup.cron}")
    public void cesLookup() {
        String lastReceivedEventId = cesTracker.fetchLastIngestedEvent();
        List<Object> cesCredentials = cesClient.fetchCredentials(lastReceivedEventId, 0L, 10L);
        if (CollectionUtils.isEmpty(cesCredentials)) {
            log.debug("Not able to find any credentials from the CES server.");
            return;
        }
        JSONArray credentials = new JSONArray(cesCredentials);
        try {
            credentials.forEach(c -> {
                initIngestionProcess((JSONObject) c);
            });
            log.info("Ingestion process has been completed.");
        } catch (Exception ex) {
            log.error("credential-event is failed for the data which lastReceivedId {}", lastReceivedEventId, ex);
        }
    }

    private void initIngestionProcess(JSONObject credential) {
        String receivedId = credential.getString(ID);
        log.info("Ingestion process has been started for id {}", receivedId);
        cesTracker.create(receivedId, IN_PROGRESS.getId(), null, credential.toString());
        try {
            JSONArray credentialSubject = credential.getJSONObject(DATA).getJSONArray(CREDENTIAL_SUBJECT);
            List<String> sdUrls = new ArrayList<>();
            credentialSubject.forEach(cs -> sdUrls.add(((JSONObject) cs).getString(ID)));
            for (String url : sdUrls) {
                //TODO need to handle the urls and persist the data into the neo4j database
                checkSelfDescriptionType(receivedId, url);
                cesTracker.create(receivedId, DONE.getId(), null, credential.toString());
            }
            log.info("Ingestion process has been completed for id {}", receivedId);
        } catch (Exception ex) {
            cesTracker.create(receivedId, FAILED.getId(), ex.getMessage(), credential.toString());
        }
    }

    private void checkSelfDescriptionType(String cesId, String url) {
        try {
            String sdJson = InvokeService.executeRequest(url, HttpMethod.GET);
            JSONObject sd = new JSONObject(sdJson);
            JSONArray verifiableCredentials = sd.getJSONObject(SELF_DESCRIPTION_CREDENTIAL).getJSONArray(VERIFIABLE_CREDENTIAL);
            for (Object vcs : verifiableCredentials) {
                JSONObject vc = (JSONObject) vcs;
                JSONObject credentialSubject = vc.getJSONObject(CREDENTIAL_SUBJECT);
                if (!Objects.equals(credentialSubject.getString(ID), url)) {
                    continue;
                }
                String type = credentialSubject.getString(TYPE);
                if (!Objects.equals(type, GX_SERVICE_OFFERING)) {
                    continue;
                }
                serviceOfferService.processServiceOfferCredential(url);
            }
        } catch (Exception ex) {
            log.error("credential-event id {} and url {} does not contain the self description.", cesId, url, ex);
            throw ex;
        }
    }
}
