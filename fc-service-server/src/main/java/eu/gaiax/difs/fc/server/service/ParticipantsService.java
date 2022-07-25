package eu.gaiax.difs.fc.server.service;

import eu.gaiax.difs.fc.api.generated.model.Participant;
import eu.gaiax.difs.fc.server.generated.controller.ParticipantsApiDelegate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class ParticipantsService implements ParticipantsApiDelegate {

  @Override
  public ResponseEntity<Participant> addParticipant(Object body) {
    return null; //new ResponseEntity<>(HttpStatus.CREATED).of(created);
  }

}
