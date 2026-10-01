package com.quickprescription.authentication;

import com.jayway.jsonpath.JsonPath;
import com.quickprescription.authentication.model.User;
import com.quickprescription.authentication.repository.UserRepository;
import com.quickprescription.authentication.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.flyway.force-on-startup=false",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false",
        "spring.datasource.hikari.initialization-fail-timeout=-1"
})
class MsaQuickPrescriptionAuthenticationApplicationTests {

  @Autowired
  private WebApplicationContext webApplicationContext;

  private MockMvc mockMvc;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  @MockitoBean
  private UserRepository userRepository;

  private final AtomicLong userIdSequence = new AtomicLong(1L);
  private final Map<String, User> usersByMail = new ConcurrentHashMap<>();

  @BeforeEach
  void setUp() {
    mockMvc = webAppContextSetup(webApplicationContext)
            .apply(springSecurity())
            .build();

    usersByMail.clear();
    userIdSequence.set(1L);

    when(userRepository.existsByUserMailIgnoreCase(anyString()))
            .thenAnswer(invocation -> findIgnoreCase(invocation.getArgument(0)).isPresent());

    when(userRepository.findByUserMailIgnoreCase(anyString()))
            .thenAnswer(invocation -> findIgnoreCase(invocation.getArgument(0)));

    when(userRepository.save(any(User.class)))
            .thenAnswer(invocation -> {
              User user = invocation.getArgument(0);
              if (user.getUserId() == null) {
                user.setUserId(userIdSequence.getAndIncrement());
              }
              usersByMail.put(user.getUserMail(), user);
              return user;
            });

    when(userRepository.findAll())
            .thenAnswer(invocation -> new ArrayList<>(usersByMail.values()));
  }

  private Optional<User> findIgnoreCase(String mail) {
    return usersByMail.values().stream()
            .filter(user -> user.getUserMail().equalsIgnoreCase(mail))
            .findFirst();
  }

  @Test
  void contextLoads() {
  }

  @Test
  void generatesAndValidatesJwtWithConfiguredSecret() {
    String token = jwtTokenProvider.generateToken(1L, "user@example.com", "ADMIN");

    assertTrue(jwtTokenProvider.validateToken(token));
    assertEquals("user@example.com", jwtTokenProvider.extractUserMail(token));
    assertEquals(1L, jwtTokenProvider.extractUserId(token));
    assertEquals("ADMIN", jwtTokenProvider.extractUserRole(token));
  }

  @Test
  void appliesCorsForAllowedOrigins() throws Exception {
    mockMvc.perform(options("/api/auth/login")
                    .header(HttpHeaders.ORIGIN, "http://localhost:3000"))
            .andExpect(status().isOk())
            .andExpect(result -> assertEquals("http://localhost:3000",
                    result.getResponse().getHeader("Access-Control-Allow-Origin")));
  }

  @Test
  void rejectsCorsForDisallowedOrigins() throws Exception {
    mockMvc.perform(options("/api/auth/login")
                    .header(HttpHeaders.ORIGIN, "http://evil.example"))
            .andExpect(status().isForbidden());
  }

  @Test
  void allowsRegisterAndLoginWithoutAuthentication() throws Exception {
    String email = "user-" + UUID.randomUUID() + "@example.com";

    mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "userName": "Test User",
                              "userMail": "%s",
                              "userPassword": "password123"
                            }
                            """.formatted(email)))
            .andExpect(status().isCreated());

    mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "userMail": "%s",
                              "userPassword": "password123"
                            }
                            """.formatted(email)))
            .andExpect(status().isOk());
  }

  @Test
  void registerReturnsOnlyContractFields() throws Exception {
    mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"userName": "Juan Pérez", "userMail": "juan@example.com", "userPassword": "password123"}
                            """))
            .andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.*", hasSize(3)))
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.userName").value("Juan Pérez"))
            .andExpect(jsonPath("$.userMail").value("juan@example.com"))
            .andExpect(jsonPath("$.userId").doesNotExist())
            .andExpect(jsonPath("$.userRole").doesNotExist())
            .andExpect(jsonPath("$.userPassword").doesNotExist());
  }

  @Test
  void loginReturnsContractTokenResponse() throws Exception {
    register("Juan", "juan@example.com", "password123");

    String body = mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"userMail": "juan@example.com", "userPassword": "password123"}
                            """))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.*", hasSize(3)))
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.expiresIn").value(jwtTokenProvider.getExpirationSeconds()))
            .andExpect(jsonPath("$.token").doesNotExist())
            .andExpect(jsonPath("$.userId").doesNotExist())
            .andExpect(jsonPath("$.userName").doesNotExist())
            .andExpect(jsonPath("$.userMail").doesNotExist())
            .andExpect(jsonPath("$.userRole").doesNotExist())
            .andReturn().getResponse().getContentAsString();

    assertEquals(86400, jwtTokenProvider.getExpirationSeconds());

    String accessToken = JsonPath.read(body, "$.accessToken");
    int expiresIn = JsonPath.read(body, "$.expiresIn");
    String[] parts = accessToken.split("\\.");
    Base64.Decoder decoder = Base64.getUrlDecoder();
    String header = new String(decoder.decode(parts[0]), StandardCharsets.UTF_8);
    String payload = new String(decoder.decode(parts[1]), StandardCharsets.UTF_8);

    assertEquals("HS512", JsonPath.read(header, "$.alg"));
    long iat = ((Number) JsonPath.read(payload, "$.iat")).longValue();
    long exp = ((Number) JsonPath.read(payload, "$.exp")).longValue();
    assertEquals(expiresIn, exp - iat);
    assertTrue(jwtTokenProvider.validateToken(accessToken));
  }

  @Test
  void validatesSessionTokenWithoutAuthentication() throws Exception {
    String token = jwtTokenProvider.generateToken(55L, "validate@example.com", "USUARIO_FINAL");

    mockMvc.perform(post("/api/auth/validate-session")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "token": "%s"
                            }
                            """.formatted(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(true))
            .andExpect(jsonPath("$.userId").value(55))
            .andExpect(jsonPath("$.userMail").value("validate@example.com"))
            .andExpect(jsonPath("$.userRole").value("USUARIO_FINAL"));
  }

  @Test
  void returnsInvalidWhenSessionTokenIsNotValid() throws Exception {
    mockMvc.perform(post("/api/auth/validate-session")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "token": "token-invalido"
                            }
                            """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(false))
            .andExpect(jsonPath("$.userId").doesNotExist())
            .andExpect(jsonPath("$.userMail").doesNotExist())
            .andExpect(jsonPath("$.userRole").doesNotExist());
  }

  @Test
  void rejectsProtectedEndpointsWithoutAuthentication() throws Exception {
    mockMvc.perform(get("/api/auth/users"))
            .andExpect(status().isUnauthorized());
  }

  @Test
  void ignoresUserRoleOnRegister() throws Exception {
    String email = "admin-" + UUID.randomUUID() + "@example.com";

    mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "userName": "Test User",
                              "userMail": "%s",
                              "userPassword": "password123",
                              "userRole": "ADMIN"
                            }
                            """.formatted(email)))
            .andExpect(status().isCreated());

    assertEquals("USUARIO_FINAL", usersByMail.get(email).getUserRole());
  }

  @Test
  void userManagementEndpointsNoLongerExist() throws Exception {
    String token = jwtTokenProvider.generateToken(99L, "secured@example.com", "ADMIN");

    mockMvc.perform(get("/api/auth/users")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isNotFound());
    mockMvc.perform(get("/api/auth/users/1")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isNotFound());
    mockMvc.perform(put("/api/auth/users/1/role")
                    .param("newRole", "ADMIN")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isNotFound());
    mockMvc.perform(delete("/api/auth/users/1")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isNotFound());
  }


  private void register(String name, String mail, String password) throws Exception {
    mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"userName": "%s", "userMail": "%s", "userPassword": "%s"}
                            """.formatted(name, mail, password)))
            .andExpect(status().isCreated());
  }

  @Test
  void rejectsEmptyRegisterBodyWithFieldErrors() throws Exception {
    mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.title").value("Solicitud inválida"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.instance").value("/api/auth/register"))
            .andExpect(jsonPath("$.errores[*].campo", hasItem("userName")))
            .andExpect(jsonPath("$.errores[*].campo", hasItem("userMail")))
            .andExpect(jsonPath("$.errores[*].campo", hasItem("userPassword")));
  }

  @Test
  void rejectsInvalidRegisterFields() throws Exception {
    String[][] invalid = {
            {"J", "ok@example.com", "password123", "userName"},
            {"J".repeat(151), "ok@example.com", "password123", "userName"},
            {"Juan", "x", "password123", "userMail"},
            {"Juan", "a".repeat(60) + "@" + ("b".repeat(60) + ".").repeat(4) + "com", "password123", "userMail"},
            {"Juan", "ok@example.com", "1234567", "userPassword"},
            {"Juan", "ok@example.com", "p".repeat(73), "userPassword"},
    };
    for (String[] c : invalid) {
      mockMvc.perform(post("/api/auth/register")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content("""
                              {"userName": "%s", "userMail": "%s", "userPassword": "%s"}
                              """.formatted(c[0], c[1], c[2])))
              .andExpect(status().isBadRequest())
              .andExpect(jsonPath("$.errores", hasSize(1)))
              .andExpect(jsonPath("$.errores[0].campo").value(c[3]))
              .andExpect(jsonPath("$.errores[0].mensaje").isNotEmpty());
    }
  }

  @Test
  void rejectsMalformedJsonWithoutEchoingIt() throws Exception {
    mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"userMail\": \"secreto@example.com\", "))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(result -> assertTrue(!result.getResponse().getContentAsString().contains("secreto")));
  }

  @Test
  void rejectsDuplicateMailIgnoringCase() throws Exception {
    register("Juan", "Juan@Example.com", "password123");

    mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"userName": "Otro", "userMail": "juan@example.com", "userPassword": "password123"}
                            """))
            .andExpect(status().isConflict())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.title").value("Conflicto"))
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.detail").value("Ya existe un consumidor registrado con ese correo."));
  }

  @Test
  void loginIgnoresMailCase() throws Exception {
    register("Juan", "Juan@Example.com", "password123");

    mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"userMail": "juan@example.com", "userPassword": "password123"}
                            """))
            .andExpect(status().isOk());
  }

  @Test
  void returnsSameUnauthorizedForUnknownMailAndWrongPassword() throws Exception {
    register("Juan", "juan@example.com", "password123");

    String unknownMail = mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"userMail": "nadie@example.com", "userPassword": "password123"}
                            """))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andReturn().getResponse().getContentAsString();

    String wrongPassword = mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"userMail": "juan@example.com", "userPassword": "incorrecta"}
                            """))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
            .andReturn().getResponse().getContentAsString();

    assertEquals(unknownMail, wrongPassword);
  }

  @Test
  void loginDoesNotValidatePasswordLength() throws Exception {
    register("Juan", "juan@example.com", "password123");

    for (String password : new String[]{"1234567", "p".repeat(73)}) {
      mockMvc.perform(post("/api/auth/login")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content("""
                              {"userMail": "juan@example.com", "userPassword": "%s"}
                              """.formatted(password)))
              .andExpect(status().isUnauthorized())
              .andExpect(jsonPath("$.detail").value("Correo o contraseña incorrectos."));
    }
  }

  @Test
  void rejectsBlankLoginPassword() throws Exception {
    mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"userMail": "juan@example.com", "userPassword": ""}
                            """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores", hasSize(1)))
            .andExpect(jsonPath("$.errores[0].campo").value("userPassword"));
  }

  @Test
  void rejectsInvalidLoginFields() throws Exception {
    mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"userMail": "x"}
                            """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores", hasSize(2)))
            .andExpect(jsonPath("$.errores[*].campo", hasItem("userMail")))
            .andExpect(jsonPath("$.errores[*].campo", hasItem("userPassword")));
  }

  @Test
  void returnsGenericInternalErrorWithoutDetails() throws Exception {
    when(userRepository.save(any(User.class))).thenThrow(new IllegalStateException("detalle interno secreto"));

    mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"userName": "Juan", "userMail": "juan@example.com", "userPassword": "password123"}
                            """))
            .andExpect(status().isInternalServerError())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.title").value("Error interno"))
            .andExpect(jsonPath("$.detail").value("Ocurrió un error inesperado. Intente nuevamente más tarde."))
            .andExpect(result -> assertTrue(!result.getResponse().getContentAsString().contains("secreto")))
            .andExpect(result -> assertTrue(!result.getResponse().getContentAsString().contains("Exception")));
  }

}
