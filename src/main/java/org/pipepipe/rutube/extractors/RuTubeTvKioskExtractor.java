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
 * «Телеканалы» kiosk: the live TV channels of {@code https://rutube.ru/feeds/live/}
 * (Россия 1, Первый канал, МАТЧ ТВ, НТВ, …), served by
 * {@code /api/v2/video/stream/tvbroadcastinfo?alias=all&category=1}.
 *
 * <p>Unlike the channel video list, this endpoint returns one item per TV channel:
 * a live stream (video) with the channel as its uploader, so the items open the
 * broadcast directly and paginate through the {@code next} URL.</p>
 */
public final class RuTubeTvKioskExtractor extends KioskExtractor<StreamInfoItem> {

    public RuTubeTvKioskExtractor(final StreamingService streamingService,
                                  final ListLinkHandler linkHandler,
                                  final String kioskId) {
        super(streamingService, linkHandler, kioskId);
    }

    @Nonnull
    @Override
    public String getName() {
        return "Телеканалы";
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader) {
        // Nothing to fetch eagerly; pages are fetched on demand.
    }

    @Nonnull
    @Override
    public InfoItemsPage<StreamInfoItem> getInitialPage()
            throws IOException, ExtractionException {
        return getPage(new Page(RuTubeParsingHelper.TV_CHANNELS_API));
    }

    @Nonnull
    @Override
    public InfoItemsPage<StreamInfoItem> getPage(final Page page)
            throws IOException, ExtractionException {
        if (page == null || Utils.isNullOrEmpty(page.getUrl())) {
            throw new IllegalArgumentException("Page doesn't contain an URL");
        }
        final JsonObject json = RuTubeParsingHelper.fetchJson(getDownloader(),
                RuTubeParsingHelper.httpsify(page.getUrl()));
        final StreamInfoItemsCollector collector =
                new StreamInfoItemsCollector(getServiceId());
        final JsonArray results = RuTubeParsingHelper.getArray(json, "results");
        int itemCount = 0;
        if (results != null) {
            for (final Object item : results) {
                if (!(item instanceof JsonObject)) {
                    continue;
                }
                final JsonObject channel = (JsonObject) item;
                if (RuTubeParsingHelper.getString(channel, "id").isEmpty()
                        || RuTubeParsingHelper.getString(channel, "title").isEmpty()) {
                    continue;
                }
                itemCount++;
                collector.commit(new RuTubeStreamInfoItemExtractor(channel));
            }
        }
        final String next = RuTubeParsingHelper.nextPageUrl(json, page.getUrl(), itemCount);
        final Page nextPage = next.isEmpty() ? null : new Page(next);
        return new InfoItemsPage<>(collector, nextPage);
    }
}
