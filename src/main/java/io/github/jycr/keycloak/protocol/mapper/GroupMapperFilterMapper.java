package io.github.jycr.keycloak.protocol.mapper;

import org.keycloak.models.GroupModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.mappers.GroupMembershipMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * {@link org.keycloak.protocol.ProtocolMapper} that add custom optional claim containing the uuids of groups the user
 * is assigned to
 */
public class GroupMapperFilterMapper extends GroupMembershipMapper {
    public static final String PROVIDER_ID = "group-filter-mapper";

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static final String KEY_CONFIG_FILTER = GroupMapperFilterMapper.class.getName() + "#filter";
    public static final String GROUPS_CONFIG_FILTER_DESCRIPTION =
            "Pattern conforming to a regular expression as supported by `java.util.regex.Pattern`. " +
                    "All group names matching the specified pattern will be kept, others will be ignored and not included. " +
                    "If this pattern is empty, no filter will be performed, all values will be kept.";
    static final String KEY_CONFIG_MAPPING = GroupMapperFilterMapper.class.getName() + "#mapper";
    public static final String GROUPS_CONFIG_MAPPING_DESCRIPTION =
            "Defines the substitution rules to perform. " +
                    "The key matches a pattern conforming to a regular expression as supported by `java.util.regex.Pattern`. " +
                    "The value corresponds to the substitution to be made (corresponds to the parameter passed to the `java.util.regex.Matcher#replaceAll` method. " +
                    "Please note, the rules are executed in the defined order. " +
                    "It is therefore preferable to indicate the rules most more restrictive at the beginning.";
    static final String KEY_CONFIG_STOP_ON_FIRST_MATCH = GroupMapperFilterMapper.class.getName() + "#stopFirstMatch";
    public static final String GROUPS_CONFIG_MAPPING_STOP_ON_FIRST_MATCH =
            "If matched on a 'Group mapping substitution rules', does not perform subsequent substitutions. " +
                    "If set to false, this may impact performance.";

    static {
        OIDCAttributeMapperHelper.addTokenClaimNameConfig(configProperties);

        final ProviderConfigProperty filterConfigProperties = new ProviderConfigProperty();
        filterConfigProperties.setName(KEY_CONFIG_FILTER);
        filterConfigProperties.setLabel("Group filter");
        filterConfigProperties.setType(ProviderConfigProperty.STRING_TYPE);
        filterConfigProperties.setDefaultValue("");
        filterConfigProperties.setHelpText(GROUPS_CONFIG_FILTER_DESCRIPTION);
        filterConfigProperties.setSecret(true);

        configProperties.add(filterConfigProperties);

        final ProviderConfigProperty mappingConfigProperties = new ProviderConfigProperty();
        mappingConfigProperties.setName(KEY_CONFIG_MAPPING);
        mappingConfigProperties.setLabel("Group mapping substitution rules");
        mappingConfigProperties.setType(ProviderConfigProperty.MAP_TYPE);
        mappingConfigProperties.setDefaultValue("");
        mappingConfigProperties.setHelpText(GROUPS_CONFIG_MAPPING_DESCRIPTION);
        mappingConfigProperties.setSecret(true);

        configProperties.add(mappingConfigProperties);

        final ProviderConfigProperty stopOnFirstMatchConfigProperties = new ProviderConfigProperty();
        stopOnFirstMatchConfigProperties.setName(KEY_CONFIG_STOP_ON_FIRST_MATCH);
        stopOnFirstMatchConfigProperties.setLabel("Stop mapping on first match");
        stopOnFirstMatchConfigProperties.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        stopOnFirstMatchConfigProperties.setDefaultValue(Boolean.toString(true));
        stopOnFirstMatchConfigProperties.setHelpText(GROUPS_CONFIG_MAPPING_STOP_ON_FIRST_MATCH);

        configProperties.add(stopOnFirstMatchConfigProperties);

        OIDCAttributeMapperHelper.addIncludeInTokensConfig(configProperties, GroupMapperFilterMapper.class);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Group Filter and Mapper Membership";
    }

    @Override
    public String getHelpText() {
        return "Add group membership to user (list of group names)";
    }

    /**
     * Adds the group membership information to the {@link IDToken#otherClaims}.
     *
     * @param token        The {@link IDToken}
     * @param mappingModel The {@link ProtocolMapperModel}
     * @param userSession  The {@link UserSessionModel}
     */
    @Override
    protected void setClaim(
            final IDToken token,
            final ProtocolMapperModel mappingModel,
            final UserSessionModel userSession
    ) {
        final Map<String, String> config = mappingModel.getConfig();

        // Liste des groupes d'appartenance de l'utilisateur
        final List<String> membership = userSession.getUser().getGroupsStream().map(GroupModel::getName).collect(Collectors.toList());

        // Nom du Claim dans lequel mettre la liste des groupes remontés
        final String protocolClaim = config.get(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME);


        final Predicate<String> filter = getFilter(config);
        final Function<String, String> mapper = getMapper(config);

        final Set<String> groups = membership.stream()
                .filter(filter)
                .map(mapper)
                .collect(Collectors.toSet());

        token.getOtherClaims().put(
                protocolClaim,
                groups
        );
    }

    private static Predicate<String> getFilter(final Map<String, String> config) {
        return ConfigHelper.CONFIG.getFilter(config.get(KEY_CONFIG_FILTER));
    }


    private static Function<String, String> getMapper(final Map<String, String> config) {
        return ConfigHelper.CONFIG.getMapper(Pair.pair(config.get(KEY_CONFIG_MAPPING), config.get(KEY_CONFIG_STOP_ON_FIRST_MATCH)));
    }
}
