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
 * Accepts RuTube playlist URLs:
 * <ul>
 *   <li>{@code /plst/{id}/}</li>
 *   <li>{@code /metainfo/tv/{id}/} (movies/series, treated as playlists)</li>
 * </ul>
 */
public final class RuTubePlaylistLinkHandlerFactory extends ListLinkHandlerFactory {

    private static final RuTubePlaylistLinkHandlerFactory INSTANCE =
            new RuTubePlaylistLinkHandlerFactory();

    private static final Pattern PLAYLIST_PATTERN =
            Pattern.compile("^/plst/(\\d+)/?$");
    private static final Pattern MOVIE_PATTERN =
            Pattern.compile("^/metainfo/tv/(\\d+)/?$");

    private RuTubePlaylistLinkHandlerFactory() {
    }

    public static RuTubePlaylistLinkHandlerFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getUrl(final String id) {
        return RuTubeParsingHelper.buildPlaylistUrl(id);
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
        Matcher matcher = PLAYLIST_PATTERN.matcher(path);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        matcher = MOVIE_PATTERN.matcher(path);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        throw new ParsingException("Could not extract playlist id from: " + url);
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
