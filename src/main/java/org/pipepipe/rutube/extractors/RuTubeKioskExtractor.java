package org.pipepipe.rutube.extractors;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;

import org.pipepipe.rutube.RuTubeParsingHelper;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.kiosk.KioskExtractor;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.StreamInfoItemsCollector;
import org.schabi.newpipe.extractor.utils.Utils;

import java.io.IOException;

import javax.annotation.Nonnull;

/**
 * Homepage feed (trending) from {@code /api/v2/video/recommendation/main}
 * with offset-based pagination through the {@code next} URL.
 */
public final class RuTubeKioskExtractor extends KioskExtractor<StreamInfoItem> {

    public RuTubeKioskExtractor(final StreamingService streamingService,
                                final ListLinkHandler linkHandler,
                                final String kioskId) {
        super(streamingService, linkHandler, kioskId);
    }

    @Nonnull
    @Override
    public String getName() throws ParsingException {
        return "Trending";
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader) {
        // Nothing to fetch eagerly; pages are fetched on demand.
    }

    @Nonnull
    @Override
    public InfoItemsPage<StreamInfoItem> getInitialPage()
            throws IOException, ExtractionException {
        return getPage(new Page(
                String.format(RuTubeParsingHelper.RECOMMENDATION_API, 0)));
    }

    @Nonnull
    @Override
    public InfoItemsPage<StreamInfoItem> getPage(final Page page)
            throws IOException, ExtractionException {
        if (page == null || Utils.isNullOrEmpty(page.getUrl())) {
            throw new IllegalArgumentException("Page doesn't contain an URL");
        }
        final JsonObject json = RuTubeParsingHelper.fetchJson(getDownloader(), page.getUrl());
        final StreamInfoItemsCollector collector =
                new StreamInfoItemsCollector(getServiceId());
        final JsonArray results = RuTubeParsingHelper.getArray(json, "results");
        if (results != null) {
            for (final Object item : results) {
                if (item instanceof JsonObject) {
                    final JsonObject result = (JsonObject) item;
                    if (!"ad".equals(RuTubeParsingHelper.getString(result, "content_type"))) {
                        collector.commit(new RuTubeStreamInfoItemExtractor(result));
                    }
                }
            }
        }
        final String next = RuTubeParsingHelper.getString(json, "next");
        final Page nextPage = next.isEmpty() ? null : new Page(next);
        return new InfoItemsPage<>(collector, nextPage);
    }
}
