package eu.gaiax.difs.fc.server.service;


import eu.gaiax.difs.fc.api.generated.model.OntologySchema;
import eu.gaiax.difs.fc.server.generated.controller.SchemasApiDelegate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * Service for the crude operation of the {@link SchemaFC} . Implementation of the
 * {@link SchemasApiDelegate} .
 */
@Slf4j
@Service
public class SchemasService implements SchemasApiDelegate {
  // TODO : We will add actual service implemented by FH for getting result back.

  /**
   *  Get a specific schema.
   *
   * @param schemaId Identifier of the Schema (required)
   * @return A specific Schema (status code 200)
   *         or May contain hints how to solve the error or indicate what was wrong in the request. (status code 400)
   *         or The specified resource was not found (status code 404)
   *         or May contain hints how to solve the error or indicate what went wrong at the server. Must not outline any
   *         information about the internal structure of the server. (status code 500)
   */
  @Override
  public ResponseEntity<OntologySchema> getSchema(String schemaId) {
    log.debug("getSchema.enter, got schemaId {}", schemaId);
    Optional<OntologySchema> schema = Optional.of(new OntologySchema());
    log.debug("getSchema.exit, returning {}", schema);
    return new ResponseEntity<>(HttpStatus.OK).of(schema);
  }

  /**
   *  Get the full list of schemas on optional parameter basis .
   *
   * @param offset The number of items to skip before starting to collect the result set. (optional, default to 0)
   * @param limit The number of items to return. (optional, default to 100)
   * @param orderBy Results will be sorted by this field. (optional)
   * @param ascending Ascending/Descending ordering. (optional, default to true)
   * @return All schemas (status code 200)
   *         or May contain hints how to solve the error or indicate what was wrong in the request. (status code 400)
   *         or May contain hints how to solve the error or indicate what went wrong at the server. Must not outline any
   *         information about the internal structure of the server. (status code 500)
   */
  @Override
  public ResponseEntity<List<OntologySchema>> getSchemas(Integer offset, Integer limit) { // String orderBy, Boolean ascending) {
    log.debug("getSchemas.enter, got offset {}, got limit {}", offset, limit);
    Optional<List<OntologySchema>> schemas = Optional.of(Collections.emptyList());
    log.debug("getSchema.exit, returning {}", schemas);
    return new ResponseEntity<>(HttpStatus.OK).of(schemas);
  }

  /**
   * Get the latest schema of all types .
   *
   * @return The latest schemas of all types (status code 200)
   *         or May contain hints how to solve the error or indicate what was wrong in the request. (status code 400)
   *         or Invalid input (status code 405)
   *         or May contain hints how to solve the error or indicate what went wrong at the server. Must not outline any
   *         information about the internal structure of the server. (status code 500)
   */
  @Override
  public ResponseEntity<List<OntologySchema>> getLatestSchemas() {
    log.debug("getLatestSchemas.enter");
    Optional<List<OntologySchema>> schemas = Optional.of(Collections.emptyList());
    log.debug("getLatestSchemas.exit, returning {}", schemas);
    return new ResponseEntity<>(HttpStatus.OK).of(schemas);
  }

  /**
   *  Get the latest schemas of a specific type .
   *
   * @param type Type of the requested Self-Description schema e.g. Service (required)
   * @return The latest schema of requested types (status code 200)
   *         or May contain hints how to solve the error or indicate what was wrong in the request. (status code 400)
   *         or Invalid input (status code 405)
   *         or May contain hints how to solve the error or indicate what went wrong at the server. Must not outline any
   *         information about the internal structure of the server. (status code 500)
   */
  @Override
  public ResponseEntity<OntologySchema> getLatestSchemaOfType(String type) {
    log.debug("getLatestSchemaOfType.enter, got type {}", type);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  /**
   * Add a new Schema to the catalogue, or replace an existing schema that has the same URI.
   *
   * @param  schema {@link SchemaFC} The new Schema, (required)
   * @return Created, (status code 201)
   *         or May contain hints how to solve the error or indicate what was wrong in the request. (status code 400)
   *         or Forbidden. The user does not have the permission to execute this request. (status code 403)
   *         or May contain hints how to solve the error or indicate what went wrong at the server. Must not outline any
   *         information about the internal structure of the server. (status code 500)
   */
  @Override
  public ResponseEntity<Void> addSchema(OntologySchema schema) {
    log.debug("addSchema.enter, got schema {}", schema);
    return new ResponseEntity<>(HttpStatus.CREATED);
  }

  /**
   *  Delete a Schema .
   *
   * @param schemaId Identifier of the Schema (required)
   * @return Deleted Schema (status code 200)
   *         or May contain hints how to solve the error or indicate what was wrong in the request. (status code 400)
   *         or The specified resource was not found (status code 404)
   *         or May contain hints how to solve the error or indicate what went wrong at the server. Must not outline any
   *         information about the internal structure of the server. (status code 500)
   */
  @Override
  public ResponseEntity<Void> deleteSchema(String schemaId) {
    log.debug("deleteSchema.enter, got schemaId {}", schemaId);
    return new ResponseEntity<>(HttpStatus.OK);
  }
}
