package eu.xfsc.fc.core.service.catalogue.participant;

import eu.xfsc.fc.core.dao.catalogue.node.NTermsAndCondition;
import eu.xfsc.fc.core.dao.catalogue.node.repository.TermsAndConditionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class TermsAndConditionService {

    private final TermsAndConditionRepository termsAndConditionRepository;

    public NTermsAndCondition create(String hash, String url, String content, boolean gaiaxTnc) {
        if (gaiaxTnc) {
            NTermsAndCondition gaiaxTncDetail = termsAndConditionRepository.getByGaiaxTnc(true);
            if (Objects.nonNull(gaiaxTncDetail)) {
                return gaiaxTncDetail;
            }
        }
        return termsAndConditionRepository.save(NTermsAndCondition.builder()
                .hash(hash)
                .url(url)
                .content(content)
                .gaiaxTnc(gaiaxTnc)
                .build());
    }
}
