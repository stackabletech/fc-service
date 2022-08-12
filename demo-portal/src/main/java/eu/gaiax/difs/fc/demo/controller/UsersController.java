package eu.gaiax.difs.fc.demo.controller;

import eu.gaiax.difs.fc.api.generated.model.User;
import eu.gaiax.difs.fc.api.generated.model.UserProfile;
import eu.gaiax.difs.fc.demo.proxy.RequestCall;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Users API Controller.
 */
@RestController
@RequestMapping("users")
public class UsersController {
  @Autowired
  @Qualifier("fcServer")
  private WebClient fcServer;

  /**
   * POST /users : Register a new user to the associated participant in the catalogue.
   *
   * @param user User profile (optional)
   * @return Created User profile (status code 201)
   *         or May contain hints how to solve the error or indicate what was wrong in the request. (status code 400)
   *         or Forbidden. The user does not have the permission to execute this request. (status code 403)
   *         or HTTP Conflict 409 (status code 409)
   *         or May contain hints how to solve the error or indicate what went wrong at the server. (status code 500)
   */
  @PostMapping
  public ResponseEntity<UserProfile> addUser(HttpServletRequest request, @RequestBody(required = false) User user) {
    return RequestCall.doPost(fcServer, request, user);
  }

  /**
   * PUT /users/{userId} : Update the user profile.
   *
   * @param id (required)
   * @param user User profile (optional)
   * @return Updated user profile (status code 200)
   *         or May contain hints how to solve the error or indicate what was wrong in the request. (status code 400)
   *         or Forbidden. The user does not have the permission to execute this request. (status code 403)
   *         or The specified resource was not found (status code 404)
   *         or May contain hints how to solve the error or indicate what went wrong at the server. (status code 500)
   */
  @PutMapping("/{userId}")
  public ResponseEntity<UserProfile> updateUser(HttpServletRequest request, @PathVariable("userId") String id,
                                                @RequestBody(required = false) User user) {
    return RequestCall.doPut(fcServer, request, user);
  }

  /**
   * DELETE /users/{userId} : Delete a user.
   *
   * @param id (required)
   * @return Deleted user profile (status code 200)
   *         or May contain hints how to solve the error or indicate what was wrong in the request. (status code 400)
   *         or Forbidden. The user does not have the permission to execute this request. (status code 403)
   *         or The specified resource was not found (status code 404)
   *         or HTTP Conflict 409 (status code 409)
   *         or May contain hints how to solve the error or indicate what went wrong at the server. (status code 500)
   */
  @DeleteMapping("/{userId}")
  public ResponseEntity<UserProfile> deleteUser(HttpServletRequest request, @PathVariable("userId") String id) {
    return RequestCall.doDelete(fcServer, request);
  }

  /**
   * GET /users/{userId} : Get the user profile.
   *
   * @param userId (required)
   * @return User profile (status code 200)
   *         or May contain hints how to solve the error or indicate what was wrong in the request. (status code 400)
   *         or Forbidden. The user does not have the permission to execute this request. (status code 403)
   *         or The specified resource was not found (status code 404)
   *         or May contain hints how to solve the error or indicate what went wrong at the server. (status code 500)
   */
  @GetMapping("/{userId}")
  public ResponseEntity<UserProfile> getUser(HttpServletRequest request, @PathVariable("userId") String userId) {
    return RequestCall.doGet(fcServer, request);
  }

  /**
   * GET /users : List the registered users.
   *
   * @param offset The number of items to skip before starting to collect the result set. (optional, default to 0)
   * @param limit The number of items to return. (optional, default to 100)
   * @param orderBy Results will be sorted by this field. (optional)
   * @param ascending Ascending/Descending ordering. (optional, default to true)
   * @return List of usernames (status code 200)
   *         or May contain hints how to solve the error or indicate what was wrong in the request. (status code 400)
   *         or May contain hints how to solve the error or indicate what went wrong at the server. (status code 500)
   */
  @GetMapping
  public ResponseEntity<List<UserProfile>> getUsers(
      HttpServletRequest request,
      @RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset,
      @RequestParam(value = "limit", required = false, defaultValue = "100") Integer limit,
      @RequestParam(value = "orderBy", required = false) String orderBy,
      @RequestParam(value = "ascending", required = false, defaultValue = "true") Boolean ascending) {
    return RequestCall.doGet(fcServer, request);
  }

  /**
   * GET /users/{userId}/roles : Get roles of the user.
   *
   * @param id (required)
   * @return User roles (status code 200)
   *         or May contain hints how to solve the error or indicate what was wrong in the request. (status code 400)
   *         or Forbidden. The user does not have the permission to execute this request. (status code 403)
   *         or The specified resource was not found (status code 404)
   *         or May contain hints how to solve the error or indicate what went wrong at the server. (status code 500)
   */
  @GetMapping("/{userId}/roles")
  public ResponseEntity<List<String>> getUserRoles(HttpServletRequest request, @PathVariable("userId") String id) {
    return RequestCall.doGet(fcServer, request);
  }

  /**
   * PUT /users/{userId}/roles : Update roles of the user.
   *
   * @param id (required)
   * @param roles List of roles which should be assigned to the user (optional)
   * @return All assigned roles of the user (status code 200)
   *         or May contain hints how to solve the error or indicate what was wrong in the request. (status code 400)
   *         or Forbidden. The user does not have the permission to execute this request. (status code 403)
   *         or The specified resource was not found (status code 404)
   *         or May contain hints how to solve the error or indicate what went wrong at the server. (status code 500)
   */
  @PutMapping("/{userId}/roles")
  public ResponseEntity<List<String>> updateUserRoles(HttpServletRequest request, @PathVariable("userId") String id,
                                                      @RequestBody(required = false) List<String> roles) {
    return RequestCall.doPut(fcServer, request, roles);
  }
}