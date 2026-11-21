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
 * Search over videos, channels and Shorts. The search URL (with the selected
 * content filter baked in) is built by
 * {@link org.pipepipe.rutube.linkHandler.RuTubeSearchQueryHandlerFactory}; this
 * extractor only fetches and parses the paginated results.
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
        final JsonObject json = RuTubeParsingHelper.fetchJson(getDownloader(),
                RuTubeParsingHelper.httpsify(page.getUrl()));
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
                if ("ad".equals(itemType)) {
                    continue;
                }
                if (RuTubeFilters.CONTENT_CHANNELS.equals(contentType)) {
                    if (!RuTubeParsingHelper.getString(result, "name").isEmpty()) {
                        collector.commit(new RuTubeChannelInfoItemExtractor(result));
                    }
                } else if ("playlist".equals(itemType)) {
                    // Videos and playlists share one RuTube endpoint; playlists are
                    // kept when they show up.
                    collector.commit(new RuTubePlaylistInfoItemExtractor(result));
                } else if (RuTubeParsingHelper.getString(result, "title").isEmpty()) {
                    // Cards without a title are not playable items.
                    continue;
                } else {
                    collector.commit(new RuTubeStreamInfoItemExtractor(result));
                }
            }
        }

        final String next = RuTubeParsingHelper.httpsify(
                RuTubeParsingHelper.getString(json, "next"));
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
