package eu.gaiax.difs.fc.server.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.gaiax.difs.fc.server.exception.ParserException;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility parser class for parsing self-descriptions in the specified format.
 */
@Slf4j
public class SelfDescriptionParser {
  private static final ObjectMapper mapper = new ObjectMapper();

  private static final String PARSER_ERROR_MESSAGE = "Self-description doesn't contain information about participant.";

  private static final String[] TYPE_VALUES = new String[] {"gax:Provider", "gax:Consumer", "gax:Federator",
      "gax:FederationService", "gax:ServiceOffering", "gax:Resource", "gax:Asset"};

  /**
   * Public static method for getting the Participant ID from the Self-description.
   *
   * @param selfDescription The verifiable SD (required).
   * @return Returns the Participant ID,
   *        or null if the specified SD doesn't contain Participant ID attribute,
   *        or throws an exception with the appropriate information why the SD wasn't parsed.
   */
  public static String getParticipantIdFromSd(String selfDescription) {
    log.debug("getParticipantIdFromSD.enter; got selfDescription: {}", selfDescription);

    try {
      for (JsonNode v : mapper.readTree(selfDescription).get("verifiableCredential")) {
        JsonNode credentialSubjectNode = v.get("credentialSubject");
        if (Arrays.asList(TYPE_VALUES).contains(credentialSubjectNode.get("@type").textValue())) {
          String participantId = credentialSubjectNode.get("@id").textValue();
          log.debug("getParticipantIdFromSD.exit; returning participantId {}.", participantId);
          return participantId;
        }
      }
    } catch (Exception exception) {
      log.error(PARSER_ERROR_MESSAGE, exception);
      throw new ParserException(PARSER_ERROR_MESSAGE, exception);
    }
    return null;
  }
}