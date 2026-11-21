package org.pipepipe.rutube.extractors;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;

import org.pipepipe.rutube.RuTubeParsingHelper;
import org.schabi.newpipe.extractor.MediaFormat;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.LinkHandler;
import org.schabi.newpipe.extractor.localization.DateWrapper;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.DeliveryMethod;
import org.schabi.newpipe.extractor.stream.Description;
import org.schabi.newpipe.extractor.stream.StreamExtractor;
import org.schabi.newpipe.extractor.stream.StreamInfoItemsCollector;
import org.schabi.newpipe.extractor.stream.StreamType;
import org.schabi.newpipe.extractor.stream.SubtitlesStream;
import org.schabi.newpipe.extractor.stream.VideoStream;
import org.schabi.newpipe.extractor.utils.Utils;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.schabi.newpipe.extractor.utils.Utils.EMPTY_STRING;

/**
 * Extracts a RuTube video page: metadata from {@code /api/video/{id}/} and
 * streams from {@code /api/play/options/{id}/} (HLS master playlist, live
 * streams and captions).
 */
public final class RuTubeStreamExtractor extends StreamExtractor {

    private static final Pattern BANDWIDTH_PATTERN =
            Pattern.compile("BANDWIDTH=(\\d+)");
    private static final Pattern FRAME_RATE_PATTERN =
            Pattern.compile("FRAME-RATE=([0-9.]+)");
    private static final Pattern RESOLUTION_PATTERN =
            Pattern.compile("RESOLUTION=(\\d+)x(\\d+)");
    private static final Pattern TIMESTAMP_PATTERN =
            Pattern.compile("[?&]t=(\\d+)");

    private JsonObject video;
    private JsonObject options;
    private String hlsUrl = EMPTY_STRING;
    private final List<VideoStream> videoStreams = new ArrayList<>();
    private final List<SubtitlesStream> subtitles = new ArrayList<>();
    private boolean streamsExtracted;

    public RuTubeStreamExtractor(final StreamingService service,
                                 final LinkHandler linkHandler) {
        super(service, linkHandler);
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        final String privateToken =
                RuTubeParsingHelper.extractPrivateToken(getOriginalUrl());
        String videoId = getId();

        // Legacy numeric embed ids (/video/embed/{trackId}) are not video ids:
        // the play options resolve them to the effective video id first.
        if (videoId.matches("\\d+")) {
            final JsonObject embedOptions = RuTubeParsingHelper.fetchJson(downloader,
                    RuTubeParsingHelper.withPrivateToken(
                            String.format(RuTubeParsingHelper.PLAY_OPTIONS_API, videoId),
                            privateToken));
            final String effectiveVideo =
                    RuTubeParsingHelper.getString(embedOptions, "effective_video");
            if (!effectiveVideo.isEmpty()) {
                videoId = effectiveVideo;
            }
        }

        video = RuTubeParsingHelper.fetchJson(downloader,
                RuTubeParsingHelper.withPrivateToken(
                        String.format(RuTubeParsingHelper.VIDEO_API, videoId),
                        privateToken));
        options = RuTubeParsingHelper.fetchJson(downloader,
                RuTubeParsingHelper.withPrivateToken(
                        String.format(RuTubeParsingHelper.PLAY_OPTIONS_API, videoId),
                        privateToken));

        if (RuTubeParsingHelper.getBoolean(video, "is_deleted")) {
            throw new ExtractionException("This video has been deleted");
        }

        final JsonObject acl = RuTubeParsingHelper.getObject(options, "acl_access");
        if (acl != null && acl.has("allowed")
                && !RuTubeParsingHelper.getBoolean(acl, "allowed")) {
            final String errorText = RuTubeParsingHelper.getString(acl, "err_text");
            throw new ExtractionException(errorText.isEmpty()
                    ? "This video is not available in your region"
                    : errorText);
        }

        extractHlsUrl();
        extractSubtitles();
    }

    private void extractHlsUrl() {
        // video_balancer holds a single URL for videos ({@code default}/{@code m3u8}),
        // live streams keep their playlists under live_streams. Both are walked
        // generically because the shape differs between VOD, live shows and TV
        // channels (strings, objects or arrays).
        final List<String> urls = new ArrayList<>();
        RuTubeParsingHelper.collectHlsUrls(options.get("video_balancer"), urls);
        if (urls.isEmpty()) {
            RuTubeParsingHelper.collectHlsUrls(options.get("live_streams"), urls);
        }
        if (urls.isEmpty()) {
            RuTubeParsingHelper.collectHlsUrls(options.get("hls"), urls);
        }
        for (final String url : urls) {
            if (url.contains(".m3u8")) {
                hlsUrl = url;
                return;
            }
        }
        if (!urls.isEmpty()) {
            hlsUrl = urls.get(0);
        }
    }

    private void extractSubtitles() {
        final JsonArray captions = RuTubeParsingHelper.getArray(options, "captions");
        if (captions == null) {
            return;
        }
        for (final Object item : captions) {
            if (!(item instanceof JsonObject)) {
                continue;
            }
            final JsonObject caption = (JsonObject) item;
            final String file = RuTubeParsingHelper.getString(caption, "file");
            final String code = RuTubeParsingHelper.getString(caption, "code");
            if (file.isEmpty() || code.isEmpty()) {
                continue;
            }
            final String extension = file.contains(".")
                    ? file.substring(file.lastIndexOf('.') + 1) : "";
            MediaFormat format = MediaFormat.getFromSuffix(extension);
            if (format == null) {
                format = MediaFormat.SRT;
            }
            subtitles.add(new SubtitlesStream.Builder()
                    .setId(code + "-" + file.hashCode())
                    .setContent(file, true)
                    .setMediaFormat(format)
                    .setDeliveryMethod(DeliveryMethod.PROGRESSIVE_HTTP)
                    .setLanguageCode(code)
                    .setAutoGenerated(
                            RuTubeParsingHelper.getBoolean(caption, "is_autogenerated"))
                    .build());
        }
    }

    private void extractVideoStreams() throws IOException, ExtractionException {
        if (streamsExtracted) {
            return;
        }
        streamsExtracted = true;
        if (hlsUrl.isEmpty()) {
            String live = String.valueOf(options.get("live_streams"));
            if (live.length() > 300) {
                live = live.substring(0, 300) + "…";
            }
            throw new ExtractionException("RuTube не вернул ссылку на поток. Поля ответа: "
                    + RuTubeParsingHelper.keysOf(options) + " (live_streams=" + live + ")");
        }
        String masterPlaylist = null;
        try {
            masterPlaylist = RuTubeParsingHelper.fetchText(getDownloader(), hlsUrl);
        } catch (final Exception ignored) {
            // Fall through: the HLS URL itself is still usable for playback.
        }
        if (masterPlaylist != null) {
            parseVariants(masterPlaylist);
        }
        if (videoStreams.isEmpty()) {
            // Live channels often serve a plain media playlist (no
            // EXT-X-STREAM-INF variants) or the master playlist is not fetchable;
            // hand the HLS URL to the player, it can pick the variant itself.
            videoStreams.add(new VideoStream.Builder()
                    .setId("hls")
                    .setContent(hlsUrl, true)
                    .setIsVideoOnly(false)
                    .setResolution("")
                    .setMediaFormat(MediaFormat.MPEG_4)
                    .setDeliveryMethod(DeliveryMethod.HLS)
                    .setBitrate(0)
                    .setRequestHeaders(RuTubeParsingHelper.mediaHeaders())
                    .build());
        }
    }

    private void parseVariants(@Nonnull final String masterPlaylist) {
        final Set<String> seenResolutions = new HashSet<>();
        final String[] lines = masterPlaylist.split("\n");
        for (int i = 0; i < lines.length; i++) {
            final String line = lines[i].trim();
            if (!line.startsWith("#EXT-X-STREAM-INF")) {
                continue;
            }
            String variantUrl = EMPTY_STRING;
            for (int j = i + 1; j < lines.length; j++) {
                final String candidate = lines[j].trim();
                if (!candidate.isEmpty() && !candidate.startsWith("#")) {
                    variantUrl = candidate;
                    break;
                }
            }
            if (variantUrl.isEmpty()) {
                continue;
            }
            final Matcher resolutionMatcher = RESOLUTION_PATTERN.matcher(line);
            int width = 0;
            int height = 0;
            if (resolutionMatcher.find()) {
                width = Integer.parseInt(resolutionMatcher.group(1));
                height = Integer.parseInt(resolutionMatcher.group(2));
            }
            final Matcher bandwidthMatcher = BANDWIDTH_PATTERN.matcher(line);
            final int bitrate = bandwidthMatcher.find()
                    ? Integer.parseInt(bandwidthMatcher.group(1)) / 1000 : 0;
            final Matcher frameRateMatcher = FRAME_RATE_PATTERN.matcher(line);
            final int fps = frameRateMatcher.find()
                    ? (int) Math.round(Double.parseDouble(frameRateMatcher.group(1))) : 0;
            // RuTube lists every variant twice (two CDNs); keep one per quality.
            final String qualityKey = height > 0 ? width + "x" + height
                    : "bandwidth-" + bitrate;
            if (!seenResolutions.add(qualityKey)) {
                continue;
            }
            try {
                variantUrl = new URL(new URL(hlsUrl), variantUrl).toString();
            } catch (final Exception ignored) {
                // keep the raw URL
            }
            videoStreams.add(new VideoStream.Builder()
                    .setId(qualityKey + "-" + bitrate)
                    .setContent(variantUrl, true)
                    .setIsVideoOnly(false)
                    .setResolution(height > 0 ? height + "p" : "")
                    .setMediaFormat(MediaFormat.MPEG_4)
                    .setDeliveryMethod(DeliveryMethod.HLS)
                    .setBitrate(bitrate)
                    .setWidth(width)
                    .setHeight(height)
                    .setFps(fps)
                    .setRequestHeaders(RuTubeParsingHelper.mediaHeaders())
                    .build());
        }
    }

    // ------------------------------------------------------------------
    // Metadata
    // ------------------------------------------------------------------

    @Nonnull
    @Override
    public String getName() {
        return RuTubeParsingHelper.getString(video, "title");
    }

    @Nonnull
    @Override
    public String getThumbnailUrl() {
        return RuTubeParsingHelper.getString(video, "thumbnail_url");
    }

    @Nonnull
    @Override
    public Description getDescription() {
        return new Description(RuTubeParsingHelper.getString(video, "description"),
                Description.PLAIN_TEXT);
    }

    @Override
    public int getAgeLimit() {
        return RuTubeParsingHelper.getBoolean(video, "is_adult") ? 18 : NO_AGE_LIMIT;
    }

    @Override
    public long getLength() {
        return RuTubeParsingHelper.getLong(video, "duration", 0);
    }

    @Override
    public long getTimeStamp() throws ParsingException {
        final Matcher matcher = TIMESTAMP_PATTERN.matcher(getOriginalUrl());
        if (matcher.find()) {
            try {
                return Long.parseLong(matcher.group(1));
            } catch (final NumberFormatException ignored) {
                // fall through
            }
        }
        return 0;
    }

    @Override
    public long getViewCount() {
        return RuTubeParsingHelper.getLong(video, "hits", -1);
    }

    @Nonnull
    @Override
    public String getUploaderUrl() {
        final JsonObject author = RuTubeParsingHelper.getObject(video, "author");
        final String siteUrl = RuTubeParsingHelper.getString(author, "site_url");
        if (!siteUrl.isEmpty()) {
            return siteUrl;
        }
        final long authorId = RuTubeParsingHelper.getLong(author, "id", -1);
        return authorId < 0 ? EMPTY_STRING
                : RuTubeParsingHelper.buildChannelUrl(String.valueOf(authorId));
    }

    @Nonnull
    @Override
    public String getUploaderName() {
        final JsonObject author = RuTubeParsingHelper.getObject(video, "author");
        return RuTubeParsingHelper.getString(author, "name");
    }

    @Nonnull
    @Override
    public String getUploaderAvatarUrl() {
        final JsonObject author = RuTubeParsingHelper.getObject(video, "author");
        return RuTubeParsingHelper.getString(author, "avatar_url");
    }

    @Override
    public boolean isUploaderVerified() {
        return RuTubeParsingHelper.getBoolean(video, "is_official");
    }

    @Nullable
    @Override
    public String getTextualUploadDate() {
        return RuTubeParsingHelper.getString(video, "created_ts");
    }

    @Nullable
    @Override
    public DateWrapper getUploadDate() {
        return RuTubeParsingHelper.parseDate(getTextualUploadDate());
    }

    @Nonnull
    @Override
    public String getCategory() {
        final JsonObject category = RuTubeParsingHelper.getObject(video, "category");
        return RuTubeParsingHelper.getString(category, "name");
    }

    @Nonnull
    @Override
    public List<String> getTags() {
        final List<String> tags = new ArrayList<>();
        final JsonArray allTags = RuTubeParsingHelper.getArray(video, "all_tags");
        if (allTags != null) {
            for (final Object item : allTags) {
                if (item instanceof JsonObject) {
                    final String name =
                            RuTubeParsingHelper.getString((JsonObject) item, "name");
                    if (!name.isEmpty()) {
                        tags.add(name);
                    }
                }
            }
        }
        final JsonArray hashtags = RuTubeParsingHelper.getArray(video, "hashtags");
        if (hashtags != null) {
            for (final Object item : hashtags) {
                if (item instanceof String && !((String) item).isEmpty()) {
                    tags.add((String) item);
                }
            }
        }
        return tags;
    }

    @Override
    public boolean isShortFormContent() {
        return "rshorts".equals(RuTubeParsingHelper.getString(video, "origin_type"));
    }

    @Override
    public String getErrorMessage() {
        final JsonObject acl = RuTubeParsingHelper.getObject(options, "acl_access");
        if (acl != null && acl.has("allowed")
                && !RuTubeParsingHelper.getBoolean(acl, "allowed")) {
            return RuTubeParsingHelper.getString(acl, "err_text");
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Streams
    // ------------------------------------------------------------------

    @Nonnull
    @Override
    public String getHlsUrl() {
        return hlsUrl;
    }

    @Nonnull
    @Override
    public List<VideoStream> getVideoStreams() throws IOException, ExtractionException {
        assertPageFetched();
        extractVideoStreams();
        return videoStreams;
    }

    @Nonnull
    @Override
    public List<VideoStream> getVideoOnlyStreams() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public List<AudioStream> getAudioStreams() {
        // RuTube serves muxed HLS variants (audio + video in one playlist),
        // so there are no separate audio-only streams.
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public List<SubtitlesStream> getSubtitlesDefault() {
        return subtitles;
    }

    @Nonnull
    @Override
    public List<SubtitlesStream> getSubtitles(final MediaFormat format) {
        final List<SubtitlesStream> result = new ArrayList<>();
        for (final SubtitlesStream subtitle : subtitles) {
            if (subtitle.getFormat() == format) {
                result.add(subtitle);
            }
        }
        return result;
    }

    @Override
    public StreamType getStreamType() {
        if (RuTubeParsingHelper.getBoolean(video, "is_livestream")
                || RuTubeParsingHelper.getBoolean(video, "is_on_air")) {
            return StreamType.LIVE_STREAM;
        }
        if (RuTubeParsingHelper.getBoolean(video, "is_audio")) {
            return StreamType.AUDIO_STREAM;
        }
        return StreamType.VIDEO_STREAM;
    }

    @Nullable
    @Override
    public StreamInfoItemsCollector getRelatedItems()
            throws IOException, ExtractionException {
        final JsonObject author = RuTubeParsingHelper.getObject(video, "author");
        final long authorId = RuTubeParsingHelper.getLong(author, "id", -1);
        if (authorId < 0) {
            return null;
        }
        final String url = String.format(RuTubeParsingHelper.CHANNEL_VIDEOS_API,
                authorId, 1, RuTubeParsingHelper.ORIGIN_VIDEOS);
        final JsonObject json = RuTubeParsingHelper.fetchJson(getDownloader(), url);
        final JsonArray results = RuTubeParsingHelper.getArray(json, "results");
        if (results == null) {
            return null;
        }
        final StreamInfoItemsCollector collector =
                new StreamInfoItemsCollector(getServiceId());
        final String currentId = RuTubeParsingHelper.getString(video, "id");
        for (final Object item : results) {
            if (item instanceof JsonObject) {
                final JsonObject related = (JsonObject) item;
                if (!RuTubeParsingHelper.getString(related, "id").equals(currentId)) {
                    collector.commit(new RuTubeStreamInfoItemExtractor(related));
                }
            }
        }
        return collector;
    }
}
