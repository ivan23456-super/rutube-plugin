package org.pipepipe.rutube.linkHandler;

import org.pipepipe.rutube.RuTubeParsingHelper;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory;
import org.schabi.newpipe.extractor.search.filter.FilterItem;
import org.schabi.newpipe.extractor.utils.Utils;

import java.net.URL;
import java.util.List;

/**
 * Kiosk handler for the «Телеканалы» kiosk — the live TV channels of
 * {@code https://rutube.ru/feeds/live/} ("ТВ онлайн").
 *
 * <p>It deliberately accepts only that path: the site root belongs to the
 * trending kiosk, and videos/channels/playlists are handled elsewhere.</p>
 */
public final class RuTubeTvKioskLinkHandlerFactory extends ListLinkHandlerFactory {

    public static final String KIOSK_TV = "tv";

    private static final String TV_PATH = "/feeds/live/";

    private static final RuTubeTvKioskLinkHandlerFactory INSTANCE =
            new RuTubeTvKioskLinkHandlerFactory();

    private RuTubeTvKioskLinkHandlerFactory() {
    }

    public static RuTubeTvKioskLinkHandlerFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getUrl(final String id,
                         final List<FilterItem> contentFilter,
                         final List<FilterItem> sortFilter) throws ParsingException {
        return RuTubeParsingHelper.BASE_URL + TV_PATH;
    }

    @Override
    public String getId(final String url) throws ParsingException {
        if (!onAcceptUrl(url)) {
            throw new ParsingException("Not a RuTube TV kiosk URL: " + url);
        }
        return KIOSK_TV;
    }

    @Override
    public boolean onAcceptUrl(final String url) {
        if (Utils.isBlank(url)) {
            return false;
        }
        try {
            final URL urlObj = Utils.stringToURL(url);
            if (!Utils.isHTTP(urlObj)) {
                return false;
            }
            final String host = urlObj.getHost();
            if (!"rutube.ru".equals(host) && !"www.rutube.ru".equals(host)) {
                return false;
            }
            final String path = urlObj.getPath();
            return path != null && (path.equals(TV_PATH) || path.equals("/feeds/live"));
        } catch (final Exception e) {
            return false;
        }
    }
}
