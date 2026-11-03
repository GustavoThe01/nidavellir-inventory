package dev.gustavo.nidavellir.cli;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NidavellirAppTest {

    @Test
    void bannerShouldContainApplicationName() {
        assertThat(NidavellirApp.banner()).contains(NidavellirApp.NAME);
    }
}
