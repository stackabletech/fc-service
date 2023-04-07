package eu.gaiax.difs.fc.core.service.sdstore.impl;

import eu.gaiax.difs.fc.api.generated.model.SelfDescriptionStatus;

public record SubjectStatusRecord(String subjectId, Integer status) {
	
	SelfDescriptionStatus getSdStatus() {
		return status == null ? null : SelfDescriptionStatus.values()[status];
	}

}
