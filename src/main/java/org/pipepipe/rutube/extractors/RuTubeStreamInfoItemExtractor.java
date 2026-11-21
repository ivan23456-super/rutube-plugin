package org.pipepipe.rutube.extractors;

import com.grack.nanojson.JsonObject;

import org.pipepipe.rutube.RuTubeParsingHelper;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.localization.DateWrapper;
import org.schabi.newpipe.extractor.stream.StreamInfoItemExtractor;
import org.schabi.newpipe.extractor.stream.StreamType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.schabi.newpipe.extractor.utils.Utils.EMPTY_STRING;

/**
 * Extracts a {@code StreamInfoItem} from a RuTube video JSON object, as returned
 * by the video, channel, playlist, search and recommendation endpoints.
 */
public final class RuTubeStreamInfoItemExtractor implements StreamInfoItemExtractor {

    private final JsonObject video;

    public RuTubeStreamInfoItemExtractor(@Nonnull final JsonObject video) {
        this.video = video;
    }

    @Nonnull
    @Override
    public String getName() {
        return RuTubeParsingHelper.getString(video, "title");
    }

    @Nonnull
    @Override
    public String getUrl() {
        final String videoUrl = RuTubeParsingHelper.getString(video, "video_url");
        if (!videoUrl.isEmpty()) {
            return videoUrl;
        }
        return RuTubeParsingHelper.buildVideoUrl(
                RuTubeParsingHelper.getString(video, "id"));
    }

    @Nonnull
    @Override
    public String getThumbnailUrl() {
        return RuTubeParsingHelper.getString(video, "thumbnail_url");
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

    @Override
    public long getDuration() {
        // Live streams carry no duration; -1 keeps the app from showing "0:00".
        return RuTubeParsingHelper.getLong(video, "duration", -1);
    }

    @Override
    public long getViewCount() {
        return RuTubeParsingHelper.getLong(video, "hits", -1);
    }

    @Nonnull
    @Override
    public String getUploaderName() {
        final JsonObject author = RuTubeParsingHelper.getObject(video, "author");
        return RuTubeParsingHelper.getString(author, "name");
    }

    @Nullable
    @Override
    public String getUploaderId() {
        final JsonObject author = RuTubeParsingHelper.getObject(video, "author");
        final long id = RuTubeParsingHelper.getLong(author, "id", -1);
        return id < 0 ? null : String.valueOf(id);
    }

    @Override
    public String getUploaderUrl() {
        final JsonObject author = RuTubeParsingHelper.getObject(video, "author");
        final String siteUrl = RuTubeParsingHelper.getString(author, "site_url");
        if (!siteUrl.isEmpty()) {
            return siteUrl;
        }
        final String uploaderId = getUploaderId();
        return uploaderId == null ? EMPTY_STRING
                : RuTubeParsingHelper.buildChannelUrl(uploaderId);
    }

    @Nullable
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

    @Nullable
    @Override
    public String getShortDescription() {
        final String description = RuTubeParsingHelper.getString(video, "description");
        return description.isEmpty() ? null : description;
    }

    @Override
    public boolean isShortFormContent() {
        return "rshorts".equals(RuTubeParsingHelper.getString(video, "origin_type"));
    }
}
