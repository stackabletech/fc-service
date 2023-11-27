package eu.xfsc.fc.core.service.catalogue.participant;

import eu.xfsc.fc.core.dao.catalogue.node.NAddress;
import eu.xfsc.fc.core.dao.catalogue.node.repository.AddressRepository;
import eu.xfsc.fc.core.service.catalogue.pojo.LocationDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class AddressService {
    private final AddressRepository addressRepository;

    public NAddress create(LocationDTO dto) {
        return create(dto.countryCode(), dto.gps());
    }

    public NAddress create(String countryCode) {
        return create(countryCode, null);
    }

    public NAddress create(String countryCode, String gps) {
        NAddress NAddress = addressRepository.getByName(countryCode);
        if (Objects.nonNull(NAddress)) {
            return NAddress;
        }
        NAddress = NAddress.builder()
                .gps(gps)
                .build();
        NAddress.setName(countryCode);
        return addressRepository.save(NAddress);
    }
}
