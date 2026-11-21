package org.pipepipe.rutube.extractors;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;

import org.pipepipe.rutube.RuTubeParsingHelper;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.MultiInfoItemsCollector;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.channel.ChannelTabExtractor;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.ChannelTabs;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.utils.Utils;

import java.io.IOException;

import javax.annotation.Nonnull;

/**
 * Channel tabs: videos and Shorts, both served by the channel video list with
 * a different {@code origin__type}. The playlists tab is not supported because
 * RuTube has no public channel-playlists endpoint.
 */
public final class RuTubeChannelTabExtractor extends ChannelTabExtractor {

    private String channelId;

    public RuTubeChannelTabExtractor(final StreamingService service,
                                     final ListLinkHandler linkHandler) {
        super(service, linkHandler);
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        channelId = RuTubeParsingHelper.resolveChannelId(downloader, getId());
    }

    private String getOriginType() throws ParsingException {
        final String tab = getTab();
        if (ChannelTabs.SHORTS.equals(tab)) {
            return RuTubeParsingHelper.ORIGIN_SHORTS;
        }
        if (ChannelTabs.VIDEOS.equals(tab)) {
            return RuTubeParsingHelper.ORIGIN_VIDEOS;
        }
        throw new ParsingException("Unsupported channel tab: " + tab);
    }

    @Nonnull
    @Override
    public InfoItemsPage<InfoItem> getInitialPage()
            throws IOException, ExtractionException {
        return getPage(new Page(String.format(RuTubeParsingHelper.CHANNEL_VIDEOS_API,
                channelId, 1, getOriginType())));
    }

    @Nonnull
    @Override
    public InfoItemsPage<InfoItem> getPage(final Page page)
            throws IOException, ExtractionException {
        if (page == null || Utils.isNullOrEmpty(page.getUrl())) {
            throw new IllegalArgumentException("Page doesn't contain an URL");
        }
        final JsonObject json = RuTubeParsingHelper.fetchJson(getDownloader(), page.getUrl());
        final MultiInfoItemsCollector collector =
                new MultiInfoItemsCollector(getServiceId());
        final JsonArray results = RuTubeParsingHelper.getArray(json, "results");
        if (results != null) {
            for (final Object item : results) {
                if (item instanceof JsonObject) {
                    collector.commit(
                            new RuTubeStreamInfoItemExtractor((JsonObject) item));
                }
            }
        }
        final String next = RuTubeParsingHelper.getString(json, "next");
        final Page nextPage = next.isEmpty() ? null : new Page(next);
        return new InfoItemsPage<>(collector, nextPage);
    }
}
