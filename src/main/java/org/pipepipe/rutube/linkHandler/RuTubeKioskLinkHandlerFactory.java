package org.pipepipe.rutube.linkHandler;

import org.pipepipe.rutube.RuTubeParsingHelper;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory;
import org.schabi.newpipe.extractor.search.filter.FilterItem;

import java.util.List;

/**
 * Kiosk handler for the RuTube homepage feed (trending/recommendations).
 * Kiosks are not opened through URLs, so {@code onAcceptUrl} always returns false.
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
        return KIOSK_TRENDING;
    }

    @Override
    public boolean onAcceptUrl(final String url) {
        return false;
    }
}
