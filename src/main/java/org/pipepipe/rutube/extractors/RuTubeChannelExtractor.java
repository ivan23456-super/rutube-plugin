package org.pipepipe.rutube.extractors;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;

import org.pipepipe.rutube.RuTubeParsingHelper;
import org.pipepipe.rutube.linkHandler.RuTubeChannelTabLinkHandlerFactory;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.channel.ChannelExtractor;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.ChannelTabs;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.search.filter.Filter;
import org.schabi.newpipe.extractor.search.filter.FilterItem;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.StreamInfoItemsCollector;
import org.schabi.newpipe.extractor.utils.Utils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

/**
 * Channel page: info from {@code /api/profile/user/{id}/} and the video list
 * from {@code /api/video/person/{id}/}.
 */
public final class RuTubeChannelExtractor extends ChannelExtractor {

    private JsonObject channelInfo;
    private String channelId;

    public RuTubeChannelExtractor(final StreamingService service,
                                  final ListLinkHandler linkHandler) {
        super(service, linkHandler);
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        channelId = RuTubeParsingHelper.resolveChannelId(downloader, getId());
        channelInfo = RuTubeParsingHelper.fetchJson(downloader,
                String.format(RuTubeParsingHelper.CHANNEL_INFO_API, channelId));
    }

    @Nonnull
    @Override
    public String getName() {
        return RuTubeParsingHelper.getString(channelInfo, "name");
    }

    @Nonnull
    @Override
    public String getAvatarUrl() {
        return RuTubeParsingHelper.getString(channelInfo, "avatar_url");
    }

    @Nonnull
    @Override
    public String getBannerUrl() {
        final JsonObject appearance =
                RuTubeParsingHelper.getObject(channelInfo, "appearance");
        return RuTubeParsingHelper.getString(appearance, "cover_image");
    }

    @Override
    public long getSubscriberCount() {
        return RuTubeParsingHelper.getLong(channelInfo, "subscribers_count", -1);
    }

    @Nonnull
    @Override
    public String getDescription() {
        return RuTubeParsingHelper.getString(channelInfo, "description");
    }

    @Override
    public boolean isVerified() {
        return RuTubeParsingHelper.getBoolean(channelInfo, "is_official");
    }

    /**
     * The sections the RuTube channel page really has: videos, Shorts, live streams
     * and playlists. Live streams are a video list with {@code origin__type=rst};
     * playlists come from {@code /api/playlist/user/{id}/}.
     */
    @Nonnull
    @Override
    public List<ListLinkHandler> getTabs() throws ParsingException {
        return Arrays.asList(
                tab(ChannelTabs.VIDEOS),
                tab(ChannelTabs.SHORTS),
                tab(ChannelTabs.LIVESTREAMS),
                tab(ChannelTabs.PLAYLISTS));
    }

    private ListLinkHandler tab(final String name) throws ParsingException {
        return RuTubeChannelTabLinkHandlerFactory.getInstance().fromQuery(
                getId(),
                Collections.singletonList(new FilterItem(
                        Filter.ITEM_IDENTIFIER_UNKNOWN, name)),
                null, getBaseUrl());
    }

    @Nonnull
    @Override
    public InfoItemsPage<StreamInfoItem> getInitialPage()
            throws IOException, ExtractionException {
        return getPage(new Page(String.format(RuTubeParsingHelper.CHANNEL_VIDEOS_API,
                channelId, 1, RuTubeParsingHelper.ORIGIN_VIDEOS)));
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
