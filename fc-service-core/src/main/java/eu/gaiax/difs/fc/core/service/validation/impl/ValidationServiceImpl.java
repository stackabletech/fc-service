package eu.gaiax.difs.fc.core.service.validation.impl;

import eu.gaiax.difs.fc.api.generated.model.Participant;
import eu.gaiax.difs.fc.api.generated.model.SelfDescription;
import eu.gaiax.difs.fc.api.generated.model.VerificationResult;
import eu.gaiax.difs.fc.core.service.validation.ValidationService;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.validation.ValidationException;
import org.springframework.stereotype.Service;

// TODO: 26.07.2022 Awaiting approval and implementation by Fraunhofer.
/**
 * Implementation of the {@link ValidationService} interface.
 */
@Service
public class ValidationServiceImpl implements ValidationService {
  /**
   * The function validates the Self-Description as JSON and tries to parse the json handed over.
   *
   * @param json The json which should be syntactically validated.
   * @return a Participant metadata validation result. If the validation fails, the reason explains the issue.
   */
  @Override
  public Participant validateParticipantSelfDescription(String json) throws ValidationException {
    return null;
  }

  /**
   * The function validates the Self-Description as JSON and tries to parse the json handed over.
   *
   * @param json The json which should be syntactically validated.
   * @return a Verification result. If the verification fails, the reason explains the issue.
   */
  @Override
  public VerificationResult verifySelfDescription(String json) throws ValidationException {
    return null;
  }

  /**
   * The function validates the Self-Description as JSON and tries to parse the json handed over.
   *
   * @param json The json which should be syntactically validated.
   * @return a Self-Description metadata validation result. If the validation fails, the reason explains the issue.
   */
  @Override
  public SelfDescription validateSelfDescription(String json) throws ValidationException {
    SelfDescription sdMetadata = new SelfDescription();
    sdMetadata.setId("string");
    sdMetadata.setSdHash("string");
    sdMetadata.setIssuer("http://example.org/test-provider");
    sdMetadata.setStatus(SelfDescription.StatusEnum.ACTIVE);
    List<String> validators = new ArrayList<>();
    validators.add("string");
    sdMetadata.setValidators(validators);
    sdMetadata.setStatusDatetime(OffsetDateTime.parse("2022-05-11T15:30:00Z"));
    sdMetadata.setUploadDatetime(OffsetDateTime.parse("2022-03-01T13:00:00Z"));
    return sdMetadata;
  }
}
