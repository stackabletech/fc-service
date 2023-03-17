package eu.gaiax.difs.fc.server.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties("federated-catalogue.query")
public class QueryProperties {

	private List<String> partners;
	private boolean recursive;
	
	public List<String> getPartners() {
		return partners;
	}

	@Override
	public String toString() {
		return "QueryProperties [partners=" + partners + ", recursive=" + recursive + "]";
	}
	
}

