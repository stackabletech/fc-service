package eu.gaiax.difs.fc.server.service;

import static eu.gaiax.difs.fc.server.util.SelfDescriptionParser.getParticipantIdFromSD;
import static eu.gaiax.difs.fc.server.util.SessionUtils.getSessionParticipantId;

import eu.gaiax.difs.fc.api.generated.model.SelfDescription;
import eu.gaiax.difs.fc.api.generated.model.SelfDescription.StatusEnum;
import eu.gaiax.difs.fc.server.exception.ClientException;
import eu.gaiax.difs.fc.server.exception.ParserException;
import eu.gaiax.difs.fc.server.generated.controller.SelfDescriptionsApiDelegate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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

        String selfDescription = getSelfDescriptionByHash(selfDescriptionHash);
        checkParticipantAccess(selfDescription);

        log.debug("deleteSelfDescription.exit; deleted self-description by hash: {}", selfDescriptionHash);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<SelfDescription> addSelfDescription(String selfDescription) {
        log.debug("addSelfDescription.enter; got selfDescription: {}", selfDescription);

        checkParticipantAccess(selfDescription);
        SelfDescription sdMetadata = getDefaultSdMetadata();

        log.debug("addSelfDescription.exit; returning self-description metadata by hash: {}", sdMetadata.getSdHash());
        return new ResponseEntity<>(sdMetadata, HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<SelfDescription> updateSelfDescription(String selfDescriptionHash) {
        log.debug("updateSelfDescription.enter; got hash: {}", selfDescriptionHash);

        String selfDescription = getSelfDescriptionByHash(selfDescriptionHash);
        checkParticipantAccess(selfDescription);

        SelfDescription sdMetadata = getDefaultSdMetadata();

        log.debug("updateSelfDescription.exit; update self-description by hash: {}", selfDescriptionHash);
        return new ResponseEntity<>(sdMetadata, HttpStatus.OK);
    }

    private void checkParticipantAccess(String selfDescription) {
        String sdParticipantId;
        try {
            sdParticipantId = getParticipantIdFromSD(selfDescription);
        } catch (ParserException exception) {
            log.debug(exception.getMessage(), exception);
            throw new ClientException(exception.getMessage());
        }
        String sessionParticipantId = getSessionParticipantId();
        if (Objects.isNull(sdParticipantId) || Objects.isNull(sessionParticipantId)
                || !sdParticipantId.equals(sessionParticipantId)) {
            log.debug("checkParticipantAccess; The user does not have access to the specified participant."
                            + " User participant id = {}, self-description participant id = {}.",
                    sessionParticipantId, sdParticipantId);
            throw new AccessDeniedException("The user does not have access to the specified participant.");
        }
    }

    // TODO: 20.07.2022 The logic must be implemented by Fraunhofer. Then the mock implementation will be changed.
    private String getSelfDescriptionByHash(String sdHash) {
        return "{\n"
                + "  \"@context\": [\n"
                + "    \"https://w3id.org/gaia-x/context.jsonld\"\n"
                + "  ],\n"
                + "  \"type\": \"VerifiablePresentation\",\n"
                + "  \"verifiableCredential\": [\n"
                + "    {\n"
                + "      \"@context\": [\n"
                + "        \"https://w3id.org/gaia-x/context.jsonld\"\n"
                + "      ],\n"
                + "      \"id\": \"http://example.edu/credentials/1872\",\n"
                + "      \"type\": \"VerifiableCredential\",\n"
                + "      \"issuer\": \"https://www.handelsregister.de/\",\n"
                + "      \"issuanceDate\": \"2010-01-01T19:73:24Z\",\n"
                + "      \"credentialSubject\": {\n"
                + "        \"@id\": \"http://example.org/test-provider\",\n"
                + "        \"@type\": \"gax:Provider\",\n"
                + "        \"gax:hasLegallyBindingName\": \"My example provider\"\n"
                + "      },\n"
                + "      \"proof\": {\n"
                + "        \"type\": \"RsaSignature2018\",\n"
                + "        \"created\": \"2017-06-18T21:19:10Z\",\n"
                + "        \"proofPurpose\": \"assertionMethod\",\n"
                + "        \"verificationMethod\": \"https://example.edu/issuers/keys/1\",\n"
                + "        \"jws\": \"eyJhbGciOiJSUzI1NiIsImI2NCI6ZmFsc2UsImNyaXQiOlsiYjY0Il19\"\n"
                + "      }\n"
                + "    }\n"
                + "  ],\n"
                + "  \"proof\": {\n"
                + "    \"type\": \"RsaSignature2018\",\n"
                + "    \"created\": \"2018-09-14T21:19:10Z\",\n"
                + "    \"proofPurpose\": \"authentication\",\n"
                + "    \"verificationMethod\": \"did:example:ebfeb1f712ebc6f1c276e12ec21#keys1\",\n"
                + "    \"challenge\": \"1f44d55f-f161-4938-a659-f8026467f126\",\n"
                + "    \"domain\": \"4jt78h47fh47\",\n"
                + "    \"jws\": \"eyJhbGciOiJSUzI1NiIsImI2NCI6ZmFsc2UsImNyaXQiOlsiYjY0Il19\"\n"
                + "  }\n"
                + "}\n";
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