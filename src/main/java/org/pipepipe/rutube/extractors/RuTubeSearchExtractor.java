package org.pipepipe.rutube.extractors;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;

import org.pipepipe.rutube.RuTubeParsingHelper;
import org.pipepipe.rutube.search.filter.RuTubeFilters;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.MultiInfoItemsCollector;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandler;
import org.schabi.newpipe.extractor.search.SearchExtractor;
import org.schabi.newpipe.extractor.utils.Utils;

import java.io.IOException;

import javax.annotation.Nonnull;

/**
 * Search over videos, channels, playlists and Shorts. The search URL (with the
 * selected content filter baked in) is built by
 * {@link org.pipepipe.rutube.linkHandler.RuTubeSearchQueryHandlerFactory};
 * this extractor only fetches and parses the paginated results.
 */
public final class RuTubeSearchExtractor extends SearchExtractor {

    public RuTubeSearchExtractor(final StreamingService service,
                                 final SearchQueryHandler linkHandler) {
        super(service, linkHandler);
    }

    @Nonnull
    @Override
    public InfoItemsPage<InfoItem> getInitialPageInternal()
            throws IOException, ExtractionException {
        return getPageInternal(new Page(getUrl()));
    }

    @Nonnull
    @Override
    public InfoItemsPage<InfoItem> getPageInternal(final Page page)
            throws IOException, ExtractionException {
        if (page == null || Utils.isNullOrEmpty(page.getUrl())) {
            throw new IllegalArgumentException("Page doesn't contain an URL");
        }
        final JsonObject json = RuTubeParsingHelper.fetchJson(getDownloader(), page.getUrl());
        final MultiInfoItemsCollector collector =
                new MultiInfoItemsCollector(getServiceId());

        final String contentType = getContentType();
        final JsonArray results = RuTubeParsingHelper.getArray(json, "results");
        if (results != null) {
            for (final Object item : results) {
                if (!(item instanceof JsonObject)) {
                    continue;
                }
                final JsonObject result = (JsonObject) item;
                final String itemType = RuTubeParsingHelper.getString(result, "content_type");
                if (RuTubeFilters.CONTENT_CHANNELS.equals(contentType)) {
                    collector.commit(new RuTubeChannelInfoItemExtractor(result));
                } else if (RuTubeFilters.CONTENT_PLAYLISTS.equals(contentType)) {
                    if ("playlist".equals(itemType)) {
                        collector.commit(new RuTubePlaylistInfoItemExtractor(result));
                    }
                } else {
                    // Videos and Shorts: skip ads and playlists.
                    if (!"ad".equals(itemType) && !"playlist".equals(itemType)) {
                        collector.commit(new RuTubeStreamInfoItemExtractor(result));
                    }
                }
            }
        }

        final String next = RuTubeParsingHelper.getString(json, "next");
        final Page nextPage = next.isEmpty() ? null : new Page(next);
        return new InfoItemsPage<>(collector, nextPage);
    }

    private String getContentType() {
        if (getLinkHandler().getContentFilters() == null
                || getLinkHandler().getContentFilters().isEmpty()) {
            return RuTubeFilters.CONTENT_VIDEOS;
        }
        return getLinkHandler().getContentFilters().get(0).getName();
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader) {
        // Nothing to fetch eagerly; pages are fetched on demand.
    }
}
