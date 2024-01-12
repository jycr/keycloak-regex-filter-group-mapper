package io.github.jycr.keycloak.protocol.mapper;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigHelperTest {

    @Test
    void value_config_should_be_parsed() {
        // Given
        String strJsonConfig = "[{\"key\":\"^G_APP[A-Z]_UI_USR_007$\",\"value\":\"user\"},{\"key\":\"^G_APP[A-Z]_UI_ADM_007$\",\"value\":\"admin\"}]";

        // When
        List<Pair<Pattern, String>> configValue = ConfigHelper.CONFIG.parseConfigValue(strJsonConfig);

        // Then
        Pair<Pattern, String> item;

        assertThat(configValue).size().isEqualTo(2);

        item = configValue.get(0);
        assertThat(item.getKey().pattern()).isEqualTo("^G_APP[A-Z]_UI_USR_007$");
        assertThat(item.getValue()).isEqualTo("user");

        item = configValue.get(1);
        assertThat(item.getKey().pattern()).isEqualTo("^G_APP[A-Z]_UI_ADM_007$");
        assertThat(item.getValue()).isEqualTo("admin");
    }

    @Test
    void invalid_pattern_should_be_ignore() {
        // Given
        String strJsonConfig = "[{\"key\":\"^G_APP[A-Z]_UI_USR_007$\",\"value\":\"user\"},{\"key\":\"^G_APP[A-Z_UI_ADM_007$\",\"value\":\"admin\"}]";

        // When
        List<Pair<Pattern, String>> configValue = ConfigHelper.CONFIG.parseConfigValue(strJsonConfig);

        // Then
        Pair<Pattern, String> item;

        assertThat(configValue).size().isEqualTo(1);

        item = configValue.get(0);
        assertThat(item.getKey().pattern()).isEqualTo("^G_APP[A-Z]_UI_USR_007$");
        assertThat(item.getValue()).isEqualTo("user");
    }

    @Test
    void duplicate_pattern_should_be_accepted() {
        // Given
        String strJsonConfig = "[{\"key\":\"^G_APP[A-Z]_UI_USR_007$\",\"value\":\"user\"},{\"key\":\"^G_APP[A-Z]_UI_USR_007$\",\"value\":\"user\"}]";

        // When
        List<Pair<Pattern, String>> configValue = ConfigHelper.CONFIG.parseConfigValue(strJsonConfig);

        // Then
        Pair<Pattern, String> item;

        assertThat(configValue).size().isEqualTo(2);


        item = configValue.get(0);
        assertThat(item.getKey().pattern()).isEqualTo("^G_APP[A-Z]_UI_USR_007$");
        assertThat(item.getValue()).isEqualTo("user");

        item = configValue.get(1);
        assertThat(item.getKey().pattern()).isEqualTo("^G_APP[A-Z]_UI_USR_007$");
        assertThat(item.getValue()).isEqualTo("user");
    }

    @Test
    void null_or_blank_config_should_return_empty_config() {
        Assertions.assertThat(ConfigHelper.CONFIG.parseConfigValue(null)).isEmpty();
        Assertions.assertThat(ConfigHelper.CONFIG.parseConfigValue("")).isEmpty();
        Assertions.assertThat(ConfigHelper.CONFIG.parseConfigValue("   ")).isEmpty();
    }

    @Test
    void should_return_no_filter_if_empty_or_null() {
        Assertions.assertThat(ConfigHelper.CONFIG.getFilter(null)).isEqualTo(ConfigHelper.NO_FILTER);
        Assertions.assertThat(ConfigHelper.CONFIG.getFilter("")).isEqualTo(ConfigHelper.NO_FILTER);
    }

    @Test
    void should_return_no_mapper_if_empty_or_null() {
        Assertions.assertThat(ConfigHelper.CONFIG.getMapper(null)).isEqualTo(ConfigHelper.NO_MAPPER);
        Assertions.assertThat(ConfigHelper.CONFIG.getMapper(Pair.pair("", null))).isEqualTo(ConfigHelper.NO_MAPPER);
    }
}