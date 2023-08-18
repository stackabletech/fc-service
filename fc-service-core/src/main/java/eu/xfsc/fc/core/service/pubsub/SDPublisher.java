package eu.xfsc.fc.core.service.pubsub;

import eu.xfsc.fc.api.generated.model.SelfDescriptionStatus;
import eu.xfsc.fc.core.pojo.SelfDescriptionMetadata;
import eu.xfsc.fc.core.pojo.VerificationResult;

public interface SDPublisher {
	
	enum SDEvent {ADD, DELETE, UPDATE};
	
	boolean publish(SelfDescriptionMetadata sd, VerificationResult verificationResult);
	boolean publish(String hash, SDEvent event, SelfDescriptionStatus status);

}
