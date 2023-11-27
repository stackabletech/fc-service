package eu.xfsc.fc.core.dao.catalogue;

import eu.xfsc.fc.core.pojo.catalogue.CESTracker;

public interface CESTrackerDao {

    CESTracker getByCesId(String cesId);

    String fetchLastIngestedEvent();

    CESTracker create(String cesId, Long status, String reason, String credential);
}
