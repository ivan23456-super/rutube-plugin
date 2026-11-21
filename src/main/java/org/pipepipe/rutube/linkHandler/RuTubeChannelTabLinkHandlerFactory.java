package org.pipepipe.rutube.linkHandler;

import org.pipepipe.rutube.RuTubeParsingHelper;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.ChannelTabs;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory;
import org.schabi.newpipe.extractor.search.filter.Filter;
import org.schabi.newpipe.extractor.search.filter.FilterItem;
import org.schabi.newpipe.extractor.utils.Utils;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Accepts RuTube channel tab URLs such as {@code /channel/{id}/videos/},
 * {@code /channel/{id}/shorts/}, {@code /channel/{id}/playlists/} and
 * {@code /u/{slug}/videos/}. The tab itself is carried in the content filter,
 * as usual for channel tabs.
 *
 * <p>{@code livestreams} is not a page on rutube.ru (the site shows live streams
 * inside the channel), but it is a real section of the channel API, so the tab
 * URL uses the same shape.</p>
 */
public final class RuTubeChannelTabLinkHandlerFactory extends ListLinkHandlerFactory {

    private static final RuTubeChannelTabLinkHandlerFactory INSTANCE =
            new RuTubeChannelTabLinkHandlerFactory();

    private static final Pattern TAB_PATTERN = Pattern.compile(
            "^/(?:channel/(\\d+)|u/([^/]+)|video/person/(\\d+))/"
                    + "(videos|shorts|playlists|livestreams)/?$");

    private RuTubeChannelTabLinkHandlerFactory() {
    }

    public static RuTubeChannelTabLinkHandlerFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getUrl(final String id,
                         final List<FilterItem> contentFilter,
                         final List<FilterItem> sortFilter) throws ParsingException {
        return getUrl(id, contentFilter, sortFilter, RuTubeParsingHelper.BASE_URL);
    }

    @Override
    public String getUrl(final String id,
                         final List<FilterItem> contentFilter,
                         final List<FilterItem> sortFilter,
                         final String baseUrl) throws ParsingException {
        if (contentFilter == null || contentFilter.isEmpty()) {
            throw new ParsingException("Channel tab URL needs a tab content filter");
        }
        final String tab = contentFilter.get(0).getName();
        if (!tab.equals(ChannelTabs.VIDEOS) && !tab.equals(ChannelTabs.SHORTS)
                && !tab.equals(ChannelTabs.PLAYLISTS)
                && !tab.equals(ChannelTabs.LIVESTREAMS)) {
            throw new ParsingException("Unsupported channel tab: " + tab);
        }
        return baseUrl + "/channel/" + id + "/" + tab + "/";
    }

    @Override
    public String getId(final String url) throws ParsingException {
        final Matcher matcher = TAB_PATTERN.matcher(getPath(url));
        if (matcher.matches()) {
            if (matcher.group(1) != null) {
                return matcher.group(1);
            }
            if (matcher.group(2) != null) {
                return matcher.group(2);
            }
            return matcher.group(3);
        }
        throw new ParsingException("Could not extract channel id from: " + url);
    }

    @Override
    public ListLinkHandler fromUrl(final String url, final String baseUrl)
            throws ParsingException {
        if (url == null) {
            throw new IllegalArgumentException("url may not be null");
        }
        final Matcher matcher = TAB_PATTERN.matcher(getPath(url));
        if (!matcher.matches()) {
            throw new ParsingException("URL not accepted: " + url);
        }
        final String tab = matcher.group(4);
        final String id = getId(url);
        final List<FilterItem> contentFilter = Collections.singletonList(
                new FilterItem(Filter.ITEM_IDENTIFIER_UNKNOWN, tab));
        final String tabUrl = getUrl(id, contentFilter, null, baseUrl);
        return new ListLinkHandler(url, tabUrl, id, contentFilter, null);
    }

    @Override
    public boolean onAcceptUrl(final String url) {
        try {
            final URL urlObj = Utils.stringToURL(url);
            if (!Utils.isHTTP(urlObj)) {
                return false;
            }
            final String host = urlObj.getHost();
            if (!host.equals("rutube.ru") && !host.equals("www.rutube.ru")) {
                return false;
            }
            return TAB_PATTERN.matcher(urlObj.getPath()).matches();
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
