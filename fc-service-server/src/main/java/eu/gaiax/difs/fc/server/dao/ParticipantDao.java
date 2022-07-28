package eu.gaiax.difs.fc.server.dao;

import static eu.gaiax.difs.fc.server.dao.KeycloakUtils.getErrorMessage;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.GroupsResource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.hash.Hashing;

import eu.gaiax.difs.fc.api.generated.model.Participant;
import eu.gaiax.difs.fc.api.generated.model.UserProfile;
import eu.gaiax.difs.fc.server.exception.ConflictException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ParticipantDao {

    private static final String ATR_NAME = "name";
    private static final String ATR_PUBLIC_KEY = "publicKey";
    private static final String ATR_SD_HASH = "sdHash";

    @Value("${keycloak.realm}")
    private String realm;
    @Autowired
    private Keycloak keycloak;

    public Participant create(Participant part) {

        GroupsResource instance = keycloak.realm(realm).groups();
        GroupRepresentation groupRepo = toGroupRepo(part);
        Response response = instance.add(groupRepo);
        if (response.getStatus() != HttpStatus.SC_CREATED) {
            String message = getErrorMessage(response);
            log.info("create.error; status {}:{}, {}", response.getStatus(), response.getStatusInfo(), message);
            throw new ConflictException(message);
        }
        return part;
    }

    public Optional<Participant> select(String partId) {

        GroupsResource instance = keycloak.realm(realm).groups();
        List<GroupRepresentation> groups = instance.groups(partId, 0, 1, false);
        if (groups.size() == 0) {
            return Optional.empty();
        }
        return Optional.of(toParticipant(groups.get(0)));
    }

    public Optional<List<UserProfile>> selectUsers(String partId) {

        GroupsResource instance = keycloak.realm(realm).groups();
        List<GroupRepresentation> groups = instance.groups(partId, 0, 1, false);
        if (groups.size() == 0) {
            return Optional.empty();
        }
        GroupRepresentation groupRepo = groups.get(0);
        return Optional.of(instance.group(groupRepo.getId())
                .members().stream().map(ur -> UserDao.toUserProfile(ur)).collect(Collectors.toList()));
    }

    public Optional<Participant> delete(String partId) {

        GroupsResource instance = keycloak.realm(realm).groups();
        List<GroupRepresentation> groups = instance.groups(partId, 0, 1, false);
        if (groups.size() == 0) {
            return Optional.empty();
        }
        GroupRepresentation groupRepo = groups.get(0);
        instance.group(groupRepo.getId()).remove();
        return Optional.of(toParticipant(groupRepo));
    }

    public Optional<Participant> update(String partId, Participant part) {

        GroupsResource instance = keycloak.realm(realm).groups();
        List<GroupRepresentation> groups = instance.groups(partId, 0, 1, true);
        if (groups.size() == 0) {
            return Optional.empty();
        }
        GroupRepresentation groupRepo = groups.get(0);
        GroupRepresentation updated = toGroupRepo(part);
        instance.group(groupRepo.getId()).update(updated);
        return Optional.of(toParticipant(updated));
    }

    public List<Participant> search(Integer offset, Integer limit) {

        GroupsResource instance = keycloak.realm(realm).groups();
        List<GroupRepresentation> groups = instance.groups(null, offset, limit, false);
        return groups.stream().map(g -> toParticipant(g)).collect(Collectors.toList());
    }

    public static GroupRepresentation toGroupRepo(Participant part) {
        GroupRepresentation groupRepo = new GroupRepresentation();
        groupRepo.setName(part.getId());
        groupRepo.singleAttribute(ATR_NAME, part.getName());
        groupRepo.singleAttribute(ATR_PUBLIC_KEY, part.getPublicKey());
        String sha256 = Hashing.sha256().hashString(part.getSelfDescription(), StandardCharsets.UTF_8).toString();
        groupRepo.singleAttribute(ATR_SD_HASH, sha256);
        return groupRepo;
    }

    private Participant toParticipant(GroupRepresentation groupRepo) {
        Map<String, List<String>> attributes = groupRepo.getAttributes();
        return new Participant(groupRepo.getName(), attributes.get(ATR_NAME).get(0),
                attributes.get(ATR_PUBLIC_KEY).get(0), attributes.get(ATR_SD_HASH).get(0));
    }

}
