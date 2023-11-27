package eu.xfsc.fc.core.dao.catalogue.node.repository;

import eu.xfsc.fc.core.dao.catalogue.node.NDataAccountExport;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DataAccountExportRepository extends Neo4jRepository<NDataAccountExport, UUID> {
}
