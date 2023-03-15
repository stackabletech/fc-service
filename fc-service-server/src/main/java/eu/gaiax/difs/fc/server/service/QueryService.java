package eu.gaiax.difs.fc.server.service;

import eu.gaiax.difs.fc.api.generated.model.QueryLanguage;
import eu.gaiax.difs.fc.api.generated.model.Results;
import eu.gaiax.difs.fc.api.generated.model.Statement;
import eu.gaiax.difs.fc.client.QueryClient;
import eu.gaiax.difs.fc.core.exception.ServerException;
import eu.gaiax.difs.fc.core.pojo.GraphQuery;
import eu.gaiax.difs.fc.core.pojo.PaginatedResults;
import eu.gaiax.difs.fc.core.service.graphdb.GraphStore;
import eu.gaiax.difs.fc.server.config.QueryProperties;
import eu.gaiax.difs.fc.server.generated.controller.QueryApiDelegate;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service for query the catalogue. Implementation of the {@link QueryApiDelegate} .
 */
@Slf4j
@Service
public class QueryService implements QueryApiDelegate {
  
  @Autowired
  private GraphStore graphStore;
  @Autowired
  private ObjectMapper jsonMapper;
  @Autowired
  private ResourceLoader resourceLoader;

  @Autowired
  private QueryProperties queryProps;
  
  private List<QueryClient> queryClients;
  
  @PostConstruct
  public void initClients() {
	log.debug("initClients.enter; props are: {}", queryProps);
	if (queryProps.isRecursive()) {
		queryClients = queryProps.getPartners().stream().map(pAddr -> new QueryClient(pAddr, webClient(pAddr))).collect(Collectors.toList());
	}
	log.debug("initClients.exit; initiated clients: {}", queryClients);
  }
  
  private WebClient webClient(String fcUri) {
      
      //ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2Client = new ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
      //oauth2Client.setDefaultOAuth2AuthorizedClient(true);
      //oauth2Client.setDefaultClientRegistrationId("fc-client-oidc");

      return WebClient.builder()
        //.apply(oauth2Client.oauth2Configuration())
        .baseUrl(fcUri)
        .codecs(configurer -> {
            configurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(jsonMapper, MediaType.APPLICATION_JSON));
            configurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(jsonMapper, MediaType.APPLICATION_JSON));
        })
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build();
  }
  
  /**
   * Get List of results from catalogue for provided {@link Statement}.
   *
   * @param queryLanguage  (required) Language for query the results like openCypher etc.
   * @param statement JSON object to send queries. Use \&quot;application/json\&quot; for openCypher queries.
   *                   A Catalogue may also support the other content types depending on its supported query languages
   *                   but only \&quot;application/json\&quot; is mandatory. (optional)
   * @return List of {@link Results}
   */
  @Override
  public ResponseEntity<Results> query(QueryLanguage queryLanguage, Integer timeout, Boolean withTotalCount, Statement statement) {
    log.debug("query.enter; got queryLanguage: {}, timeout: {}, withTotalCount: {}, statement: {}", queryLanguage, timeout, withTotalCount, statement);
    if (!checkIfLimitPresent(statement) && statement.getStatement().toLowerCase().indexOf("return") != -1) {
      addDefaultLimit(statement);
    }
    Map<String, Results> extra = queryPartners(queryLanguage, timeout, withTotalCount, statement);
    PaginatedResults<Map<String, Object>> queryResultList = graphStore.queryData(new GraphQuery(statement.getStatement(), 
            statement.getParameters(), queryLanguage, timeout, withTotalCount));
    Results result = new Results((int) queryResultList.getTotalCount(), queryResultList.getResults());
   	result = mergePartnerResults(result, extra);
    log.debug("query.exit; returning results: {}", result);
    return ResponseEntity.ok(result);
  }
  
  
  /**
   * {@inheritDoc}
   */
  @Override
  public ResponseEntity<String> querywebsite() {
    log.debug("queryPage.enter");

    final Resource resource = resourceLoader.getResource("classpath:static/query.html");
    String page;
    try {
      Reader reader = new InputStreamReader(resource.getInputStream());
      page = FileCopyUtils.copyToString(reader);
    } catch (IOException e) {
      log.error("queryPage; error in getting file: {}", e);
      throw new ServerException(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
    }
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.set("Content-Type", "text/html");
    log.debug("queryPage.exit; returning page");
    return ResponseEntity.ok()
        .headers(responseHeaders)
        .body(page);
  }

  /**
   * Adding default limit for the query if not present.
   *
   * @param statement Query Statement
   */
  private void addDefaultLimit(Statement statement) {
    String appendLimit = " limit $limit";
    statement.setStatement(statement.getStatement().concat(appendLimit));
    if (null == statement.getParameters()) {
      statement.setParameters(Map.of("limit", 100));
    } else {
      statement.getParameters().putIfAbsent("limit", 100);
    }
  }

  /**
   * Check if limit is present or not in query.
   *
   * @param statement Query Statement
   * @return boolean match status
   */
  private boolean checkIfLimitPresent(Statement statement) {
    String subItem = "limit";
    String pattern = "(?m)(^|\\s)" + subItem + "(\\s|$)";
    Pattern p = Pattern.compile(pattern);
    Matcher m = p.matcher(statement.getStatement().toLowerCase());
    return m.find();
  }
  
  private Map<String, Results> queryPartners(QueryLanguage queryLanguage, Integer timeout, boolean withTotalCount, Statement statement) {
	Map<String, Results> results = new HashMap<>();
	if (queryClients != null) {
	  queryClients.forEach(c -> {
		try {
		  Results r = c.query(queryLanguage, timeout, withTotalCount, statement);
		  results.put(c.getUrl(), r);
		} catch (Exception ex) {
		  log.debug("queryPartners.error; for client: {}", c.getUrl(), ex);	
		}
	  });
	}
	return results;
  }
  
  private Results mergePartnerResults(Results local, Map<String, Results> extra) {
	if (!extra.isEmpty()) {
		extra.values().stream().forEach(r -> {
		    r.getItems().forEach(m -> local.addItemsItem(m));
		    local.setTotalCount(local.getTotalCount() + r.getTotalCount());
		}); 
	}
    return local;
  }
  
}