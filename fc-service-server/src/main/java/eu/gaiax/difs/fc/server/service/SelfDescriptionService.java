package eu.gaiax.difs.fc.server.service;

import eu.gaiax.difs.fc.api.generated.model.SelfDescription;
import eu.gaiax.difs.fc.api.generated.model.SelfDescription.StatusEnum;
import eu.gaiax.difs.fc.server.generated.controller.SelfDescriptionsApiDelegate;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SelfDescriptionService implements SelfDescriptionsApiDelegate {
    // TODO: 13.07.2022 Need to replace mocked Data with business logic

    @Override
    public ResponseEntity<List<SelfDescription>> readSelfDescriptions(String uploadTimeRange, String statusTimeRange,
                                                                      String issuer, String validator,
                                                                      String statusValue, String id, String hash,
                                                                      Integer offset, Integer limit) {
        log.debug("readSelfDescriptions.enter; got uploadTimeRange: {}, statusTimeRange: {},"
                        + "issuer: {}, validator: {}, status: {}, id: {}, hash: {}, offset: {}, limit: {}",
                uploadTimeRange, statusTimeRange, issuer, validator, statusValue, id, hash, offset, limit);
        List<SelfDescription> selfDescriptions = new ArrayList<>();
        selfDescriptions.add(getDefaultSdMetadata());
        log.debug("readSelfDescriptions.exit; returning: {}", selfDescriptions.size());
        return new ResponseEntity<>(selfDescriptions, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Object> readSelfDescriptionByHash(String selfDescriptionHash) {
        log.debug("readSelfDescriptionByHash.enter; got hash: {}", selfDescriptionHash);

        String sd = "{\n"
                + " \"@context\": \"https://json-ld.org/contexts/gaia-x.jsonld\",\n"
                + " \"@id\": \"http://dbpedia.org/resource/MyService\",\n"
                + " \"provider\": \"http://dbpedia.org/resource/MyProvider\",\n"
                + " \"name\": \"https://gaia-x.catalogue.com\"\n"
                + " }";

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("Content-Type", "application/ld+json");

        log.debug("readSelfDescriptionByHash.exit; returning self-description by hash: {}", selfDescriptionHash);
        return ResponseEntity.ok()
                .headers(responseHeaders)
                .body(sd);
    }

    @Override
    public ResponseEntity<Void> deleteSelfDescription(String selfDescriptionHash) {
        log.debug("deleteSelfDescription.enter; got hash: {}", selfDescriptionHash);

        log.debug("deleteSelfDescription.exit; deleted self-description by hash: {}", selfDescriptionHash);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<SelfDescription> addSelfDescription(Object selfDescription) {
        log.debug("addSelfDescription.enter; got selfDescription: {}", selfDescription);

        SelfDescription sdMetadata = getDefaultSdMetadata();

        log.debug("addSelfDescription.exit; returning self-description metadata by hash: {}", sdMetadata.getSdHash());
        return new ResponseEntity<>(sdMetadata, HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<SelfDescription> updateSelfDescription(String selfDescriptionHash) {
        log.debug("updateSelfDescription.enter; got hash: {}", selfDescriptionHash);

        SelfDescription sdMetadata = getDefaultSdMetadata();

        log.debug("updateSelfDescription.exit; update self-description by hash: {}", selfDescriptionHash);
        return new ResponseEntity<>(sdMetadata, HttpStatus.OK);
    }

    private SelfDescription getDefaultSdMetadata() {
        SelfDescription sdMetadata = new SelfDescription();
        sdMetadata.setId("string");
        sdMetadata.setSdHash("string");
        sdMetadata.setIssuer("string");
        sdMetadata.setStatus(StatusEnum.ACTIVE);
        List<String> validators = new ArrayList<>();
        validators.add("string");
        sdMetadata.setValidators(validators);
        sdMetadata.setStatusTime("2022-05-11T15:30:00Z");
        sdMetadata.setUploadTime("2022-03-01T13:00:00Z");
        return sdMetadata;
    }
}