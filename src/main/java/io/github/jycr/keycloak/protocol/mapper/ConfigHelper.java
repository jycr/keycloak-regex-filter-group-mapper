package io.github.jycr.keycloak.protocol.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jboss.logging.Logger;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

public class ConfigHelper {
    private static final Logger LOGGER = Logger.getLogger(ConfigHelper.class);
    private static final TypeReference<List<Pair<String, String>>> MAP_TYPE_REPRESENTATION = new TypeReference<List<Pair<String, String>>>() {
    };
    static final Predicate<String> NO_FILTER = s -> true;

    /**
     * Singleton
     */
    public static final ConfigHelper CONFIG = new ConfigHelper();
    static final Function<String, String> NO_MAPPER = Function.identity();

    private ConfigHelper() {
    }

    private final LoadingCache<String, Predicate<String>> configCacheForFilter = Caffeine.newBuilder()
            .expireAfterAccess(1, TimeUnit.DAYS)
            .maximumSize(30)
            .build(this::buildPredicateFilter);

    private final LoadingCache<Pair<String, String>, Function<String, String>> configCacheForMapping = Caffeine.newBuilder()
            .expireAfterAccess(1, TimeUnit.DAYS)
            .maximumSize(30)
            .build(this::buildMapper);

    private Function<String, String> buildMapper(Pair<String, String> configs) {
        if (configs == null || configs.getKey() == null || configs.getKey().isEmpty()) {
            LOGGER.debug("No mapper");
            return NO_MAPPER;
        }
        boolean stopOnFirstMatch = Boolean.parseBoolean(configs.getValue());

        final List<Pair<Pattern, String>> config = parseConfigValue(configs.getKey());
        return groupName -> {
            String result = groupName;
            for (Pair<Pattern, String> patternStringPair : config) {
                Matcher matcher = patternStringPair.getKey().matcher(result);
                if (matcher.matches()) {
                    result = matcher.replaceAll(patternStringPair.getValue());
                    if (stopOnFirstMatch) {
                        return result;
                    }
                }
            }
            return result;
        };
    }

    List<Pair<Pattern, String>> parseConfigValue(String jsonConfig) {
        if (jsonConfig == null || jsonConfig.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return JsonSerialization.readValue(jsonConfig, MAP_TYPE_REPRESENTATION).stream()
                    .map(p -> new Pair<>(
                            parseRegex(p.getKey()),
                            p.getValue()
                    ))
                    .filter(p -> Objects.nonNull(p.getKey()) && Objects.nonNull(p.getValue()))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            LOGGER.error("Unable to process config: " + jsonConfig, e);
            return Collections.emptyList();
        }
    }

    private Predicate<String> buildPredicateFilter(String patternString) {
        try {
            final Pattern pattern = Pattern.compile(patternString);
            return str -> pattern.matcher(str).matches();
        } catch (PatternSyntaxException e) {
            LOGGER.error("Ignore illegal pattern: " + patternString + "  cause: " + e.getMessage());
            return NO_FILTER;
        }
    }

    Pattern parseRegex(String pattern) {
        try {
            return Pattern.compile(pattern);
        } catch (PatternSyntaxException e) {
            LOGGER.error("Ignore illegal pattern: " + pattern + "  cause: " + e.getMessage());
            return null;
        }
    }

    Predicate<String> getFilter(String config) {
        if (config == null || config.isEmpty()) {
            LOGGER.debug("No filter");
            return NO_FILTER;
        }
        return configCacheForFilter.get(config);
    }

    Function<String, String> getMapper(@Nullable Pair<String, String> configs) {
        if (configs == null) {
            LOGGER.debug("No mapper");
            return NO_MAPPER;
        }
        return configCacheForMapping.get(configs);
    }
}
