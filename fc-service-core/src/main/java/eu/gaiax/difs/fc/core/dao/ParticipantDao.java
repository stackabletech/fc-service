package eu.gaiax.difs.fc.core.dao;

import java.util.List;
import java.util.Optional;

import eu.gaiax.difs.fc.api.generated.model.Participant;
import eu.gaiax.difs.fc.api.generated.model.UserProfile;

public interface ParticipantDao {

    Participant create(Participant part);
    Optional<Participant> select(String partId);
    Optional<List<UserProfile>> selectUsers(String partId);
    Optional<Participant> delete(String partId);
    Optional<Participant> update(String partId, Participant part);
    List<Participant> search(Integer offset, Integer limit);

}
