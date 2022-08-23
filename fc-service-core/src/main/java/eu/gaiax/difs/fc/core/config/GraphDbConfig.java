package eu.gaiax.difs.fc.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Configuration
@PropertySource("classpath:/graphdb.properties")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class GraphDbConfig {

	@Value("${uri}")
	private String uri;
	@Value("${user}")
	private String user;
	@Value("${password}")
	private String password;

}
