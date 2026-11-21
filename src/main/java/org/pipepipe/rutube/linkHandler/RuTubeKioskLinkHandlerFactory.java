package org.pipepipe.rutube.linkHandler;

import org.pipepipe.rutube.RuTubeParsingHelper;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory;
import org.schabi.newpipe.extractor.search.filter.FilterItem;
import org.schabi.newpipe.extractor.utils.Utils;

import java.net.URL;
import java.util.List;

/**
 * Kiosk handler for the RuTube homepage feed (trending/recommendations).
 *
 * <p>PipePipe opens the default kiosk through {@code KioskList.getExtractorByUrl()}
 * with the service base URL ({@code https://rutube.ru/}), so this factory has to
 * accept that URL — otherwise the app reports "Could not find a kiosk that fits
 * to the url".</p>
 */
public final class RuTubeKioskLinkHandlerFactory extends ListLinkHandlerFactory {

    public static final String KIOSK_TRENDING = "trending";

    private static final RuTubeKioskLinkHandlerFactory INSTANCE =
            new RuTubeKioskLinkHandlerFactory();

    private RuTubeKioskLinkHandlerFactory() {
    }

    public static RuTubeKioskLinkHandlerFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getUrl(final String id,
                         final List<FilterItem> contentFilter,
                         final List<FilterItem> sortFilter) throws ParsingException {
        return RuTubeParsingHelper.BASE_URL + "/";
    }

    @Override
    public String getId(final String url) throws ParsingException {
        if (!onAcceptUrl(url)) {
            throw new ParsingException("Not a RuTube kiosk URL: " + url);
        }
        return KIOSK_TRENDING;
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
            // Only the site root is the kiosk; everything else is a video,
            // channel or playlist and must not be swallowed by the kiosk.
            return path == null || path.isEmpty() || "/".equals(path);
        } catch (final Exception e) {
            return false;
        }
    }
}
