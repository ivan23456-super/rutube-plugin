package org.pipepipe.rutube;

import static java.util.Arrays.asList;
import static org.schabi.newpipe.extractor.StreamingService.ServiceInfo.MediaCapability.COMMENTS;
import static org.schabi.newpipe.extractor.StreamingService.ServiceInfo.MediaCapability.LIVE;
import static org.schabi.newpipe.extractor.StreamingService.ServiceInfo.MediaCapability.VIDEO;

import org.pipepipe.rutube.extractors.RuTubeChannelExtractor;
import org.pipepipe.rutube.extractors.RuTubeChannelTabExtractor;
import org.pipepipe.rutube.extractors.RuTubeCommentsExtractor;
import org.pipepipe.rutube.extractors.RuTubeKioskExtractor;
import org.pipepipe.rutube.extractors.RuTubePlaylistExtractor;
import org.pipepipe.rutube.extractors.RuTubeSearchExtractor;
import org.pipepipe.rutube.extractors.RuTubeStreamExtractor;
import org.pipepipe.rutube.extractors.RuTubeSuggestionExtractor;
import org.pipepipe.rutube.extractors.RuTubeTvKioskExtractor;
import org.pipepipe.rutube.linkHandler.RuTubeChannelLinkHandlerFactory;
import org.pipepipe.rutube.linkHandler.RuTubeChannelTabLinkHandlerFactory;
import org.pipepipe.rutube.linkHandler.RuTubeCommentsLinkHandlerFactory;
import org.pipepipe.rutube.linkHandler.RuTubeKioskLinkHandlerFactory;
import org.pipepipe.rutube.linkHandler.RuTubePlaylistLinkHandlerFactory;
import org.pipepipe.rutube.linkHandler.RuTubeSearchQueryHandlerFactory;
import org.pipepipe.rutube.linkHandler.RuTubeStreamLinkHandlerFactory;
import org.pipepipe.rutube.linkHandler.RuTubeTvKioskLinkHandlerFactory;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.channel.ChannelExtractor;
import org.schabi.newpipe.extractor.channel.ChannelTabExtractor;
import org.schabi.newpipe.extractor.comments.CommentsExtractor;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.kiosk.KioskList;
import org.schabi.newpipe.extractor.linkhandler.LinkHandler;
import org.schabi.newpipe.extractor.linkhandler.LinkHandlerFactory;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory;
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandler;
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandlerFactory;
import org.schabi.newpipe.extractor.playlist.PlaylistExtractor;
import org.schabi.newpipe.extractor.plugin.PluginLogin;
import org.schabi.newpipe.extractor.plugin.PluginPreferences;
import org.schabi.newpipe.extractor.search.SearchExtractor;
import org.schabi.newpipe.extractor.stream.StreamExtractor;
import org.schabi.newpipe.extractor.subscription.SubscriptionExtractor;
import org.schabi.newpipe.extractor.suggestion.SuggestionExtractor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * RuTube service. The serviceId is assigned by the host and must be passed on unchanged.
 */
public final class RuTubeService extends StreamingService {

    /** Cookie header of a web login made by the user, empty when signed out. */
    private static volatile String loginCookieHeader = "";

    public RuTubeService(final int id, final PluginPreferences preferences) {
        super(id, "RuTube", asList(VIDEO, LIVE, COMMENTS));
        updateLoginCookies(preferences.getString(PluginLogin.DEFAULT_KEY));
        preferences.addChangeListener(key -> {
            if (PluginLogin.DEFAULT_KEY.equals(key)) {
                updateLoginCookies(preferences.getString(key));
            }
        });
    }

    private static void updateLoginCookies(@Nullable final String cookies) {
        loginCookieHeader = cookies == null ? "" : cookies.trim();
    }

    /**
     * @return the {@code Cookie} header to send with RuTube requests: the web login
     * cookies when the user signed in through the plugin settings, otherwise empty
     */
    @Nonnull
    public static String getLoginCookieHeader() {
        return loginCookieHeader;
    }

    @Override
    public String getBaseUrl() {
        return RuTubeParsingHelper.BASE_URL;
    }

    @Override
    public LinkHandlerFactory getStreamLHFactory() {
        return RuTubeStreamLinkHandlerFactory.getInstance();
    }

    @Override
    public ListLinkHandlerFactory getChannelLHFactory() {
        return RuTubeChannelLinkHandlerFactory.getInstance();
    }

    @Override
    public ListLinkHandlerFactory getChannelTabLHFactory() {
        return RuTubeChannelTabLinkHandlerFactory.getInstance();
    }

    @Override
    public ListLinkHandlerFactory getPlaylistLHFactory() {
        return RuTubePlaylistLinkHandlerFactory.getInstance();
    }

    @Override
    public SearchQueryHandlerFactory getSearchQHFactory() {
        return RuTubeSearchQueryHandlerFactory.getInstance();
    }

    @Override
    public ListLinkHandlerFactory getCommentsLHFactory() {
        return RuTubeCommentsLinkHandlerFactory.getInstance();
    }

    @Override
    public SearchExtractor getSearchExtractor(final SearchQueryHandler queryHandler) {
        return new RuTubeSearchExtractor(this, queryHandler);
    }

    @Override
    public SuggestionExtractor getSuggestionExtractor() {
        return new RuTubeSuggestionExtractor(this);
    }

    @Override
    public SubscriptionExtractor getSubscriptionExtractor() {
        return null;
    }

    @Override
    public KioskList getKioskList() throws ExtractionException {
        final KioskList.KioskExtractorFactory kioskFactory = (streamingService, url, id) ->
                new RuTubeKioskExtractor(streamingService,
                        RuTubeKioskLinkHandlerFactory.getInstance().fromId(id), id);
        final KioskList.KioskExtractorFactory tvKioskFactory = (streamingService, url, id) ->
                new RuTubeTvKioskExtractor(streamingService,
                        RuTubeTvKioskLinkHandlerFactory.getInstance().fromId(id), id);

        final KioskList list = new KioskList(this);
        try {
            list.addKioskEntry(kioskFactory,
                    RuTubeKioskLinkHandlerFactory.getInstance(),
                    RuTubeKioskLinkHandlerFactory.KIOSK_TRENDING);
            list.addKioskEntry(tvKioskFactory,
                    RuTubeTvKioskLinkHandlerFactory.getInstance(),
                    RuTubeTvKioskLinkHandlerFactory.KIOSK_TV);
            list.setDefaultKiosk(RuTubeKioskLinkHandlerFactory.KIOSK_TRENDING);
        } catch (final Exception e) {
            throw new ExtractionException(e);
        }
        return list;
    }

    @Override
    public ChannelExtractor getChannelExtractor(final ListLinkHandler linkHandler)
            throws ExtractionException {
        return new RuTubeChannelExtractor(this, linkHandler);
    }

    @Override
    public ChannelTabExtractor getChannelTabExtractor(final ListLinkHandler linkHandler)
            throws ExtractionException {
        return new RuTubeChannelTabExtractor(this, linkHandler);
    }

    @Override
    public PlaylistExtractor getPlaylistExtractor(final ListLinkHandler linkHandler)
            throws ExtractionException {
        return new RuTubePlaylistExtractor(this, linkHandler);
    }

    @Override
    public StreamExtractor getStreamExtractor(final LinkHandler linkHandler)
            throws ExtractionException {
        return new RuTubeStreamExtractor(this, linkHandler);
    }

    @Override
    public CommentsExtractor getCommentsExtractor(final ListLinkHandler linkHandler)
            throws ExtractionException {
        return new RuTubeCommentsExtractor(this, linkHandler);
    }
}
