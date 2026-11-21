package org.pipepipe.rutube.linkHandler;

import org.pipepipe.rutube.RuTubeParsingHelper;
import org.schabi.newpipe.extractor.exceptions.FoundAdException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.LinkHandlerFactory;
import org.schabi.newpipe.extractor.utils.Utils;

import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Accepts RuTube video URLs:
 * <ul>
 *   <li>{@code /video/{id}/}, {@code /audio/{id}/}</li>
 *   <li>{@code /live/video/{id}/}</li>
 *   <li>{@code /video/private/{id}/} (private videos, token in the query)</li>
 *   <li>{@code /shorts/{id}/}, {@code /yappy/{id}/}</li>
 *   <li>{@code /play/embed/{id}}, {@code /embed/{id}}, {@code /video/embed/{id}}</li>
 * </ul>
 * The id is a 32 character hex string, except for legacy numeric embed ids.
 */
public final class RuTubeStreamLinkHandlerFactory extends LinkHandlerFactory {

    private static final RuTubeStreamLinkHandlerFactory INSTANCE =
            new RuTubeStreamLinkHandlerFactory();

    private static final String ID = "([0-9a-zA-Z:_-]{1,32})";
    private static final Pattern[] ID_PATTERNS = {
            Pattern.compile("^/(?:video|audio)/(?:private/)?" + ID + "/?$"),
            Pattern.compile("^/live/video/(?:private/)?" + ID + "/?$"),
            Pattern.compile("^/(?:shorts|yappy)/" + ID + "/?$"),
            Pattern.compile("^/(?:play/)?embed/" + ID + "/?$"),
            Pattern.compile("^/video/embed/" + ID + "/?$")
    };

    private RuTubeStreamLinkHandlerFactory() {
    }

    public static RuTubeStreamLinkHandlerFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getUrl(final String id) {
        return RuTubeParsingHelper.buildVideoUrl(id);
    }

    @Override
    public String getId(final String url) throws ParsingException {
        final String path = getPath(url);
        for (final Pattern pattern : ID_PATTERNS) {
            final Matcher matcher = pattern.matcher(path);
            if (matcher.matches()) {
                final String id = matcher.group(1);
                // Guard against /video/person/, /video/category/ and similar
                // non-video paths that would otherwise match the id pattern.
                if (id.equals("person") || id.equals("category")
                        || id.equals("embed") || id.equals("private")) {
                    continue;
                }
                return id;
            }
        }
        throw new ParsingException("Could not extract video id from: " + url);
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
