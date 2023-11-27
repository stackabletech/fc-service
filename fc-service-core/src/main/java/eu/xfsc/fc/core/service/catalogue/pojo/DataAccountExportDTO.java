package eu.xfsc.fc.core.service.catalogue.pojo;

import java.util.Set;

public record DataAccountExportDTO(String requestType, String accessType, Set<String> formatType) {
}
