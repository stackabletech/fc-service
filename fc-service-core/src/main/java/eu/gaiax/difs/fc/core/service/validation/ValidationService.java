package eu.gaiax.difs.fc.core.service.validation;

import eu.gaiax.difs.fc.api.generated.model.Participant;
import eu.gaiax.difs.fc.api.generated.model.SelfDescription;
import eu.gaiax.difs.fc.api.generated.model.VerificationResult;
import javax.validation.ValidationException;

import org.springframework.stereotype.Service;

/**
 * Validation Self-Description interface.
 */
//TODO: 26.07.2022 Awaiting approval from Fraunhofer.
@Service
public interface ValidationService {

  /**
   * The function validates the Self-Description as JSON and tries to parse the json handed over.
   *
   * @param json The json which should be syntactically validated.
   * @return a Participant metadata validation result. If the validation fails, the reason explains the issue.
   */
  Participant validateParticipantSelfDescription(String json) throws ValidationException;

  /**
   * The function validates the Self-Description as JSON and tries to parse the json handed over.
   *
   * @param json The json which should be syntactically validated.
   * @return a Verification result. If the verification fails, the reason explains the issue.
   */
  VerificationResult verifySelfDescription(String json) throws ValidationException;

  /**
   * The function validates the Self-Description as JSON and tries to parse the json handed over.
   *
   * @param json The json which should be syntactically validated.
   * @return a Self-Description metadata validation result. If the validation fails, the reason explains the issue.
   */
  SelfDescription validateSelfDescription(String json) throws ValidationException;
}