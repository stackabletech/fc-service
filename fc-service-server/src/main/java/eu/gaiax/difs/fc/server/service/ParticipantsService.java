package eu.gaiax.difs.fc.server.service;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import eu.gaiax.difs.fc.api.generated.model.Participant;
import eu.gaiax.difs.fc.server.generated.controller.ParticipantsApiDelegate;

@Service
public class ParticipantsService implements ParticipantsApiDelegate {
    
    @Override
    public ResponseEntity<Participant> addParticipant(Object body) {
        return null; //new ResponseEntity<>(HttpStatus.CREATED).of(created);
    }    

}
