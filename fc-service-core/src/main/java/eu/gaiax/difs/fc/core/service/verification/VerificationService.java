package eu.gaiax.difs.fc.core.service.verification;

import eu.gaiax.difs.fc.core.exception.VerificationException;
import eu.gaiax.difs.fc.core.pojo.ContentAccessor;
import eu.gaiax.difs.fc.core.pojo.SemanticValidationResult;
import eu.gaiax.difs.fc.core.pojo.VerificationResult;
import eu.gaiax.difs.fc.core.pojo.Validator;
import eu.gaiax.difs.fc.core.pojo.VerificationResultOffering;
import eu.gaiax.difs.fc.core.pojo.VerificationResultParticipant;
import org.springframework.stereotype.Service;

/**
 * Validation Self-Description interface.
 */
@Service
public interface VerificationService {

  /**
   * The function validates the Self-Description as JSON and tries to parse the json handed over.
   *
   * @param payload ContentAccessor to SD which should be syntactically validated.
   * @return a Participant metadata validation result. If the validation fails, the reason explains the issue.
   */
  VerificationResultParticipant verifyParticipantSelfDescription(ContentAccessor payload) throws VerificationException;

  /**
   * The function validates the Self-Description as JSON and tries to parse the json handed over.
   *
   * @param payload ContentAccessor to SD which should be syntactically validated.
   * @return a Verification result. If the verification fails, the reason explains the issue.
   */
  VerificationResultOffering verifyOfferingSelfDescription(ContentAccessor payload) throws VerificationException;

  /**
   * The function validates the Self-Description as JSON and tries to parse the json handed over.
   *
   * @param payload ContentAccessor to SD which should be syntactically validated.
   * @return a Self-Description metadata validation result. If the validation fails, the reason explains the issue.
   */
  VerificationResult verifySelfDescription(ContentAccessor payload) throws VerificationException;

  /**
   * The function validates the Self-Description against the composite schema.
   *
   * @param payload ContentAccessor to SD which should be validated.
   * @return the result of the semantic validation.
   */
  SemanticValidationResult verifySelfDescriptionAgainstCompositeSchema(ContentAccessor payload);

  /**
   * The function validates the Self-Description as JSON and tries to parse the json handed over.
   *
   * @param payload
   * @param verifySemantics
   * @param verifySchema
   * @param verifySignatures
   * @return
   * @throws VerificationException
   */
  VerificationResult verifySelfDescription(ContentAccessor payload,
      boolean verifySemantics, boolean verifySchema, boolean verifySignatures) throws VerificationException;
}
