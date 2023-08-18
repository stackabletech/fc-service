package eu.xfsc.fc.core.service.pubsub.nats;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.xfsc.fc.api.generated.model.SelfDescriptionStatus;
import eu.xfsc.fc.core.pojo.SelfDescriptionMetadata;
import eu.xfsc.fc.core.pojo.VerificationResult;
import eu.xfsc.fc.core.service.pubsub.SDPublisher;
import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import io.nats.client.impl.Headers;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NatsSDPublisherImpl implements SDPublisher {
	
    @Value("${publisher.instance}")
    private String instance;
    @Value("${publisher.subject}")
    private String subject;
    @Value("${publisher.send-content}")
    private boolean sendContent;
	
	@Autowired
	private Connection pubConnection;
	@Autowired
	private ObjectMapper jsonMapper;

	@Override
	public boolean publish(SelfDescriptionMetadata sd, VerificationResult verificationResult) {
		log.debug("publish. sd: {}", sd);
		try {
			Headers headers = new Headers();
			headers.put("source", instance);
			headers.put("event", SDEvent.ADD.name());
			headers.put("status", sd.getStatus().name());
			byte[] body = null;
			if (sendContent) {
			    Map<String, Object> data = Map.of("content", sd.getSelfDescription().getContentAsString(), 
				    "verificationResult", jsonMapper.writeValueAsString(verificationResult));
			    body = jsonMapper.writeValueAsString(data).getBytes();
			}
			pubConnection.jetStream().publish(subject + "." + sd.getSdHash(), headers, body); 
			return true;
		} catch (IOException | JetStreamApiException ex) {
			log.error("publish.error", ex);
		}
		return false;
	}

	@Override
	public boolean publish(String hash, SDEvent event, SelfDescriptionStatus status) {
		log.debug("publish. hash: {}, event: {}, status: {}", hash, event, status);
		try {
			Headers headers = new Headers();
			headers.put("source", instance);
			headers.put("event", event.name());
			if (status != null) {
				headers.put("status", status.name());
			}
			pubConnection.jetStream().publish(subject + "." + hash, headers, null);
			return true;
		} catch (IOException | JetStreamApiException ex) {
			log.error("publish.error", ex);
		}
		return false;
	}

}
