package io.github.jycr.keycloak.protocol.mapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.representations.IDToken;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonValue;
import javax.json.JsonWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static io.github.jycr.keycloak.protocol.mapper.GroupMapperFilterMapper.KEY_CONFIG_FILTER;
import static io.github.jycr.keycloak.protocol.mapper.GroupMapperFilterMapper.KEY_CONFIG_MAPPING;
import static io.github.jycr.keycloak.protocol.mapper.GroupMapperFilterMapper.KEY_CONFIG_STOP_ON_FIRST_MATCH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupMapperFilterMapperTest {
    @Mock
    KeycloakSession keycloakSession;
    @Mock
    ClientSessionContext clientSessionCtx;

    @Test
    void null_empty_and_duplicate_values_should_be_ignore() {
        // Given
        String[] userMembership = {"", null, "foo", "foo", "bar"};
        String configFilter = null;
        Map<String, String> configMapping = new HashMap<>();
        boolean configStopOnFirstMatch = true;

        // When
        Iterable<String> groups = runMapper(configFilter, configMapping, configStopOnFirstMatch, userMembership);

        // Then
        assertThat(groups).containsExactlyInAnyOrder("foo", "bar");
    }

    private Iterable<String> runMapper(String configFilter, Map<String, String> configMapping, boolean configStopOnFirstMatch, String... membership) {
        UserSessionModel userSession = initSessionForUserWithMembership(membership);
        IDToken token = new IDToken();
        String protocolClaim = "custom_group";
        ProtocolMapperModel mapperModel = createProtocolMapperModel(protocolClaim, configFilter, configMapping, configStopOnFirstMatch);
        GroupMapperFilterMapper mapper = new GroupMapperFilterMapper();
        mapper.setClaim(token, mapperModel, userSession, keycloakSession, clientSessionCtx);
        return (Iterable<String>) token.getOtherClaims().get(protocolClaim);
    }

    private UserSessionModel initSessionForUserWithMembership(String... groups) {
        UserModel user = mock(UserModel.class);
        Stream<GroupModel> groupStream = Stream.of(groups).map(this::toGroupModel);
        when(user.getGroupsStream()).thenReturn(groupStream);

        UserSessionModel userSession = mock(UserSessionModel.class);
        when(userSession.getUser()).thenReturn(user);
        return userSession;
    }

    private GroupModel toGroupModel(String groupName) {
        GroupModel group = mock(GroupModel.class);
        when(group.getName()).thenReturn(groupName);
        return group;
    }

    private ProtocolMapperModel createProtocolMapperModel(String protocolClaim, String filter, Map<String, String> mapping, boolean stopOnFirstMatch) {
        JsonArray mappingJsonConfig = toKeyValueArrayString(mapping);
        Map<String, String> config = new HashMap<>();
        config.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, protocolClaim);
        config.put(KEY_CONFIG_FILTER, filter);
        config.put(KEY_CONFIG_MAPPING, toKeyValueArrayString(mappingJsonConfig));
        config.put(KEY_CONFIG_STOP_ON_FIRST_MATCH, Boolean.toString(stopOnFirstMatch));
        ProtocolMapperModel mappingModel = new ProtocolMapperModel();
        mappingModel.setConfig(config);
        return mappingModel;
    }

    private JsonArray toKeyValueArrayString(Map<String, String> mapping) {
        JsonArrayBuilder builder = Json.createArrayBuilder();
        return builder.build();
    }

    private String toKeyValueArrayString(JsonValue json) {
        try (StringWriter sw = new StringWriter();
             JsonWriter writer = Json.createWriter(sw);) {
            writer.write(json);
            return sw.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}