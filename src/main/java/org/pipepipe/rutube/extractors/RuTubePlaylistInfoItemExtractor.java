package org.pipepipe.rutube.extractors;

import com.grack.nanojson.JsonObject;

import org.pipepipe.rutube.RuTubeParsingHelper;
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItemExtractor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Extracts a {@code PlaylistInfoItem} from a RuTube playlist JSON object,
 * as returned by the combined search endpoint.
 */
public final class RuTubePlaylistInfoItemExtractor implements PlaylistInfoItemExtractor {

    private final JsonObject playlist;

    public RuTubePlaylistInfoItemExtractor(@Nonnull final JsonObject playlist) {
        this.playlist = playlist;
    }

    @Nonnull
    @Override
    public String getName() {
        String name = RuTubeParsingHelper.getString(playlist, "title");
        if (name.isEmpty()) {
            name = RuTubeParsingHelper.getString(playlist, "name");
        }
        return name;
    }

    @Nonnull
    @Override
    public String getUrl() {
        return RuTubeParsingHelper.buildPlaylistUrl(
                String.valueOf(RuTubeParsingHelper.getLong(playlist, "id", -1)));
    }

    @Nonnull
    @Override
    public String getThumbnailUrl() {
        String thumbnail = RuTubeParsingHelper.getString(playlist, "thumbnail_url");
        if (thumbnail.isEmpty()) {
            thumbnail = RuTubeParsingHelper.getString(playlist, "picture");
        }
        return thumbnail;
    }

    @Override
    public String getUploaderName() {
        final JsonObject author = RuTubeParsingHelper.getObject(playlist, "author");
        return RuTubeParsingHelper.getString(author, "name");
    }

    @Override
    public String getUploaderUrl() {
        final JsonObject author = RuTubeParsingHelper.getObject(playlist, "author");
        return RuTubeParsingHelper.getString(author, "site_url");
    }

    @Override
    public long getStreamCount() {
        return RuTubeParsingHelper.getLong(playlist, "videos_count", -1);
    }
}
