package org.pipepipe.rutube.extractors;

import com.grack.nanojson.JsonObject;

import org.pipepipe.rutube.RuTubeParsingHelper;
import org.schabi.newpipe.extractor.channel.ChannelInfoItemExtractor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Extracts a {@code ChannelInfoItem} from a RuTube channel card JSON object,
 * as returned by the combined search endpoint.
 */
public final class RuTubeChannelInfoItemExtractor implements ChannelInfoItemExtractor {

    private final JsonObject channel;

    public RuTubeChannelInfoItemExtractor(@Nonnull final JsonObject channel) {
        this.channel = channel;
    }

    @Nonnull
    @Override
    public String getName() {
        return RuTubeParsingHelper.getString(channel, "name");
    }

    @Nonnull
    @Override
    public String getUrl() {
        final String absoluteUrl =
                RuTubeParsingHelper.getString(channel, "absolute_url");
        if (!absoluteUrl.isEmpty()) {
            return absoluteUrl;
        }
        final String uploaderId = getUploaderId();
        return uploaderId == null ? ""
                : RuTubeParsingHelper.buildChannelUrl(uploaderId);
    }

    @Nonnull
    @Override
    public String getThumbnailUrl() {
        String thumbnail = RuTubeParsingHelper.getString(channel, "icon");
        if (thumbnail.isEmpty()) {
            thumbnail = RuTubeParsingHelper.getString(channel, "picture");
        }
        return thumbnail;
    }

    @Nullable
    @Override
    public String getUploaderId() {
        final long id = RuTubeParsingHelper.getLong(channel, "id", -1);
        return id < 0 ? null : String.valueOf(id);
    }

    @Override
    public String getDescription() {
        return RuTubeParsingHelper.getString(channel, "description");
    }

    @Override
    public long getSubscriberCount() {
        return RuTubeParsingHelper.getLong(channel, "subscribers_count", -1);
    }

    @Override
    public long getStreamCount() {
        return RuTubeParsingHelper.getLong(channel, "video_count", -1);
    }

    @Override
    public boolean isVerified() {
        return RuTubeParsingHelper.getBoolean(channel, "is_official");
    }
}
