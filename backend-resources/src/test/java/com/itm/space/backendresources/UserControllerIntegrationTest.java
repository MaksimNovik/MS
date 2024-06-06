package com.itm.space.backendresources;

import com.itm.space.backendresources.api.request.UserRequest;
import com.itm.space.backendresources.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "MODERATOR")
class UserControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    UserService userService;
    @Autowired
    Keycloak keycloak;

    @AfterEach
    void cleanUp() {
        List<UserRepresentation> listUserRepresentation
                = keycloak.realm("ITM").users().search("Ivan");
        if (!(listUserRepresentation.isEmpty())) {
            UserRepresentation userRepresentation = listUserRepresentation.get(0);
            keycloak.realm("ITM").users().get(userRepresentation.getId()).remove();
        }
    }


    @Test
    void createUserRequestReturnsValidResponseEntity() throws Exception {
        var requestBuilder = post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                        "username": "Ivan",
                        "firstName": "Ivan",
                        "lastName": "Popov",
                        "email": "ivanpopov@mail.com",
                        "password": "12345f"
                        }
                        """);
        this.mvc.perform(requestBuilder)
                .andExpect(status().isOk());
    }

    @Test
    void createUserRequestPayloadIsInvalidReturnsValidResponseEntity() throws Exception {
        var requestBuilder = post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                        "username": " ",
                        "firstName": "Ivan",
                        "lastName": "Popov",
                        "email": "ivanpopov",
                        "password": "12"
                        }
                        """);
        this.mvc.perform(requestBuilder)
                .andExpectAll(
                        status().isBadRequest(),
                        header().doesNotExist(HttpHeaders.LOCATION),
                        content().contentType(MediaType.APPLICATION_JSON));

    }

    @Test
    void getUserByIdReturnsValidResponseEntity() throws Exception {
        userService.createUser(new UserRequest("Ivan", "ivanpopov@mail.com", "12345", "Ivan", "Popov"));
        String newUserRequestUUID = keycloak.realm("ITM").users().search("Ivan").get(0).getId();
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get("/api/users/" + newUserRequestUUID);
        mvc.perform(requestBuilder)
                .andExpectAll(
                        status().isOk(),
                        content().contentType(MediaType.APPLICATION_JSON),
                        content().json("""
                                {
                                "firstName": "Ivan",
                                "lastName": "Popov",
                                "email": "ivanpopov@mail.com",
                                "roles": ["default-roles-itm"],
                                "groups": []
                                }
                                """)
                );
    }
    @Test
    public void getUserByIdReturnsInValidResponseEntity() throws Exception {
        String newUserRequestUUID = String.valueOf(UUID.randomUUID());
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get("/api/users/" + newUserRequestUUID);
        mvc.perform(requestBuilder).andExpect(status().is(500));
    }

    @Test
    void helloTest() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get("/api/users/hello");
        mvc.perform(requestBuilder)
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void helloWithoutAuthorities() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get("/api/users/hello");
        mvc.perform(requestBuilder)
                .andExpect(status().is4xxClientError());
    }
}