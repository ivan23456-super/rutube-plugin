package org.pipepipe.rutube.extractors;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;

import org.pipepipe.rutube.RuTubeParsingHelper;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.suggestion.SuggestionExtractor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Search suggestions from {@code /api/search/suggest}: only the text items
 * are returned as suggestions.
 */
public final class RuTubeSuggestionExtractor extends SuggestionExtractor {

    public RuTubeSuggestionExtractor(final StreamingService service) {
        super(service);
    }

    @Override
    public List<String> suggestionList(final String query)
            throws IOException, ExtractionException {
        final String url = RuTubeParsingHelper.SEARCH_SUGGEST_API
                + "?query=" + RuTubeParsingHelper.encode(query);
        final JsonObject json = RuTubeParsingHelper.fetchJson(NewPipe.getDownloader(), url);
        final List<String> suggestions = new ArrayList<>();
        final JsonArray items = RuTubeParsingHelper.getArray(json, "items");
        if (items != null) {
            for (final Object item : items) {
                if (item instanceof JsonObject) {
                    final JsonObject suggestion = (JsonObject) item;
                    if ("text".equals(RuTubeParsingHelper.getString(suggestion, "type"))) {
                        final String text =
                                RuTubeParsingHelper.getString(suggestion, "text");
                        if (!text.isEmpty()) {
                            suggestions.add(text);
                        }
                    }
                }
            }
        }
        return suggestions;
    }
}
