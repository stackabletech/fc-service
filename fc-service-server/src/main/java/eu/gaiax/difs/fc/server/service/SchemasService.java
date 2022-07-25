package eu.gaiax.difs.fc.server.service;

import eu.gaiax.difs.fc.api.generated.model.SchemaFC;
import eu.gaiax.difs.fc.server.generated.controller.SchemasApiDelegate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SchemasService implements SchemasApiDelegate {
  // TODO : We will add actual service implemented by FH for getting result back.
  @Override
  public ResponseEntity<SchemaFC> getSchema(String schemaId) {
    log.debug("processing getSchema");
    Optional<SchemaFC> schema = Optional.of(new SchemaFC());
    return new ResponseEntity<>(HttpStatus.OK).of(schema);
  }

  @Override
  public ResponseEntity<List<SchemaFC>> getSchemas(Integer offset, Integer limit, String orderBy,
                                                   Boolean ascending) {
    log.debug("processing getSchemas");
    Optional<List<SchemaFC>> schemas = Optional.of(Collections.emptyList());
    return new ResponseEntity<>(HttpStatus.OK).of(schemas);
  }

  @Override
  public ResponseEntity<List<Object>> getLatestSchemas() {
    log.debug("processing getLatestSchemas");
    Optional<List<Object>> schemas = Optional.of(Collections.emptyList());
    return new ResponseEntity<>(HttpStatus.OK).of(schemas);
  }

  @Override
  public ResponseEntity<Void> getLatestSchemaOfType(String type) {
    log.debug("processing getLatestSchemaOfType");
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Override
  public ResponseEntity<Void> addSchema(SchemaFC schema) {
    log.debug("processing addSchema");
    return new ResponseEntity<>(HttpStatus.CREATED);
  }

  @Override
  public ResponseEntity<Void> deleteSchema(String schemaId) {
    log.debug("processing deleteSchema");
    return new ResponseEntity<>(HttpStatus.OK);
  }
}
