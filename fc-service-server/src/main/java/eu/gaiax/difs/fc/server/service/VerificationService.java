package eu.gaiax.difs.fc.server.service;

import eu.gaiax.difs.fc.api.generated.model.VerificationResult;
import eu.gaiax.difs.fc.server.generated.controller.VerificationApiDelegate;
import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class VerificationService implements VerificationApiDelegate {
    // TODO: 18.07.2022 Need to replace mocked Data with business logic

    public ResponseEntity<VerificationResult> verify(Object selfDescription) {
        log.debug("verify.enter; got self-description: {}", selfDescription);

        VerificationResult result = new VerificationResult();
        result.setVerificationTimestamp("string");
        result.setLifecycleStatus("string");
        result.setIssuedDate("string");
        result.setSignatures(new ArrayList<>());
        log.debug("verify.exit; returning result from time {} with status {} of SD {}",
                result.getVerificationTimestamp(), result.getLifecycleStatus(), selfDescription);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    public ResponseEntity<String> verifyPage() {
        log.debug("verifyPage.enter");

        String page = "string";
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("Content-Type", "text/html");

        log.debug("verifyPage.exit; returning page {}", page);
        return ResponseEntity.ok()
                .headers(responseHeaders)
                .body(page);
    }
}
