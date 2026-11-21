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
 * Channel tabs of a RuTube channel:
 * <ul>
 *     <li>{@code videos} – uploads ({@code /api/video/person/{id}/}, origin rtb,ifrm,rspa);</li>
 *     <li>{@code shorts} – Shorts (origin {@code rshorts});</li>
 *     <li>{@code livestreams} – live streams only (origin {@code rst});</li>
 *     <li>{@code playlists} – channel playlists ({@code /api/playlist/user/{id}/}).</li>
 * </ul>
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

    private boolean isPlaylistsTab() {
        return ChannelTabs.PLAYLISTS.equals(getTab());
    }

    private String getOriginType() throws ParsingException {
        final String tab = getTab();
        if (ChannelTabs.SHORTS.equals(tab)) {
            return RuTubeParsingHelper.ORIGIN_SHORTS;
        }
        if (ChannelTabs.LIVESTREAMS.equals(tab)) {
            return RuTubeParsingHelper.ORIGIN_LIVE;
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
        if (isPlaylistsTab()) {
            return getPage(new Page(String.format(
                    RuTubeParsingHelper.CHANNEL_PLAYLISTS_API, channelId, 1)));
        }
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
        final boolean liveTab = ChannelTabs.LIVESTREAMS.equals(getTab());
        int itemCount = 0;
        if (results != null) {
            for (final Object item : results) {
                if (item instanceof JsonObject) {
                    itemCount++;
                    if (liveTab && RuTubeParsingHelper.isFinishedBroadcast(
                            (JsonObject) item)) {
                        // RuTube keeps finished broadcasts in the live list;
                        // they cannot be played, so they do not belong here.
                        continue;
                    }
                    if (isPlaylistsTab()) {
                        collector.commit(
                                new RuTubePlaylistInfoItemExtractor((JsonObject) item));
                    } else {
                        collector.commit(
                                new RuTubeStreamInfoItemExtractor((JsonObject) item));
                    }
                }
            }
        }
        final String next = RuTubeParsingHelper.nextPageUrl(json, page.getUrl(), itemCount);
        final Page nextPage = next.isEmpty() ? null : new Page(next);
        return new InfoItemsPage<>(collector, nextPage);
    }
}
