package org.pipepipe.rutube.linkHandler;

import org.pipepipe.rutube.RuTubeParsingHelper;
import org.schabi.newpipe.extractor.exceptions.FoundAdException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory;
import org.schabi.newpipe.extractor.search.filter.FilterItem;
import org.schabi.newpipe.extractor.utils.Utils;

import java.util.List;

import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Accepts RuTube channel URLs:
 * <ul>
 *   <li>{@code /channel/{numericId}/}</li>
 *   <li>{@code /u/{slug}/}</li>
 *   <li>{@code /video/person/{numericId}/}</li>
 * </ul>
 * URLs with a tab suffix ({@code /videos}, {@code /shorts}, {@code /playlists})
 * are handled by {@link RuTubeChannelTabLinkHandlerFactory} instead.
 */
public final class RuTubeChannelLinkHandlerFactory extends ListLinkHandlerFactory {

    private static final RuTubeChannelLinkHandlerFactory INSTANCE =
            new RuTubeChannelLinkHandlerFactory();

    private static final Pattern NUMERIC_CHANNEL =
            Pattern.compile("^/channel/(\\d+)/?$");
    private static final Pattern SLUG_CHANNEL =
            Pattern.compile("^/u/([^/]+)/?$");
    private static final Pattern PERSON_CHANNEL =
            Pattern.compile("^/video/person/(\\d+)/?$");

    private RuTubeChannelLinkHandlerFactory() {
    }

    public static RuTubeChannelLinkHandlerFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getUrl(final String id) {
        return RuTubeParsingHelper.buildChannelUrl(id);
    }

    @Override
    public String getUrl(final String id,
                         final List<FilterItem> contentFilters,
                         final List<FilterItem> sortFilter)
            throws ParsingException {
        return getUrl(id);
    }

    @Override
    public String getId(final String url) throws ParsingException {
        final String path = getPath(url);
        Matcher matcher = NUMERIC_CHANNEL.matcher(path);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        matcher = SLUG_CHANNEL.matcher(path);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        matcher = PERSON_CHANNEL.matcher(path);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        throw new ParsingException("Could not extract channel id from: " + url);
    }

    @Override
    public boolean onAcceptUrl(final String url) throws FoundAdException {
        try {
            final URL urlObj = Utils.stringToURL(url);
            if (!Utils.isHTTP(urlObj)) {
                return false;
            }
            final String host = urlObj.getHost();
            if (!host.equals("rutube.ru") && !host.equals("www.rutube.ru")) {
                return false;
            }
            getId(url);
            return true;
        } catch (final Exception e) {
            return false;
        }
    }

    private static String getPath(final String url) throws ParsingException {
        try {
            return Utils.stringToURL(url).getPath();
        } catch (final Exception e) {
            throw new ParsingException("Could not parse URL: " + url, e);
        }
    }
}
