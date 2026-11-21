package org.pipepipe.rutube.extractors;

import com.grack.nanojson.JsonObject;

import org.pipepipe.rutube.RuTubeParsingHelper;
import org.schabi.newpipe.extractor.comments.CommentsInfoItem;
import org.schabi.newpipe.extractor.comments.CommentsInfoItemExtractor;
import org.schabi.newpipe.extractor.localization.DateWrapper;
import org.schabi.newpipe.extractor.stream.Description;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.schabi.newpipe.extractor.utils.Utils.EMPTY_STRING;

/**
 * Extracts a {@code CommentsInfoItem} from a RuTube comment JSON object,
 * as returned by {@code /api/v3/comments/video/{id}/}.
 */
public final class RuTubeCommentsInfoItemExtractor implements CommentsInfoItemExtractor {

    private final JsonObject comment;
    private final boolean pinned;

    public RuTubeCommentsInfoItemExtractor(@Nonnull final JsonObject comment) {
        this(comment, false);
    }

    public RuTubeCommentsInfoItemExtractor(@Nonnull final JsonObject comment,
                                           final boolean pinned) {
        this.comment = comment;
        this.pinned = pinned;
    }

    @Nonnull
    @Override
    public String getName() {
        return getUploaderName();
    }

    @Nonnull
    @Override
    public String getUrl() {
        return getUploaderUrl();
    }

    @Nonnull
    @Override
    public String getThumbnailUrl() {
        return getUploaderAvatarUrl();
    }

    @Override
    public int getLikeCount() {
        final long likes = RuTubeParsingHelper.getLong(comment, "likes_number", -1);
        return likes < 0 ? CommentsInfoItem.NO_LIKE_COUNT : (int) likes;
    }

    @Nonnull
    @Override
    public Description getCommentText() {
        return new Description(RuTubeParsingHelper.getString(comment, "text"),
                Description.PLAIN_TEXT);
    }

    @Nonnull
    @Override
    public String getTextualUploadDate() {
        return RuTubeParsingHelper.getString(comment, "created_ts");
    }

    @Nullable
    @Override
    public DateWrapper getUploadDate() {
        return RuTubeParsingHelper.parseDate(getTextualUploadDate());
    }

    @Override
    public String getCommentId() {
        return RuTubeParsingHelper.getString(comment, "id");
    }

    @Override
    public String getUploaderUrl() {
        final JsonObject user = RuTubeParsingHelper.getObject(comment, "user");
        final String siteUrl = RuTubeParsingHelper.getString(user, "site_url");
        if (!siteUrl.isEmpty()) {
            return siteUrl;
        }
        final String uploaderId = getUploaderId();
        return uploaderId == null ? EMPTY_STRING
                : RuTubeParsingHelper.buildChannelUrl(uploaderId);
    }

    @Nullable
    @Override
    public String getUploaderId() {
        final JsonObject user = RuTubeParsingHelper.getObject(comment, "user");
        final long id = RuTubeParsingHelper.getLong(user, "id", -1);
        return id < 0 ? null : String.valueOf(id);
    }

    @Override
    public String getUploaderName() {
        final JsonObject user = RuTubeParsingHelper.getObject(comment, "user");
        return RuTubeParsingHelper.getString(user, "name");
    }

    @Override
    public String getUploaderAvatarUrl() {
        final JsonObject user = RuTubeParsingHelper.getObject(comment, "user");
        return RuTubeParsingHelper.getString(user, "avatar_url");
    }

    @Override
    public boolean isPinned() {
        return pinned;
    }

    @Override
    public int getReplyCount() {
        final long replies = RuTubeParsingHelper.getLong(comment, "replies_number", -1);
        return replies < 0 ? CommentsInfoItem.UNKNOWN_REPLY_COUNT : (int) replies;
    }
}
