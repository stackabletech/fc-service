package eu.xfsc.fc.core.service.catalogue.participant;

import eu.xfsc.fc.core.dao.catalogue.node.NRegistrationNumber;
import eu.xfsc.fc.core.dao.catalogue.node.repository.RegistrationNumberRepository;
import eu.xfsc.fc.core.service.catalogue.pojo.RegistrationNumberDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationNumberService {

    private final RegistrationNumberRepository registrationNumberRepository;

    public Set<NRegistrationNumber> create(List<RegistrationNumberDTO> registrationNumbers) {
        return registrationNumbers.stream().map(r -> create(r.type(), r.number(), r.url())).collect(Collectors.toSet());
    }

    public NRegistrationNumber create(String type, String number) {
        return registrationNumberRepository.save(NRegistrationNumber.builder()
                .type(type)
                .number(number)
                .build());
    }

    public NRegistrationNumber create(String type, String number, String url) {
        return registrationNumberRepository.save(NRegistrationNumber.builder()
                .type(type)
                .number(number)
                .url(url)
                .build());
    }
}
