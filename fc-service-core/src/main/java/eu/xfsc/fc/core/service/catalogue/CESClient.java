package eu.xfsc.fc.core.service.catalogue;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(value = "GaiaxCESClient", url = "${host.ces}")
public interface CESClient {

    @GetMapping(value = "/credentials-events", consumes = MediaType.APPLICATION_JSON_VALUE)
    List<Object> fetchCredentials(@RequestParam("lastReceivedID") String lastReceivedID, @RequestParam(value = "page", defaultValue = "0") Long page, @RequestParam(value = "size", defaultValue = "20") Long size);

    @GetMapping(value = "/credentials-events/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    Object fetchCredentialById(@PathVariable("id") String cesId);
}
