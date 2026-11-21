package org.pipepipe.rutube.extractors;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;

import org.pipepipe.rutube.RuTubeParsingHelper;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.comments.CommentsExtractor;
import org.schabi.newpipe.extractor.comments.CommentsInfoItem;
import org.schabi.newpipe.extractor.comments.CommentsInfoItemsCollector;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.utils.Utils;

import java.io.IOException;

import javax.annotation.Nonnull;

/**
 * Video comments from {@code /api/v3/comments/video/{id}/} with cursor
 * pagination through the {@code next} URL.
 */
public final class RuTubeCommentsExtractor extends CommentsExtractor {

    public RuTubeCommentsExtractor(final StreamingService service,
                                   final ListLinkHandler linkHandler) {
        super(service, linkHandler);
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader) {
        // Nothing to fetch eagerly; pages are fetched on demand.
    }

    @Nonnull
    @Override
    public InfoItemsPage<CommentsInfoItem> getInitialPage()
            throws IOException, ExtractionException {
        return getPage(new Page(
                String.format(RuTubeParsingHelper.COMMENTS_API, getId())));
    }

    @Nonnull
    @Override
    public InfoItemsPage<CommentsInfoItem> getPage(final Page page)
            throws IOException, ExtractionException {
        if (page == null || Utils.isNullOrEmpty(page.getUrl())) {
            throw new IllegalArgumentException("Page doesn't contain an URL");
        }
        final JsonObject json = RuTubeParsingHelper.fetchJson(getDownloader(), page.getUrl());
        final CommentsInfoItemsCollector collector =
                new CommentsInfoItemsCollector(getServiceId());

        final JsonObject pinned = RuTubeParsingHelper.getObject(json, "pinned_comment");
        final String pinnedId = RuTubeParsingHelper.getString(pinned, "id");

        final JsonArray results = RuTubeParsingHelper.getArray(json, "results");
        if (results != null) {
            for (final Object item : results) {
                if (item instanceof JsonObject) {
                    final JsonObject comment = (JsonObject) item;
                    final boolean isPinned = !pinnedId.isEmpty()
                            && pinnedId.equals(RuTubeParsingHelper.getString(comment, "id"));
                    collector.commit(
                            new RuTubeCommentsInfoItemExtractor(comment, isPinned));
                }
            }
        }

        final String next = RuTubeParsingHelper.getString(json, "next");
        final Page nextPage = next.isEmpty() ? null : new Page(next);
        return new InfoItemsPage<>(collector, nextPage);
    }
}
