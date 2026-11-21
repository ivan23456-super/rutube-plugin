package org.pipepipe.rutube.linkHandler;

import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory;
import org.schabi.newpipe.extractor.search.filter.FilterItem;

import java.util.List;

/**
 * Comments are attached to videos, so this factory accepts the same URLs as the
 * stream factory and reuses the video id.
 */
public final class RuTubeCommentsLinkHandlerFactory extends ListLinkHandlerFactory {

    private static final RuTubeCommentsLinkHandlerFactory INSTANCE =
            new RuTubeCommentsLinkHandlerFactory();

    private RuTubeCommentsLinkHandlerFactory() {
    }

    public static RuTubeCommentsLinkHandlerFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getUrl(final String id,
                         final List<FilterItem> contentFilter,
                         final List<FilterItem> sortFilter) throws ParsingException {
        return RuTubeStreamLinkHandlerFactory.getInstance().getUrl(id);
    }

    @Override
    public String getId(final String url) throws ParsingException {
        return RuTubeStreamLinkHandlerFactory.getInstance().getId(url);
    }

    @Override
    public boolean onAcceptUrl(final String url) throws ParsingException {
        return RuTubeStreamLinkHandlerFactory.getInstance().onAcceptUrl(url);
    }
}
