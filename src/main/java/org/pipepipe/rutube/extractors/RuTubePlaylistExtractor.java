package org.pipepipe.rutube.extractors;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;

import org.pipepipe.rutube.RuTubeParsingHelper;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.playlist.PlaylistExtractor;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.StreamInfoItemsCollector;
import org.schabi.newpipe.extractor.utils.Utils;

import java.io.IOException;

import javax.annotation.Nonnull;

/**
 * Playlist page: info from {@code /api/playlist/custom/{id}/} (or
 * {@code /api/metainfo/tv/{id}/} for movies) and the video list.
 */
public final class RuTubePlaylistExtractor extends PlaylistExtractor {

    private JsonObject playlistInfo;
    private JsonObject uploaderInfo;
    private boolean isMovie;

    public RuTubePlaylistExtractor(final StreamingService service,
                                   final ListLinkHandler linkHandler) {
        super(service, linkHandler);
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        isMovie = getOriginalUrl().contains("/metainfo/tv/");
        final String infoUrl = isMovie
                ? String.format(RuTubeParsingHelper.MOVIE_INFO_API, getId())
                : String.format(RuTubeParsingHelper.PLAYLIST_INFO_API, getId());
        playlistInfo = RuTubeParsingHelper.fetchJson(downloader, infoUrl);

        final long userId = RuTubeParsingHelper.getLong(playlistInfo, "user_id", -1);
        if (userId >= 0) {
            try {
                uploaderInfo = RuTubeParsingHelper.fetchJson(downloader,
                        String.format(RuTubeParsingHelper.CHANNEL_INFO_API, userId));
            } catch (final Exception ignored) {
                uploaderInfo = null;
            }
        }
    }

    @Nonnull
    @Override
    public String getName() {
        String name = RuTubeParsingHelper.getString(playlistInfo, "title");
        if (name.isEmpty()) {
            name = RuTubeParsingHelper.getString(playlistInfo, "name");
        }
        return name;
    }

    @Nonnull
    @Override
    public String getThumbnailUrl() {
        return RuTubeParsingHelper.getString(playlistInfo, "thumbnail_url");
    }

    @Nonnull
    @Override
    public String getUploaderUrl() {
        if (uploaderInfo != null) {
            final long userId = RuTubeParsingHelper.getLong(uploaderInfo, "id", -1);
            if (userId >= 0) {
                return RuTubeParsingHelper.buildChannelUrl(String.valueOf(userId));
            }
        }
        return "";
    }

    @Nonnull
    @Override
    public String getUploaderName() {
        return RuTubeParsingHelper.getString(uploaderInfo, "name");
    }

    @Nonnull
    @Override
    public String getUploaderAvatarUrl() {
        return RuTubeParsingHelper.getString(uploaderInfo, "avatar_url");
    }

    @Override
    public boolean isUploaderVerified() {
        return RuTubeParsingHelper.getBoolean(uploaderInfo, "is_official");
    }

    @Override
    public long getStreamCount() {
        return RuTubeParsingHelper.getLong(playlistInfo, "videos_count", -1);
    }

    @Nonnull
    @Override
    public InfoItemsPage<StreamInfoItem> getInitialPage()
            throws IOException, ExtractionException {
        final String url = isMovie
                ? String.format(RuTubeParsingHelper.MOVIE_VIDEOS_API, getId(), 1)
                : String.format(RuTubeParsingHelper.PLAYLIST_VIDEOS_API, getId(), 1);
        return getPage(new Page(url));
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
