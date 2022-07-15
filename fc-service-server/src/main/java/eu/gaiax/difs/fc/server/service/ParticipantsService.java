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
        // store Participant in DB, create SD for it etc.. 
        // here we can do our own implementation with some our helper classes, may be using Data Access Layer, 
        // or we will use components provided by Fraunhofer to work with their entities.. 
        //Optional<Participant> created = Optional.of(participant); 
        return null; //new ResponseEntity<>(HttpStatus.CREATED).of(created);
    }    

}
