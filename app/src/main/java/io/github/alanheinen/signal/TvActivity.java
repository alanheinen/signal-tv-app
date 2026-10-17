package io.github.alanheinen.signal;

/** The Signal TV tile: the TV guide and player. */
public class TvActivity extends WebActivity {
    @Override
    protected String siteUrl() {
        return BuildConfig.TV_URL;
    }
}
