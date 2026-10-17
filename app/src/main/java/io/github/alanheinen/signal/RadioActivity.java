package io.github.alanheinen.signal;

/** The Signal Radio tile: the radio guide and player. */
public class RadioActivity extends WebActivity {
    @Override
    protected String siteUrl() {
        return BuildConfig.RADIO_URL;
    }
}
